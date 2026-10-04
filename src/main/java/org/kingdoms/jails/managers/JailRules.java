package org.kingdoms.jails.managers;

import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.data.KingdomJails;

import java.util.UUID;

/** Requirements shared by every way of jailing a player. */
public final class JailRules {
    private JailRules() {}

    /**
     * {@code jailing.cooldown}: how long since the player was last jailed, by this kingdom or by
     * any kingdom depending on {@code jailing.cooldown-per-kingdom}.
     *
     * @return the time left before the player can be jailed again, {@code 0} if they can now.
     */
    public static long cooldownLeft(Kingdom kingdom, UUID player) {
        long cooldown = JailsConfig.JAILING_COOLDOWN.getMillis();
        if (cooldown <= 0) return 0;

        Kingdom scope = JailsConfig.JAILING_COOLDOWN_PER_KINGDOM.getBoolean() ? kingdom : null;
        long last = KingdomJails.getLastJailed(player, scope);
        if (last <= 0) return 0;

        return Math.max(0, last + cooldown - System.currentTimeMillis());
    }

    /** {@code jail.max-prisoners}, {@code 0} meaning no limit. */
    public static int maxPrisoners() {
        return Math.max(0, JailsConfig.JAIL_MAX_PRISONERS.getInt());
    }

    public static boolean isFull(Kingdom kingdom) {
        int max = maxPrisoners();
        return max > 0 && KingdomJails.countPrisoners(kingdom) >= max;
    }
}
