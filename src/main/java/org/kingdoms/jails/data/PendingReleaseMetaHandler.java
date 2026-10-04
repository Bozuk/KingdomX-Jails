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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A release that happened while the prisoner was offline, stored on the player as
 * {@code Jails:PENDING_RELEASE}: the message and the teleport are handed out on the next login.
 */
public final class PendingReleaseMetaHandler extends KingdomMetadataHandler {
    public static final PendingReleaseMetaHandler INSTANCE = new PendingReleaseMetaHandler();

    private PendingReleaseMetaHandler() {
        super(new Namespace("Jails", "PENDING_RELEASE"));
    }

    @Override
    public KingdomMetadata deserialize(KeyedKingdomsObject<?> container,
                                       DeserializationContext<SectionableDataGetter> context) {
        Map<String, String> record = Codec.decode(context.getDataProvider().asString());
        ReleaseReason reason;
        try {
            reason = ReleaseReason.valueOf(record.get("reason"));
        } catch (RuntimeException ex) {
            reason = ReleaseReason.PLUGIN;
        }
        return new PendingRelease(reason, record.get("kingdom"), "true".equalsIgnoreCase(record.get("teleport")),
                record.get("jailer-home"), record.get("previous"));
    }

    public static final class PendingRelease implements KingdomMetadata {
        private final ReleaseReason reason;
        private final String kingdomName;
        private final boolean teleport;
        private final String jailerHome;
        private final String previousLocation;

        public PendingRelease(ReleaseReason reason, String kingdomName, boolean teleport, String jailerHome,
                              String previousLocation) {
            this.reason = reason;
            this.kingdomName = kingdomName;
            this.teleport = teleport;
            this.jailerHome = jailerHome;
            this.previousLocation = previousLocation;
        }

        public ReleaseReason getReason() {
            return reason;
        }

        /** Name of the kingdom that held the prisoner, at the time of the release. */
        public String getKingdomName() {
            return kingdomName;
        }

        public boolean shouldTeleport() {
            return teleport;
        }

        /** The jailing kingdom's home, serialized, in case it is one of the release destinations. */
        public String getJailerHome() {
            return jailerHome;
        }

        /** Where the prisoner stood when they were jailed, serialized. */
        public String getPreviousLocation() {
            return previousLocation;
        }

        @Override
        public Object getValue() {
            return this;
        }

        @Override
        public void setValue(Object value) {
            // Immutable: replaced as a whole.
        }

        @Override
        public void serialize(KeyedKingdomsObject<?> container,
                              SerializationContext<SectionCreatableDataSetter> context) {
            Map<String, String> record = new LinkedHashMap<>();
            record.put("reason", reason.name());
            if (kingdomName != null) record.put("kingdom", kingdomName);
            record.put("teleport", Boolean.toString(teleport));
            if (jailerHome != null) record.put("jailer-home", jailerHome);
            if (previousLocation != null) record.put("previous", previousLocation);
            context.getDataProvider().setString(Codec.encode(record));
        }

        @Override
        public boolean shouldSave(KeyedKingdomsObject<?> container) {
            return true;
        }
    }
}
