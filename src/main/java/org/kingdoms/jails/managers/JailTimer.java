package org.kingdoms.jails.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.data.ReleaseReason;
import org.kingdoms.jails.util.Durations;
import org.kingdoms.jails.util.Scheduling;
import org.kingdoms.scheduler.DelayedRepeatingTask;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts the time served and lets prisoners out once their sentence is over - online or offline,
 * see {@code release.time.count-offline-time}. Also sends the periodic reminder and keeps the
 * prisoner index in sync with the kingdoms.
 */
public final class JailTimer {
    private static final long REINDEX_EVERY = 60_000L;

    private static DelayedRepeatingTask task;
    private static long lastReindex;
    private static final Map<UUID, Long> LAST_REMINDER = new ConcurrentHashMap<>();

    private JailTimer() {}

    public static synchronized void start() {
        stop();
        long interval = Math.max(1000L, Durations.parseOr(JailsConfig.TIMER_INTERVAL.getString(), 1000L));
        task = Scheduling.repeatGlobal(Duration.ofSeconds(1), Duration.ofMillis(interval), JailTimer::tick);
    }

    public static synchronized void stop() {
        if (task != null) {
            try {
                task.cancel();
            } catch (RuntimeException ignored) {
                // Already cancelled by the scheduler shutting down.
            }
            task = null;
        }
    }

    private static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastReindex > REINDEX_EVERY) {
            lastReindex = now;
            KingdomJails.reindex();
        }

        boolean timeRelease = JailsConfig.RELEASE_TIME_ENABLED.getBoolean();
        boolean countOffline = JailsConfig.RELEASE_TIME_COUNT_OFFLINE.getBoolean();
        long reminderInterval = JailsConfig.NOTIFICATIONS_REMINDER_INTERVAL.getMillis();

        for (JailSession session : KingdomJails.getAllSessions()) {
            Player player = Bukkit.getPlayer(session.getPrisoner());
            boolean online = player != null && player.isOnline();

            if (online) session.tickOnline(now);

            if (timeRelease && session.isServed(countOffline)) {
                JailService.release(session, ReleaseReason.TIME_SERVED);
                LAST_REMINDER.remove(session.getPrisoner());
                continue;
            }

            if (online && reminderInterval > 0) {
                Long last = LAST_REMINDER.get(session.getPrisoner());
                if (last == null) {
                    LAST_REMINDER.put(session.getPrisoner(), now);
                } else if (now - last >= reminderInterval) {
                    LAST_REMINDER.put(session.getPrisoner(), now);
                    JailsLang.NOTIFICATIONS_REMINDER.sendMessage(player, JailMessages.placeholders(session, player));
                }
            }
        }
    }

    public static void forget(UUID player) {
        LAST_REMINDER.remove(player);
    }
}
