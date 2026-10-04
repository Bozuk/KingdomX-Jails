package org.kingdoms.jails.data;

import org.kingdoms.constants.base.KeyedKingdomsObject;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.metadata.KingdomMetadata;
import org.kingdoms.constants.metadata.KingdomMetadataHandler;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.data.database.dataprovider.SectionCreatableDataSetter;
import org.kingdoms.data.database.dataprovider.SectionableDataGetter;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.util.Codec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * When the kingdom last jailed each player, stored as {@code Jails:COOLDOWNS}. Drives the
 * {@code jailing.cooldown}; entries older than the cooldown are dropped on save.
 */
public final class CooldownsMetaHandler extends KingdomMetadataHandler {
    public static final CooldownsMetaHandler INSTANCE = new CooldownsMetaHandler();

    private CooldownsMetaHandler() {
        super(new Namespace("Jails", "COOLDOWNS"));
    }

    @Override
    public KingdomMetadata deserialize(KeyedKingdomsObject<?> container,
                                       DeserializationContext<SectionableDataGetter> context) {
        CooldownsMeta meta = new CooldownsMeta();
        for (Map.Entry<String, String> entry : Codec.decode(context.getDataProvider().asString()).entrySet()) {
            try {
                meta.jailedAt.put(UUID.fromString(entry.getKey()), Long.parseLong(entry.getValue()));
            } catch (IllegalArgumentException ignored) {
                // A corrupted entry only loses one cooldown.
            }
        }
        return meta;
    }

    public static final class CooldownsMeta implements KingdomMetadata {
        private final Map<UUID, Long> jailedAt = new ConcurrentHashMap<>();

        /** @return when this kingdom last jailed the player, {@code 0} if never (or long ago). */
        public long getLastJailed(UUID player) {
            Long time = jailedAt.get(player);
            return time == null ? 0 : time;
        }

        public void setLastJailed(UUID player, long time) {
            jailedAt.put(player, time);
        }

        public void clear(UUID player) {
            jailedAt.remove(player);
        }

        @Override
        public Object getValue() {
            return this;
        }

        @Override
        public void setValue(Object value) {
            // Only changed through KingdomJails.
        }

        @Override
        public void serialize(KeyedKingdomsObject<?> container,
                              SerializationContext<SectionCreatableDataSetter> context) {
            context.getDataProvider().setString(Codec.encode(liveEntries()));
        }

        @Override
        public boolean shouldSave(KeyedKingdomsObject<?> container) {
            return !liveEntries().isEmpty();
        }

        private Map<String, String> liveEntries() {
            long cooldown = JailsConfig.JAILING_COOLDOWN.getMillis();
            long now = System.currentTimeMillis();

            Map<String, String> live = new LinkedHashMap<>();
            for (Map.Entry<UUID, Long> entry : jailedAt.entrySet()) {
                if (now - entry.getValue() < cooldown) {
                    live.put(entry.getKey().toString(), Long.toString(entry.getValue()));
                }
            }
            return live;
        }

        @Override
        public String toString() {
            return "CooldownsMeta" + jailedAt;
        }
    }
}
