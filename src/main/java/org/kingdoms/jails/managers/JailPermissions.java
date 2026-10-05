package org.kingdoms.jails.managers;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.kingdoms.constants.player.KingdomPermission;
import org.kingdoms.constants.player.KingdomPermissionRegistry;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.constants.player.StandardKingdomPermission;
import org.kingdoms.jails.config.JailsConfig;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/** Who may manage a jail, who escapes the restrictions, who can't be jailed at all. */
public final class JailPermissions {
    private JailPermissions() {}

    /**
     * The kingdom permission that manages the jail, {@code jail.permission}. {@code JAIL} by
     * default, which KingdomsX gives to the king only: add it to the ranks that should have it.
     */
    public static KingdomPermission jailPermission() {
        String name = JailsConfig.JAIL_PERMISSION.getString().trim();
        if (!name.isEmpty()) {
            try {
                Set<KingdomPermission> parsed = KingdomPermissionRegistry.parse(Collections.singletonList(name));
                if (parsed != null && !parsed.isEmpty()) return parsed.iterator().next();
            } catch (RuntimeException ignored) {
                // Unknown permission: fall back to JAIL below.
            }
        }
        return StandardKingdomPermission.JAIL;
    }

    /** Admin mode ({@code /k admin}) counts as having every kingdom permission. */
    public static boolean canManageJail(KingdomPlayer player) {
        if (player == null) return false;
        return player.isAdmin() || player.hasPermission(jailPermission());
    }

    /** Kingdom-side check only, for members who are offline: admin mode doesn't apply. */
    public static boolean holdsJailPermission(KingdomPlayer player) {
        return player != null && player.hasPermission(jailPermission());
    }

    /** {@code jail.bypass-permission}: prisoners holding it ignore every restriction. */
    public static boolean bypassesRestrictions(Player player) {
        String permission = JailsConfig.JAIL_BYPASS_PERMISSION.getString().trim();
        return !permission.isEmpty() && player.hasPermission(permission);
    }

    /** {@code jail.exempt-permission}: players holding it can never be jailed. */
    public static boolean isExempt(Player player) {
        String permission = JailsConfig.JAIL_EXEMPT_PERMISSION.getString().trim();
        return player != null && !permission.isEmpty() && player.hasPermission(permission);
    }

    /** {@code jail.disabled-worlds}. */
    public static boolean isDisabledWorld(World world) {
        if (world == null) return false;
        String name = world.getName().toLowerCase(Locale.ENGLISH);
        for (String disabled : JailsConfig.JAIL_DISABLED_WORLDS.getStringList()) {
            if (disabled != null && disabled.trim().toLowerCase(Locale.ENGLISH).equals(name)) return true;
        }
        return false;
    }
}
