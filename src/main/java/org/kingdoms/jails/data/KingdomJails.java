package org.kingdoms.jails.data;

import org.bukkit.Location;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.metadata.KingdomMetadata;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.jails.util.Locations;
import org.kingdoms.main.Kingdoms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The public entry point to the data of the addon: jail locations, prisoners, cooldowns.
 * <p>
 * Reading and writing here has no side effect - no teleport, no message, no event. Jailing and
 * releasing a player for real goes through {@link org.kingdoms.jails.managers.JailService}.
 * <p>
 * Sessions live in the data of the jailing kingdom. This class keeps an index from prisoner to
 * kingdom so that "is this player jailed?" - asked on every move, chat line and command - never
 * has to walk every kingdom.
 */
public final class KingdomJails {
    private static final Map<UUID, UUID> INDEX = new ConcurrentHashMap<>();

    private KingdomJails() {}

    // ------------------------------------------------------------------ index

    /** Called whenever a kingdom's prisoners are loaded from the database. */
    static void index(Collection<JailSession> sessions) {
        for (JailSession session : sessions) INDEX.put(session.getPrisoner(), session.getKingdom());
    }

    /** Rebuilds the index from every kingdom. Cheap: kingdoms are always in memory. */
    public static void reindex() {
        Map<UUID, UUID> fresh = new ConcurrentHashMap<>();
        for (Kingdom kingdom : allKingdoms()) {
            PrisonersMetaHandler.PrisonersMeta meta = prisonersMeta(kingdom, false);
            if (meta == null) continue;
            for (JailSession session : meta.getSessions()) fresh.put(session.getPrisoner(), kingdom.getId());
        }
        INDEX.keySet().retainAll(fresh.keySet());
        INDEX.putAll(fresh);
    }

    public static Collection<Kingdom> allKingdoms() {
        try {
            return Kingdoms.get().getDataCenter().getKingdomManager().getKingdoms();
        } catch (RuntimeException ex) {
            return Collections.emptyList();
        }
    }

    // ---------------------------------------------------------------- sessions

    /** @return the player's jail session, or {@code null} if they are free. */
    public static JailSession getSession(UUID player) {
        if (player == null) return null;
        UUID kingdomId = INDEX.get(player);
        if (kingdomId == null) return null;

        Kingdom kingdom = Kingdom.getKingdom(kingdomId);
        PrisonersMetaHandler.PrisonersMeta meta = kingdom == null ? null : prisonersMeta(kingdom, false);
        JailSession session = meta == null ? null : meta.get(player);

        // The kingdom is gone or no longer holds them: the index was stale.
        if (session == null) INDEX.remove(player, kingdomId);
        return session;
    }

    public static boolean isJailed(UUID player) {
        return getSession(player) != null;
    }

    /** @return every prisoner of the kingdom's jail. */
    public static List<JailSession> getPrisoners(Kingdom kingdom) {
        PrisonersMetaHandler.PrisonersMeta meta = prisonersMeta(kingdom, false);
        return meta == null ? Collections.<JailSession>emptyList() : new ArrayList<>(meta.getSessions());
    }

    public static int countPrisoners(Kingdom kingdom) {
        PrisonersMetaHandler.PrisonersMeta meta = prisonersMeta(kingdom, false);
        return meta == null ? 0 : meta.size();
    }

    /** @return every session of every kingdom. */
    public static List<JailSession> getAllSessions() {
        List<JailSession> sessions = new ArrayList<>();
        for (UUID prisoner : new ArrayList<>(INDEX.keySet())) {
            JailSession session = getSession(prisoner);
            if (session != null) sessions.add(session);
        }
        return sessions;
    }

    /** Stores a session. Does not check anything: {@code JailService} does. */
    public static void addSession(Kingdom kingdom, JailSession session) {
        prisonersMeta(kingdom, true).put(session);
        INDEX.put(session.getPrisoner(), kingdom.getId());
    }

    /** @return the removed session, {@code null} if the player wasn't held by this kingdom. */
    public static JailSession removeSession(Kingdom kingdom, UUID prisoner) {
        INDEX.remove(prisoner, kingdom.getId());
        PrisonersMetaHandler.PrisonersMeta meta = prisonersMeta(kingdom, false);
        if (meta == null) return null;

        JailSession removed = meta.remove(prisoner);
        if (meta.size() == 0) kingdom.getMetadata().remove(PrisonersMetaHandler.INSTANCE);
        return removed;
    }

    /** Forgets the sessions of a kingdom that no longer exists. */
    public static void forgetKingdom(UUID kingdomId) {
        INDEX.values().removeIf(kingdomId::equals);
    }

