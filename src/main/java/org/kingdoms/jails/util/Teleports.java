package org.kingdoms.jails.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * Teleports that work on Spigot, Paper and Folia alike.
 * <p>
 * Folia refuses the synchronous {@code teleport}: only Paper's {@code teleportAsync} is allowed
 * there. Paper's method is looked up by reflection because the addon compiles against the Spigot
 * API, which does not have it.
 */
public final class Teleports {
    private static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
    private static final Method TELEPORT_ASYNC = findTeleportAsync();

    private Teleports() {}

    public static boolean isFolia() {
        return FOLIA;
    }

    /** @return a future completed with {@code true} once the entity actually moved. */
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Boolean> teleport(Entity entity, Location location) {
        if (entity == null || location == null || location.getWorld() == null) {
            return CompletableFuture.completedFuture(false);
        }

        if (FOLIA && TELEPORT_ASYNC != null) {
            try {
                return (CompletableFuture<Boolean>) TELEPORT_ASYNC.invoke(entity, location,
                        PlayerTeleportEvent.TeleportCause.PLUGIN);
            } catch (ReflectiveOperationException | RuntimeException ex) {
                CompletableFuture<Boolean> failed = new CompletableFuture<>();
                failed.completeExceptionally(ex);
                return failed;
            }
        }

        return CompletableFuture.completedFuture(entity.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN));
    }

    private static Method findTeleportAsync() {
        try {
            return Entity.class.getMethod("teleportAsync", Location.class, PlayerTeleportEvent.TeleportCause.class);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
