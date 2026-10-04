package org.kingdoms.jails.managers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.JailType;
import org.kingdoms.jails.data.ReleaseReason;
import org.kingdoms.jails.util.Durations;
import org.kingdoms.jails.util.Locations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds the placeholders shared by every jail message and sends the notifications.
 * <p>
 * Durations are formatted per receiver, in the units of their own language, which is why the
 * placeholders are always built for one given receiver.
 */
public final class JailMessages {
    private static final Map<UUID, Map<JailsLang, Long>> THROTTLE = new ConcurrentHashMap<>();

    private JailMessages() {}

    /**
     * {@code %player% %kingdom% %jailer% %reason% %type% %duration% %remaining% %served% %bail%
     * %location%}.
     */
    public static Object[] placeholders(JailSession session, CommandSender receiver) {
        boolean countOffline = JailsConfig.RELEASE_TIME_COUNT_OFFLINE.getBoolean();
        Kingdom kingdom = Kingdom.getKingdom(session.getKingdom());

        String duration = session.isPermanent()
                ? JailsLang.VALUES_PERMANENT.parse(sender(receiver))
                : Durations.format(session.getDuration(), receiver);
        String remaining = session.isPermanent()
                ? JailsLang.VALUES_PERMANENT.parse(sender(receiver))
                : Durations.format(session.getRemaining(countOffline), receiver);

        return new Object[]{
                "player", playerName(session.getPrisoner()),
                "kingdom", kingdom == null ? "?" : kingdom.getName(),
                "jailer", session.getIssuer() == null
                        ? JailsLang.VALUES_SERVER.parse(sender(receiver))
                        : playerName(session.getIssuer()),
                "reason", reason(session.getReason(), receiver),
                "type", JailMessages.typeName(session.getType()).parse(sender(receiver)),
                "duration", duration,
                "remaining", remaining,
                "served", Durations.format(session.getServed(countOffline), receiver),
                "bail", bail(session.getBail(), receiver),
                "location", location(kingdom, receiver),
        };
    }

    /** The {@code jails.types.<type>} message. */
    public static JailsLang typeName(JailType type) {
        switch (type) {
            case INVASION: return JailsLang.TYPES_INVASION;
            case ADMIN: return JailsLang.TYPES_ADMIN;
            default: return JailsLang.TYPES_MANUAL;
        }
    }

    /** The {@code jails.release.reasons.<reason>} message. */
    public static JailsLang reasonName(ReleaseReason reason) {
        switch (reason) {
            case TIME_SERVED: return JailsLang.RELEASE_REASONS_TIME_SERVED;
            case BAIL_PAID: return JailsLang.RELEASE_REASONS_BAIL_PAID;
            case RELEASED: return JailsLang.RELEASE_REASONS_RELEASED;
            case ADMIN: return JailsLang.RELEASE_REASONS_ADMIN;
            case LEFT_KINGDOM: return JailsLang.RELEASE_REASONS_LEFT_KINGDOM;
            case ESCAPED: return JailsLang.RELEASE_REASONS_ESCAPED;
            case KINGDOM_DISBANDED: return JailsLang.RELEASE_REASONS_KINGDOM_DISBANDED;
            case JAIL_REMOVED: return JailsLang.RELEASE_REASONS_JAIL_REMOVED;
            default: return JailsLang.RELEASE_REASONS_PLUGIN;
        }
    }

    public static Object[] concat(Object[] first, Object... second) {
        Object[] all = new Object[first.length + second.length];
        System.arraycopy(first, 0, all, 0, first.length);
        System.arraycopy(second, 0, all, first.length, second.length);
        return all;
    }

    public static String playerName(UUID id) {
        if (id == null) return "?";
        OfflinePlayer player = Bukkit.getOfflinePlayer(id);
        String name = player.getName();
        return name == null ? id.toString().substring(0, 8) : name;
    }

    public static String bail(double bail, CommandSender receiver) {
        if (bail <= 0) return JailsLang.VALUES_NO_BAIL.parse(sender(receiver));
        return JailsLang.VALUES_BAIL.parse(sender(receiver), "amount", amount(bail));
    }

    /** An amount of money, with the {@code jails.values.bail} format, even when it is zero. */
    public static String money(double amount, CommandSender receiver) {
        return JailsLang.VALUES_BAIL.parse(sender(receiver), "amount", amount(amount));
    }

    /** {@code 1500} rather than {@code 1500.0}, {@code 12.5} rather than {@code 12.50}. */
    public static String amount(double value) {
        BigDecimal decimal = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return decimal.scale() < 0 ? decimal.setScale(0).toPlainString() : decimal.toPlainString();
    }

    public static String reason(String reason, CommandSender receiver) {
        return reason == null || reason.isEmpty() ? JailsLang.VALUES_NO_REASON.parse(sender(receiver)) : reason;
    }

    public static String location(Kingdom kingdom, CommandSender receiver) {
        String raw = kingdom == null ? null : org.kingdoms.jails.data.KingdomJails.getJailLocationRaw(kingdom);
        if (raw == null) return JailsLang.VALUES_NO_LOCATION.parse(sender(receiver));

        org.bukkit.Location location = Locations.deserialize(raw);
        return location == null ? Locations.worldOf(raw) : Locations.describe(location);
    }

    // ------------------------------------------------------------- notifications

    /** Sends a message to every online member of the kingdom, built for each of them. */
    public static void notifyMembers(Kingdom kingdom, JailsLang message, JailSession session, UUID except,
                                     Object... extra) {
        if (kingdom == null) return;
        for (Player member : kingdom.getOnlineMembers()) {
            if (member.getUniqueId().equals(except)) continue;
            message.sendMessage(member, concat(placeholders(session, member), extra));
        }
    }

    public static void broadcast(JailsLang message, JailSession session, Object... extra) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            message.sendMessage(online, concat(placeholders(session, online), extra));
        }
        message.sendMessage(Bukkit.getConsoleSender(), concat(placeholders(session, Bukkit.getConsoleSender()), extra));
    }

    /**
     * Error shown when a restriction kicks in, at most once per
     * {@code restrictions.message-cooldown} for the same message: moving the mouse over a block
     * would otherwise flood the chat.
     */
    public static void restricted(Player player, JailsLang message) {
        long cooldown = JailsConfig.RESTRICTIONS_MESSAGE_COOLDOWN.getMillis();
        long now = System.currentTimeMillis();

        Map<JailsLang, Long> sent = THROTTLE.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>());
        Long last = sent.get(message);
        if (last != null && now - last < cooldown) return;

        sent.put(message, now);
        message.sendError(player);
    }

    public static void forget(UUID player) {
        THROTTLE.remove(player);
    }

    private static CommandSender sender(CommandSender receiver) {
        return receiver == null ? Bukkit.getConsoleSender() : receiver;
    }
}