    private static PrisonersMetaHandler.PrisonersMeta prisonersMeta(Kingdom kingdom, boolean create) {
        if (kingdom == null) return null;
        KingdomMetadata metadata = kingdom.getMetadata().get(PrisonersMetaHandler.INSTANCE);
        if (metadata instanceof PrisonersMetaHandler.PrisonersMeta) return (PrisonersMetaHandler.PrisonersMeta) metadata;
        if (!create) return null;

        PrisonersMetaHandler.PrisonersMeta meta = new PrisonersMetaHandler.PrisonersMeta();
        kingdom.getMetadata().put(PrisonersMetaHandler.INSTANCE, meta);
        return meta;
    }

    // ---------------------------------------------------------------- location

    /** @return the jail location, {@code null} if none is set or its world isn't loaded. */
    public static Location getJailLocation(Kingdom kingdom) {
        return Locations.deserialize(getJailLocationRaw(kingdom));
    }

    /** The jail location as stored, even when its world isn't loaded. */
    public static String getJailLocationRaw(Kingdom kingdom) {
        if (kingdom == null) return null;
        KingdomMetadata metadata = kingdom.getMetadata().get(JailLocationMetaHandler.INSTANCE);
        if (metadata == null) return null;

        Object value = metadata.getValue();
        return value == null || String.valueOf(value).isEmpty() ? null : String.valueOf(value);
    }

    public static boolean hasJailLocation(Kingdom kingdom) {
        return getJailLocationRaw(kingdom) != null;
    }

    public static void setJailLocation(Kingdom kingdom, Location location) {
        String serialized = Locations.serialize(location);
        if (serialized == null) {
            removeJailLocation(kingdom);
            return;
        }
        kingdom.getMetadata().put(JailLocationMetaHandler.INSTANCE, new JailLocationMetaHandler.LocationMeta(serialized));
    }

    public static void removeJailLocation(Kingdom kingdom) {
        kingdom.getMetadata().remove(JailLocationMetaHandler.INSTANCE);
    }

    // --------------------------------------------------------------- cooldowns

    /**
     * @param kingdom the kingdom asking, or {@code null} for the most recent jailing by any kingdom.
     * @return when the player was last jailed, epoch millis, {@code 0} if never.
     */
    public static long getLastJailed(UUID player, Kingdom kingdom) {
        if (kingdom != null) {
            CooldownsMetaHandler.CooldownsMeta meta = cooldownsMeta(kingdom, false);
            return meta == null ? 0 : meta.getLastJailed(player);
        }

        long last = 0;
        for (Kingdom any : allKingdoms()) {
            CooldownsMetaHandler.CooldownsMeta meta = cooldownsMeta(any, false);
            if (meta != null) last = Math.max(last, meta.getLastJailed(player));
        }
        return last;
    }

    public static void setLastJailed(Kingdom kingdom, UUID player, long time) {
        cooldownsMeta(kingdom, true).setLastJailed(player, time);
    }

    /** Clears the cooldown of a player in every kingdom. */
    public static void clearCooldown(UUID player) {
        for (Kingdom kingdom : allKingdoms()) {
            CooldownsMetaHandler.CooldownsMeta meta = cooldownsMeta(kingdom, false);
            if (meta != null) meta.clear(player);
        }
    }

    private static CooldownsMetaHandler.CooldownsMeta cooldownsMeta(Kingdom kingdom, boolean create) {
        if (kingdom == null) return null;
        KingdomMetadata metadata = kingdom.getMetadata().get(CooldownsMetaHandler.INSTANCE);
        if (metadata instanceof CooldownsMetaHandler.CooldownsMeta) return (CooldownsMetaHandler.CooldownsMeta) metadata;
        if (!create) return null;

        CooldownsMetaHandler.CooldownsMeta meta = new CooldownsMetaHandler.CooldownsMeta();
        kingdom.getMetadata().put(CooldownsMetaHandler.INSTANCE, meta);
        return meta;
    }

    // --------------------------------------------------------- pending releases

    public static PendingReleaseMetaHandler.PendingRelease getPendingRelease(KingdomPlayer player) {
        KingdomMetadata metadata = player.getMetadata().get(PendingReleaseMetaHandler.INSTANCE);
        return metadata instanceof PendingReleaseMetaHandler.PendingRelease
                ? (PendingReleaseMetaHandler.PendingRelease) metadata : null;
    }

    public static void setPendingRelease(KingdomPlayer player, PendingReleaseMetaHandler.PendingRelease release) {
        if (release == null) player.getMetadata().remove(PendingReleaseMetaHandler.INSTANCE);
        else player.getMetadata().put(PendingReleaseMetaHandler.INSTANCE, release);
    }
}
