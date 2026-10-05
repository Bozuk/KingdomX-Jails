package org.kingdoms.jails.data;

import org.kingdoms.constants.base.KeyedKingdomsObject;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.metadata.KingdomMetadata;
import org.kingdoms.constants.metadata.KingdomMetadataHandler;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.data.database.dataprovider.SectionCreatableDataSetter;
import org.kingdoms.data.database.dataprovider.SectionableDataGetter;
import org.kingdoms.jails.util.Codec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The prisoners a kingdom holds, stored in the kingdom's data as {@code Jails:PRISONERS}.
 * <p>
 * Kept on the kingdom rather than on the players: a kingdom is always loaded, a player's data is
 * not, and a jail that disappears with its kingdom frees its prisoners by construction.
 */
public final class PrisonersMetaHandler extends KingdomMetadataHandler {
    public static final PrisonersMetaHandler INSTANCE = new PrisonersMetaHandler();

    private PrisonersMetaHandler() {
        super(new Namespace("Jails", "PRISONERS"));
    }

    @Override
    public KingdomMetadata deserialize(KeyedKingdomsObject<?> container,
                                       DeserializationContext<SectionableDataGetter> context) {
        UUID kingdom = container.getKey() instanceof UUID ? (UUID) container.getKey() : null;
        PrisonersMeta meta = new PrisonersMeta();
        if (kingdom == null) return meta;

        for (Map<String, String> record : Codec.decodeAll(context.getDataProvider().asString())) {
            JailSession session = JailSession.deserialize(record, kingdom);
            if (session != null) meta.sessions.put(session.getPrisoner(), session);
        }

        KingdomJails.index(meta.sessions.values());
        return meta;
    }

    public static final class PrisonersMeta implements KingdomMetadata {
        private final Map<UUID, JailSession> sessions = new ConcurrentHashMap<>();

        public JailSession get(UUID prisoner) {
            return sessions.get(prisoner);
        }

        public Collection<JailSession> getSessions() {
            return Collections.unmodifiableCollection(sessions.values());
        }

        void put(JailSession session) {
            sessions.put(session.getPrisoner(), session);
        }

        JailSession remove(UUID prisoner) {
            return sessions.remove(prisoner);
        }

        public int size() {
            return sessions.size();
        }

        @Override
        public Object getValue() {
            return this;
        }

        @Override
        public void setValue(Object value) {
            // Sessions are only ever changed through KingdomJails.
        }

        @Override
        public void serialize(KeyedKingdomsObject<?> container,
                              SerializationContext<SectionCreatableDataSetter> context) {
            List<Map<String, String>> records = new ArrayList<>();
            for (JailSession session : sessions.values()) records.add(session.serialize());
            context.getDataProvider().setString(Codec.encodeAll(records));
        }

        @Override
        public boolean shouldSave(KeyedKingdomsObject<?> container) {
            return !sessions.isEmpty();
        }

        @Override
        public String toString() {
            return "PrisonersMeta" + sessions.values();
        }
    }
}
