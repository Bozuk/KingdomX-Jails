package org.kingdoms.jails.util;

import org.bukkit.entity.Entity;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.scheduler.DelayedRepeatingTask;
import org.kingdoms.scheduler.TaskThreadType;

import java.time.Duration;

/**
 * Every task of the addon goes through the KingdomsX scheduler, which already knows whether it
 * runs on Folia (global region + one scheduler per entity) or on a regular server (main thread).
 * Kept in one place so a signature change in KingdomsX only has one place to break.
 */
public final class Scheduling {
    private Scheduling() {}

    /** Runs on the thread that owns this entity - the main thread outside of Folia. */
    public static void runFor(Entity entity, Runnable task) {
        Kingdoms.minecraftTaskScheduler(TaskThreadType.SYNC).of(entity).execute(task);
    }

    /** Runs on the thread that owns this entity, after a delay. */
    public static void runLaterFor(Entity entity, Duration delay, Runnable task) {
        if (delay == null || delay.isZero() || delay.isNegative()) {
            runFor(entity, task);
            return;
        }
        Kingdoms.minecraftTaskScheduler(TaskThreadType.SYNC).of(entity).delayed(delay, task);
    }

    /** Runs on the global thread - the main thread outside of Folia. */
    public static void runGlobal(Runnable task) {
        Kingdoms.taskScheduler().sync().execute(task);
    }

    /** Runs on the global thread, after a delay. */
    public static void runGlobalLater(Duration delay, Runnable task) {
        if (delay == null || delay.isZero() || delay.isNegative()) {
            runGlobal(task);
            return;
        }
        Kingdoms.taskScheduler().sync().delayed(delay, task);
    }

    /** A repeating task on the global thread. */
    public static DelayedRepeatingTask repeatGlobal(Duration delay, Duration period, Runnable task) {
        return Kingdoms.taskScheduler().sync().repeating(delay, period, task);
    }
}
