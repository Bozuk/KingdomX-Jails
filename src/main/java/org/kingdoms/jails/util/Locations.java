package org.kingdoms.jails.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;

/**
 * Bukkit locations as plain strings: {@code world;x;y;z;yaw;pitch}.
 * <p>
 * Stored by world <em>name</em>, the same way a server admin reads and writes them. A location in
 * a world that is not loaded resolves to {@code null}, which the callers treat as "no location".
 */
public final class Locations {
    private Locations() {}

    public static String serialize(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return location.getWorld().getName() + ';'
                + format(location.getX()) + ';'
                + format(location.getY()) + ';'
                + format(location.getZ()) + ';'
                + format(location.getYaw()) + ';'
                + format(location.getPitch());
    }

    public static Location deserialize(String text) {
        if (text == null || text.isEmpty()) return null;
        String[] parts = text.split(";");
        if (parts.length < 4) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** The world name of a serialized location, even when that world isn't loaded. */
    public static String worldOf(String text) {
        if (text == null) return null;
        int separator = text.indexOf(';');
        return separator < 0 ? null : text.substring(0, separator);
    }

    /** {@code world, x, y, z} with block coordinates, for messages. */
    public static String describe(Location location) {
        if (location == null || location.getWorld() == null) return "-";
        return location.getWorld().getName() + ", "
                + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private static String format(double value) {
        return String.format(Locale.ENGLISH, "%.3f", value);
    }
}
