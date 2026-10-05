package org.kingdoms.jails.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.events.lands.LandChangeEvent;
import org.kingdoms.events.lands.UnclaimLandEvent;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.data.ReleaseReason;
import org.kingdoms.jails.util.Locations;

import java.util.UUID;

/**
 * The kingdom is responsible for building a proper jail: a prisoner who manages to reach the
 * wilderness is free. Also forgets a jail whose land was unclaimed.
 */
public final class EscapeListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLandChange(LandChangeEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        JailSession session = KingdomJails.getSession(player.getUniqueId());
        if (session == null || JailService.isTeleporting(player.getUniqueId())) return;
        if (!isOutside(session, event.getToLand())) return;

        if (JailsConfig.RELEASE_ESCAPE_ENABLED.getBoolean()) {
            JailService.release(session, ReleaseReason.ESCAPED);
            return;
        }

        if ("TELEPORT_BACK".equals(JailsConfig.RELEASE_ESCAPE_WHEN_DISABLED.getEnumName())
                && !JailPermissions.bypassesRestrictions(player)) {
            // Cancelling the land change cancels the move that caused it.
            event.setCancelled(true);
            JailMessages.restricted(player, JailsLang.NOTIFICATIONS_ESCAPE_BLOCKED);
        }
    }

    /**
     * {@code release.escape.mode}: {@code WILDERNESS} only counts unclaimed land,
     * {@code OUTSIDE_KINGDOM} counts any land the jailing kingdom doesn't own.
     */
    static boolean isOutside(JailSession session, Land land) {
        UUID owner = land == null ? null : land.getKingdomId();
        if (owner == null) return true;
        if ("OUTSIDE_KINGDOM".equals(JailsConfig.RELEASE_ESCAPE_MODE.getEnumName())) {
            return !owner.equals(session.getKingdom());
        }
        return false;
    }

    /** {@code jail.location.remove-on-unclaim}: a jail can't stand in the wilderness. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnclaim(UnclaimLandEvent event) {
        if (!JailsConfig.JAIL_LOCATION_REMOVE_ON_UNCLAIM.getBoolean()) return;

        Kingdom kingdom = event.getKingdom();
        if (kingdom == null) return;

        String raw = KingdomJails.getJailLocationRaw(kingdom);
        org.bukkit.Location jail = Locations.deserialize(raw);
        if (jail == null) return;

        int chunkX = jail.getBlockX() >> 4;
        int chunkZ = jail.getBlockZ() >> 4;
        String world = Locations.worldOf(raw);

        for (SimpleChunkLocation chunk : event.getLandLocations()) {
            if (chunk.getX() == chunkX && chunk.getZ() == chunkZ && chunk.getWorld().equals(world)) {
                JailService.removeJailLocation(kingdom);
                return;
            }
        }
    }
}
