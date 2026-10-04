package org.kingdoms.jails.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.jails.JailsAddon;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.JailType;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.data.PendingReleaseMetaHandler;
import org.kingdoms.jails.data.ReleaseReason;
import org.kingdoms.jails.events.PlayerJailEvent;
import org.kingdoms.jails.events.PlayerUnjailEvent;
import org.kingdoms.jails.util.Locations;
import org.kingdoms.jails.util.Scheduling;
import org.kingdoms.jails.util.Teleports;
import org.kingdoms.platform.bukkit.adapters.BukkitAdapter;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single place where players go in and out of jail, with every side effect: the API events,
 * the teleports, the messages and the notifications.
 * <p>
 * Requirements (permissions, cooldowns, targets...) are checked by the callers - the commands and
 * the invasion listener - since each of them has its own rules. This class assumes the decision
 * is made.
 */
public final class JailService {
    /** Players this addon is teleporting right now, the teleport restriction lets them through. */
    private static final Set<UUID> TELEPORTING = ConcurrentHashMap.newKeySet();

    private JailService() {}

    // ------------------------------------------------------------------ jailing

    /**
     * Puts a player in the kingdom's jail.
     *
     * @param issuer   the member who jailed them, {@code null} for the server.
     * @param duration the sentence in millis, {@code 0} for no time limit.
     * @param bail     {@code 0} for no bail.
     * @return the new session, or {@code null} if an API listener cancelled it.
     */
    public static JailSession jail(Kingdom kingdom, OfflinePlayer target, UUID issuer, JailType type,
                                   long duration, double bail, String reason) {
        Player online = target.getPlayer();
        String previous = online != null ? Locations.serialize(online.getLocation()) : null;

        JailSession session = new JailSession(target.getUniqueId(), kingdom.getId(), issuer, type,
                System.currentTimeMillis(), duration, bail, reason, previous);

        PlayerJailEvent event = new PlayerJailEvent(kingdom, session);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;

        KingdomJails.addSession(kingdom, session);
        KingdomJails.setLastJailed(kingdom, target.getUniqueId(), session.getSince());

        boolean teleport = JailsConfig.JAILING_TELEPORT_TO_JAIL.getBoolean() && KingdomJails.hasJailLocation(kingdom);
        if (online != null && online.isOnline()) {
            JailsLang.NOTIFICATIONS_JAILED_PRISONER.sendMessage(online, JailMessages.placeholders(session, online));
            session.setNotified(true);
            if (teleport) teleportToJail(online, kingdom, session);
        } else {
            session.setPendingTeleport(teleport);
        }

        notifyJailed(kingdom, session);
        return session;
    }

    private static void notifyJailed(Kingdom kingdom, JailSession session) {
        if (JailsConfig.NOTIFICATIONS_JAILER_KINGDOM.getBoolean()) {
            JailMessages.notifyMembers(kingdom, JailsLang.NOTIFICATIONS_JAILED_JAILER_KINGDOM, session, session.getPrisoner());
        }

        if (JailsConfig.NOTIFICATIONS_PRISONER_KINGDOM.getBoolean()) {
            KingdomPlayer prisoner = KingdomPlayer.getKingdomPlayer(session.getPrisoner());
            Kingdom own = prisoner == null ? null : prisoner.getKingdom();
            if (own != null && !own.getId().equals(kingdom.getId())) {
                JailMessages.notifyMembers(own, JailsLang.NOTIFICATIONS_JAILED_PRISONER_KINGDOM, session, session.getPrisoner());
            }
        }

        if (JailsConfig.NOTIFICATIONS_BROADCAST.getBoolean()) {
            JailMessages.broadcast(JailsLang.NOTIFICATIONS_JAILED_BROADCAST, session);
        }
    }

    // ---------------------------------------------------------------- releasing

    /** Releases a prisoner from whichever kingdom holds them. */
    public static boolean release(JailSession session, ReleaseReason reason) {
        return release(Kingdom.getKingdom(session.getKingdom()), session, reason);
    }

