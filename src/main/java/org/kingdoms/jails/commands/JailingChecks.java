package org.kingdoms.jails.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.group.model.relationships.KingdomRelation;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.managers.JailMessages;
import org.kingdoms.jails.managers.JailPermissions;
import org.kingdoms.jails.managers.JailRules;
import org.kingdoms.jails.util.Durations;

import java.util.UUID;

/**
 * Every requirement of {@code /k jail member}, in one place so that the admin command can run the
 * same sentence checks while skipping the ones about who may jail whom.
 */
final class JailingChecks {
    private JailingChecks() {}

    static final class Sentence {
        final long duration;
        final double bail;
        final String reason;

        Sentence(long duration, double bail, String reason) {
            this.duration = duration;
            this.bail = bail;
            this.reason = reason;
        }
    }

    /**
     * @param jailer the member jailing, {@code null} for an admin.
     * @return the sentence, or {@code null} once an error was sent.
     */
    static Sentence check(CommandContext context, Kingdom kingdom, KingdomPlayer jailer, OfflinePlayer target,
                          SentenceArguments arguments) {
        boolean admin = jailer == null;
        UUID targetId = target.getUniqueId();
        context.var("player", target.getName() == null ? targetId.toString() : target.getName());

        if (jailer != null && targetId.equals(jailer.getId())) {
            context.sendError(JailsLang.COMMAND_JAIL_MEMBER_SELF);
            return null;
        }

        JailSession existing = KingdomJails.getSession(targetId);
        if (existing != null) {
            Kingdom holder = Kingdom.getKingdom(existing.getKingdom());
            context.var("kingdom", holder == null ? "?" : holder.getName());
            context.sendError(JailsLang.COMMAND_JAIL_MEMBER_ALREADY_JAILED);
            return null;
        }

        Player online = target.getPlayer();
        if (online != null && JailPermissions.isExempt(online)) {
            context.sendError(JailsLang.COMMAND_JAIL_MEMBER_EXEMPT);
            return null;
        }

        if (!admin && !checkTarget(context, kingdom, jailer, target, online)) return null;

        Double bail = bail(context, arguments, admin);
        if (bail == null) return null;

        Long duration = duration(context, arguments, admin);
        if (duration == null) return null;

        String reason = reason(context, arguments, admin);
        if (reason == null) return null;

        return new Sentence(duration, bail, reason.isEmpty() ? null : reason);
    }

    // ------------------------------------------------------------------- target

    private static boolean checkTarget(CommandContext context, Kingdom kingdom, KingdomPlayer jailer,
                                       OfflinePlayer target, Player online) {
        if (JailsConfig.JAILING_REQUIRE_LOCATION.getBoolean() && KingdomJails.getJailLocation(kingdom) == null) {
            context.sendError(JailsLang.COMMAND_JAIL_LOCATION_NOT_SET);
            return false;
        }

        boolean isOnline = online != null && online.isOnline();
        if (!isOnline && JailsConfig.JAILING_TARGETS_ONLINE_ONLY.getBoolean()) {
            context.sendError(JailsLang.COMMAND_JAIL_MEMBER_OFFLINE);
            return false;
        }

        KingdomPlayer targetPlayer = KingdomPlayer.getKingdomPlayer(target.getUniqueId());
        Kingdom targetKingdom = targetPlayer == null ? null : targetPlayer.getKingdom();

        if (targetKingdom != null && targetKingdom.getId().equals(kingdom.getId())) {
            if (!JailsConfig.JAILING_TARGETS_MEMBERS.getBoolean()) {
                context.sendError(JailsLang.COMMAND_JAIL_MEMBER_MEMBERS_DISABLED);
                return false;
            }
            // Members holding the jail permission can never jail one another.
            if (JailPermissions.holdsJailPermission(targetPlayer)) {
                context.sendError(JailsLang.COMMAND_JAIL_MEMBER_SAME_PERMISSION);
                return false;
            }
            if (JailsConfig.JAILING_TARGETS_RESPECT_RANKS.getBoolean() && !jailer.isHigher(targetPlayer)) {
                context.sendError(JailsLang.COMMAND_JAIL_MEMBER_HIGHER_RANK);
                return false;
            }
        } else if (targetKingdom == null) {
            if (!JailsConfig.JAILING_TARGETS_NO_KINGDOM.getBoolean()) {
                context.sendError(JailsLang.COMMAND_JAIL_MEMBER_NO_KINGDOM_DISABLED);
                return false;
            }
        } else {
            KingdomRelation relation = kingdom.getRelationWith(targetKingdom);
            String name = relation == null ? "NEUTRAL" : relation.name();
            if (!JailsConfig.JAILING_TARGETS_RELATIONS.getUpperCaseList().contains(name)) {
                context.var("target_kingdom", targetKingdom.getName()).var("relation", name);
                context.sendError(JailsLang.COMMAND_JAIL_MEMBER_RELATION_DISABLED);
                return false;
            }
        }

        if (isOnline && JailsConfig.JAILING_TARGETS_MUST_BE_IN_OWN_LAND.getBoolean()) {
            Land land = Land.getLand(online.getLocation());
            if (land == null || !kingdom.getId().equals(land.getKingdomId())) {
                context.sendError(JailsLang.COMMAND_JAIL_MEMBER_NOT_IN_LAND);
                return false;
            }
        }

        long cooldown = JailRules.cooldownLeft(kingdom, target.getUniqueId());
        if (cooldown > 0) {
            context.var("time", Durations.format(cooldown, context.getMessageReceiver()));
            context.sendError(JailsLang.COMMAND_JAIL_MEMBER_COOLDOWN);
            return false;
        }

        if (JailRules.isFull(kingdom)) {
            context.var("max", JailRules.maxPrisoners());
            context.sendError(JailsLang.COMMAND_JAIL_MEMBER_MAX_PRISONERS);
            return false;
        }
        return true;
    }

