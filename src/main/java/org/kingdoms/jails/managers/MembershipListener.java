package org.kingdoms.jails.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.events.general.KingdomDisbandEvent;
import org.kingdoms.events.members.KingdomLeaveEvent;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.data.ReleaseReason;
import org.kingdoms.jails.util.Scheduling;

import java.time.Duration;
import java.util.UUID;

/**
 * Everything tied to a player's life cycle: logging in and out, respawning, leaving the kingdom,
 * and the kingdom itself disappearing.
 */
public final class MembershipListener implements Listener {
    private static final Duration ONE_TICK = Duration.ofMillis(50);

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // A tick later: KingdomsX loads the player's data on join, and other plugins may still
        // move the player around during the join event itself.
        Scheduling.runLaterFor(player, ONE_TICK, () -> {
            if (player.isOnline()) JailService.handleJoin(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        JailSession session = KingdomJails.getSession(id);
        if (session != null) session.pauseClock();

        JailMessages.forget(id);
        JailTimer.forget(id);
    }

    /** {@code jail.respawn-in-jail}: dying is not a way out. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!JailsConfig.JAIL_RESPAWN_IN_JAIL.getBoolean()) return;

        JailSession session = KingdomJails.getSession(event.getPlayer().getUniqueId());
        if (session == null) return;

        Kingdom kingdom = Kingdom.getKingdom(session.getKingdom());
        Location jail = kingdom == null ? null : KingdomJails.getJailLocation(kingdom);
        if (jail != null) event.setRespawnLocation(jail);
    }

    /**
     * A member of the jailing kingdom who leaves it walks out of its jail. Released a tick later,
     * once they are actually out: the release teleport would otherwise send them to the home of
     * the kingdom they are leaving.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeave(KingdomLeaveEvent event) {
        if (!JailsConfig.RELEASE_LEAVE_KINGDOM_ENABLED.getBoolean()) return;

        KingdomPlayer player = event.getPlayer();
        Kingdom kingdom = event.getKingdom();
        if (player == null || kingdom == null) return;

        JailSession session = KingdomJails.getSession(player.getId());
        if (session == null || !session.getKingdom().equals(kingdom.getId())) return;

        String reason = event.getReason() == null ? "LEFT" : event.getReason().name();
        if (!JailsConfig.RELEASE_LEAVE_KINGDOM_REASONS.getUpperCaseList().contains(reason)) return;

        Scheduling.runGlobalLater(ONE_TICK, () -> {
            JailSession current = KingdomJails.getSession(player.getId());
            if (current == session) JailService.release(kingdom, session, ReleaseReason.LEFT_KINGDOM);
        });
    }

    /** A disbanded kingdom has no jail anymore. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDisband(KingdomDisbandEvent event) {
        Kingdom kingdom = event.getKingdom();
        if (kingdom == null) return;

        JailService.releaseAll(kingdom, ReleaseReason.KINGDOM_DISBANDED);
        KingdomJails.forgetKingdom(kingdom.getId());
    }
}