    /**
     * @param kingdom the kingdom holding the prisoner, {@code null} if it no longer exists.
     * @return {@code false} if an API listener kept the prisoner in.
     */
    public static boolean release(Kingdom kingdom, JailSession session, ReleaseReason reason) {
        PlayerUnjailEvent event = new PlayerUnjailEvent(kingdom, session, reason);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        if (kingdom != null) KingdomJails.removeSession(kingdom, session.getPrisoner());
        else KingdomJails.forgetKingdom(session.getKingdom());

        boolean teleport = JailsConfig.RELEASE_TELEPORT_REASONS.getUpperCaseList().contains(reason.name());
        String kingdomName = kingdom == null ? "?" : kingdom.getName();
        String jailerHome = kingdom == null ? null : homeOf(kingdom);

        Player online = Bukkit.getPlayer(session.getPrisoner());
        if (online != null && online.isOnline()) {
            JailsLang.NOTIFICATIONS_RELEASED_PRISONER.sendMessage(online, JailMessages.concat(
                    JailMessages.placeholders(session, online),
                    "release_reason", JailMessages.reasonName(reason).parse(online)));
            if (teleport) teleportOnRelease(online, jailerHome, session.getPreviousLocationRaw());
        } else {
            KingdomPlayer offline = KingdomPlayer.getKingdomPlayer(session.getPrisoner());
            if (offline != null) {
                KingdomJails.setPendingRelease(offline, new PendingReleaseMetaHandler.PendingRelease(
                        reason, kingdomName, teleport, jailerHome, session.getPreviousLocationRaw()));
            }
        }

        if (kingdom != null && JailsConfig.NOTIFICATIONS_JAILER_KINGDOM.getBoolean()) {
            JailsLang message = reason == ReleaseReason.ESCAPED
                    ? JailsLang.NOTIFICATIONS_ESCAPED_KINGDOM
                    : JailsLang.NOTIFICATIONS_RELEASED_KINGDOM;
            for (Player member : kingdom.getOnlineMembers()) {
                if (member.getUniqueId().equals(session.getPrisoner())) continue;
                message.sendMessage(member, JailMessages.concat(
                        JailMessages.placeholders(session, member),
                        "release_reason", JailMessages.reasonName(reason).parse(member)));
            }
        }
        return true;
    }

    /** Releases every prisoner of the kingdom. */
    public static int releaseAll(Kingdom kingdom, ReleaseReason reason) {
        int released = 0;
        for (JailSession session : KingdomJails.getPrisoners(kingdom)) {
            if (release(kingdom, session, reason)) released++;
        }
        return released;
    }

    /**
     * Removes the kingdom's jail location, then applies {@code jail.location.on-remove}: the
     * prisoners either stay jailed (without a place to be sent to) or are all released.
     *
     * @return how many prisoners were released.
     */
    public static int removeJailLocation(Kingdom kingdom) {
        KingdomJails.removeJailLocation(kingdom);
        if (!"RELEASE".equals(JailsConfig.JAIL_LOCATION_ON_REMOVE.getEnumName())) return 0;
        return releaseAll(kingdom, ReleaseReason.JAIL_REMOVED);
    }

    // ---------------------------------------------------------------- on login

    /** Hands out what happened while the player was offline. Runs on the player's own thread. */
    public static void handleJoin(Player player) {
        KingdomPlayer kingdomPlayer = KingdomPlayer.getKingdomPlayer(player);
        PendingReleaseMetaHandler.PendingRelease pending = kingdomPlayer == null ? null : KingdomJails.getPendingRelease(kingdomPlayer);
        if (pending != null) {
            KingdomJails.setPendingRelease(kingdomPlayer, null);
            JailsLang.NOTIFICATIONS_RELEASED_PRISONER.sendMessage(player,
                    "kingdom", pending.getKingdomName() == null ? "?" : pending.getKingdomName(),
                    "release_reason", JailMessages.reasonName(pending.getReason()).parse(player));
            if (pending.shouldTeleport()) teleportOnRelease(player, pending.getJailerHome(), pending.getPreviousLocation());
        }

        JailSession session = KingdomJails.getSession(player.getUniqueId());
        if (session == null) return;
        session.pauseClock();

        boolean countOffline = JailsConfig.RELEASE_TIME_COUNT_OFFLINE.getBoolean();
        if (JailsConfig.RELEASE_TIME_ENABLED.getBoolean() && session.isServed(countOffline)) {
            release(session, ReleaseReason.TIME_SERVED);
            return;
        }

        // Jailed while offline: they learn about it now. Otherwise, a reminder.
        JailsLang message = session.isNotified() ? JailsLang.NOTIFICATIONS_REMINDER : JailsLang.NOTIFICATIONS_JAILED_PRISONER;
        message.sendMessage(player, JailMessages.placeholders(session, player));
        session.setNotified(true);

        Kingdom kingdom = Kingdom.getKingdom(session.getKingdom());
        if (session.isPendingTeleport() && JailsConfig.JAIL_TELEPORT_ON_JOIN.getBoolean() && kingdom != null) {
            teleportToJail(player, kingdom, session);
        }
    }