    // ----------------------------------------------------------------- sentence

    private static Double bail(CommandContext context, SentenceArguments arguments, boolean admin) {
        if (arguments.noBail) return 0D;

        boolean enabled = JailsConfig.JAILING_BAIL_ENABLED.getBoolean();
        if (arguments.bail == null) return enabled ? Math.max(0, JailsConfig.JAILING_BAIL_DEFAULT.getDouble()) : 0D;

        double bail = arguments.bail;
        if (bail == 0) return 0D;
        if (!enabled && !admin) {
            context.sendError(JailsLang.COMMAND_JAIL_MEMBER_BAIL_DISABLED);
            return null;
        }

        double min = Math.max(0, JailsConfig.JAILING_BAIL_MIN.getDouble());
        double max = JailsConfig.JAILING_BAIL_MAX.getDouble();
        boolean outOfRange = bail < 0 || (!admin && (bail < min || (max > 0 && bail > max)));
        if (outOfRange) {
            context.var("bail", JailMessages.amount(bail))
                    .var("min", JailMessages.amount(min))
                    .var("max", max > 0 ? JailMessages.amount(max) : "∞");
            context.sendError(JailsLang.COMMAND_JAIL_MEMBER_INVALID_BAIL);
            return null;
        }
        return bail;
    }

    private static Long duration(CommandContext context, SentenceArguments arguments, boolean admin) {
        if (arguments.duration < 0) {
            return Math.max(0, JailsConfig.JAILING_DURATION_DEFAULT.getMillis());
        }

        if (!admin && !JailsConfig.JAILING_DURATION_ALLOW_CUSTOM.getBoolean()) {
            context.sendError(JailsLang.COMMAND_JAIL_MEMBER_CUSTOM_DURATION_DISABLED);
            return null;
        }
        if (admin) return arguments.duration;

        long min = JailsConfig.JAILING_DURATION_MIN.getMillis();
        long max = JailsConfig.JAILING_DURATION_MAX.getMillis();
        boolean permanentRefused = arguments.duration == 0 && !JailsConfig.JAILING_DURATION_ALLOW_PERMANENT.getBoolean();
        boolean outOfRange = arguments.duration > 0 && (arguments.duration < min || (max > 0 && arguments.duration > max));
        if (permanentRefused || outOfRange) {
            context.var("min", Durations.format(min, context.getMessageReceiver()))
                    .var("max", max > 0 ? Durations.format(max, context.getMessageReceiver()) : "∞");
            context.sendError(JailsLang.COMMAND_JAIL_MEMBER_INVALID_DURATION);
            return null;
        }
        return arguments.duration;
    }

    private static String reason(CommandContext context, SentenceArguments arguments, boolean admin) {
        String reason = arguments.reason == null ? "" : sanitize(arguments.reason);
        if (reason.isEmpty()) {
            if (!admin && JailsConfig.JAILING_REASON_REQUIRED.getBoolean()) {
                context.sendError(JailsLang.COMMAND_JAIL_MEMBER_REASON_REQUIRED);
                return null;
            }
            return sanitize(JailsConfig.JAILING_REASON_DEFAULT.getString());
        }

        int max = JailsConfig.JAILING_REASON_MAX_LENGTH.getInt();
        if (!admin && max > 0 && reason.length() > max) {
            context.var("max", max);
            context.sendError(JailsLang.COMMAND_JAIL_MEMBER_REASON_TOO_LONG);
            return null;
        }
        return reason;
    }

    /**
     * A reason is shown inside messages: color codes, placeholders and the KingdomsX message
     * syntax ({@code {...}}, {@code %...%}) typed by a player are stripped.
     */
    static String sanitize(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)[&§][0-9a-fk-orx]", "")
                .replaceAll("[§{}%]", "")
                .trim();
    }
}
