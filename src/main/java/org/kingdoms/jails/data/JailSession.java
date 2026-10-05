package org.kingdoms.jails.data;

import org.bukkit.Location;
import org.kingdoms.jails.util.Locations;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One prisoner held by one kingdom.
 * <p>
 * Sessions are stored inside the jailing kingdom's data (see {@link PrisonersMetaHandler}), so they
 * follow the KingdomsX save and disappear with the kingdom. The class is mutable only for what
 * changes while the prisoner is held: the time served and the pending teleport.
 */
public final class JailSession {
    private final UUID prisoner;
    private final UUID kingdom;
    private final UUID issuer;
    private final JailType type;
    private final long since;
    private final long duration;
    private final double bail;
    private final String reason;
    private final String previousLocation;

    private volatile long served;
    private volatile long lastTick;
    private volatile boolean pendingTeleport;
    private volatile boolean notified;

    public JailSession(UUID prisoner, UUID kingdom, UUID issuer, JailType type, long since, long duration,
                       double bail, String reason, String previousLocation) {
        this.prisoner = prisoner;
        this.kingdom = kingdom;
        this.issuer = issuer;
        this.type = type == null ? JailType.MANUAL : type;
        this.since = since;
        this.duration = Math.max(0, duration);
        this.bail = Math.max(0, bail);
        this.reason = reason;
        this.previousLocation = previousLocation;
    }

    public UUID getPrisoner() {
        return prisoner;
    }

    /** The kingdom whose jail holds the prisoner. */
    public UUID getKingdom() {
        return kingdom;
    }

    /** The member who jailed the prisoner, {@code null} when it was the server (invasions). */
    public UUID getIssuer() {
        return issuer;
    }

    public JailType getType() {
        return type;
    }

    /** When the prisoner was jailed, epoch millis. */
    public long getSince() {
        return since;
    }

    /** Length of the sentence in millis, {@code 0} for a sentence with no time limit. */
    public long getDuration() {
        return duration;
    }

    public boolean isPermanent() {
        return duration <= 0;
    }

    /** {@code 0} when there is no bail: the sentence can't be bought off. */
    public double getBail() {
        return bail;
    }

    public boolean hasBail() {
        return bail > 0;
    }

    public String getReason() {
        return reason;
    }

    /** Where the prisoner stood when they were jailed, {@code null} if unknown. */
    public Location getPreviousLocation() {
        return Locations.deserialize(previousLocation);
    }

    /** {@link #getPreviousLocation()} as stored, {@code world;x;y;z;yaw;pitch}. */
    public String getPreviousLocationRaw() {
        return previousLocation;
    }

    // ------------------------------------------------------------------ time

    /**
     * Time served so far.
     *
     * @param countOffline whether the time spent offline counts, see
     *                     {@code release.time.count-offline-time}.
     */
    public long getServed(boolean countOffline) {
        return countOffline ? Math.max(0, System.currentTimeMillis() - since) : served;
    }

    /** @return the remaining time, {@code 0} once served, {@code -1} for a sentence with no limit. */
    public long getRemaining(boolean countOffline) {
        if (isPermanent()) return -1;
        return Math.max(0, duration - getServed(countOffline));
    }

    public boolean isServed(boolean countOffline) {
        return !isPermanent() && getRemaining(countOffline) <= 0;
    }

    /**
     * Adds the time elapsed since the last call to the online time served. Called by the timer for
     * online prisoners only; the first call after a login only starts the clock.
     */
    public void tickOnline(long now) {
        long last = lastTick;
        if (last > 0 && now > last) served += Math.min(now - last, 60_000L);
        lastTick = now;
    }

    /** Stops the online clock, so the time spent offline is not counted on the next tick. */
    public void pauseClock() {
        lastTick = 0;
    }

    public long getOnlineServed() {
        return served;
    }

    // -------------------------------------------------------------- teleport

    /** The prisoner was jailed while offline and still has to be moved into the jail. */
    public boolean isPendingTeleport() {
        return pendingTeleport;
    }

    public void setPendingTeleport(boolean pendingTeleport) {
        this.pendingTeleport = pendingTeleport;
    }

    /** Whether the prisoner was told they were jailed: not yet if it happened while offline. */
    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    // --------------------------------------------------------- serialization

    Map<String, String> serialize() {
        Map<String, String> record = new LinkedHashMap<>();
        record.put("prisoner", prisoner.toString());
        record.put("kingdom", kingdom.toString());
        if (issuer != null) record.put("issuer", issuer.toString());
        record.put("type", type.name());
        record.put("since", Long.toString(since));
        record.put("duration", Long.toString(duration));
        record.put("bail", Double.toString(bail));
        if (reason != null) record.put("reason", reason);
        if (previousLocation != null) record.put("previous", previousLocation);
        record.put("served", Long.toString(served));
        if (pendingTeleport) record.put("pending-teleport", "true");
        if (notified) record.put("notified", "true");
        return record;
    }

    /** @return {@code null} for a record that can't be read. */
    static JailSession deserialize(Map<String, String> record, UUID kingdom) {
        try {
            UUID prisoner = UUID.fromString(record.get("prisoner"));
            String issuer = record.get("issuer");

            JailSession session = new JailSession(
                    prisoner,
                    kingdom,
                    issuer == null || issuer.isEmpty() ? null : UUID.fromString(issuer),
                    JailType.fromString(record.get("type")),
                    parseLong(record.get("since"), System.currentTimeMillis()),
                    parseLong(record.get("duration"), 0),
                    parseDouble(record.get("bail")),
                    record.get("reason"),
                    record.get("previous"));
            session.served = parseLong(record.get("served"), 0);
            session.pendingTeleport = "true".equalsIgnoreCase(record.get("pending-teleport"));
            session.notified = "true".equalsIgnoreCase(record.get("notified"));
            return session;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static long parseLong(String text, long fallback) {
        if (text == null) return fallback;
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static double parseDouble(String text) {
        if (text == null) return 0;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "JailSession[" + prisoner + " in " + kingdom + ", " + type + ']';
    }
}