    // ---------------------------------------------------------------- teleports

    /** Whether this addon is the one teleporting the player right now. */
    public static boolean isTeleporting(UUID player) {
        return TELEPORTING.contains(player);
    }

    /** Moves the prisoner into the jail, on the prisoner's own thread. */
    public static void teleportToJail(Player player, Kingdom kingdom, JailSession session) {
        Location jail = KingdomJails.getJailLocation(kingdom);
        if (jail == null) {
            session.setPendingTeleport(false);
            return;
        }

        session.setPendingTeleport(false);
        Scheduling.runFor(player, () -> {
            UUID id = player.getUniqueId();
            TELEPORTING.add(id);
            Teleports.teleport(player, jail).whenComplete((moved, error) -> {
                TELEPORTING.remove(id);
                if (error != null || !Boolean.TRUE.equals(moved)) {
                    // Another plugin refused: try again on the next login.
                    session.setPendingTeleport(true);
                    if (error != null) {
                        JailsAddon.get().getLogger().warning("Could not teleport " + player.getName() + " to the jail: " + error);
                    }
                }
            });
        });
    }

    /** A teleport the teleport restriction lets through, on the player's own thread. */
    public static void teleport(Player player, Location location) {
        Scheduling.runFor(player, () -> teleportNow(player, location));
    }

    /** Sends a released player to the first available release destination. */
    private static void teleportOnRelease(Player player, String jailerHome, String previous) {
        Scheduling.runFor(player, () -> {
            Location destination = releaseDestination(player, jailerHome, previous);
            if (destination != null) teleportNow(player, destination);
        });
    }

    private static void teleportNow(Player player, Location location) {
        UUID id = player.getUniqueId();
        TELEPORTING.add(id);
        Teleports.teleport(player, location).whenComplete((moved, error) -> TELEPORTING.remove(id));
    }

    /**
     * The first available destination of {@code release.teleport.destinations}, or {@code null} to
     * leave the player where they stand.
     */
    static Location releaseDestination(Player player, String jailerHome, String previous) {
        List<String> destinations = JailsConfig.RELEASE_TELEPORT_DESTINATIONS.getUpperCaseList();
        for (String destination : destinations) {
            Location location = null;
            try {
                switch (destination) {
                    case "NONE":
                        return null;
                    case "OWN_KINGDOM_HOME": {
                        KingdomPlayer kingdomPlayer = KingdomPlayer.getKingdomPlayer(player);
                        Kingdom own = kingdomPlayer == null ? null : kingdomPlayer.getKingdom();
                        location = own == null ? null : Locations.deserialize(homeOf(own));
                        break;
                    }
                    case "JAILER_KINGDOM_HOME":
                        location = Locations.deserialize(jailerHome);
                        break;
                    case "PREVIOUS_LOCATION":
                        location = Locations.deserialize(previous);
                        break;
                    case "BED_SPAWN":
                        location = player.getBedSpawnLocation();
                        break;
                    case "WORLD_SPAWN":
                        location = player.getWorld().getSpawnLocation();
                        break;
                    case "MAIN_WORLD_SPAWN":
                        location = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation();
                        break;
                    default:
                        JailsAddon.get().getLogger().warning("Unknown release destination in jails.yml: " + destination);
                }
            } catch (RuntimeException | LinkageError ex) {
                // A KingdomsX API that moved: skip that destination rather than break the release.
                JailsAddon.get().getLogger().warning("Release destination " + destination + " failed: " + ex);
            }
            if (location != null && location.getWorld() != null) return location;
        }
        return null;
    }

    /** The kingdom's home, serialized, {@code null} if it has none. */
    static String homeOf(Kingdom kingdom) {
        try {
            org.kingdoms.server.location.Location home = kingdom.getHome();
            return home == null ? null : Locations.serialize(BukkitAdapter.adapt(home));
        } catch (RuntimeException | LinkageError ex) {
            return null;
        }
    }
}
