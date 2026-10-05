package org.kingdoms.jails.data;

import org.kingdoms.constants.base.KeyedKingdomsObject;
import org.kingdoms.constants.land.abstraction.data.DeserializationContext;
import org.kingdoms.constants.land.abstraction.data.SerializationContext;
import org.kingdoms.constants.metadata.KingdomMetadata;
import org.kingdoms.constants.metadata.KingdomMetadataHandler;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.data.database.dataprovider.SectionCreatableDataSetter;
import org.kingdoms.data.database.dataprovider.SectionableDataGetter;

/**
 * The kingdom's jail location, {@code world;x;y;z;yaw;pitch}, stored as {@code Jails:LOCATION}.
 * Kept as text so that a jail in a world that isn't loaded survives a restart untouched.
 */
public final class JailLocationMetaHandler extends KingdomMetadataHandler {
    public static final JailLocationMetaHandler INSTANCE = new JailLocationMetaHandler();

    private JailLocationMetaHandler() {
        super(new Namespace("Jails", "LOCATION"));
    }

    @Override
    public KingdomMetadata deserialize(KeyedKingdomsObject<?> container,
                                       DeserializationContext<SectionableDataGetter> context) {
        return new LocationMeta(context.getDataProvider().asString());
    }

    public static final class LocationMeta implements KingdomMetadata {
        private String location;

        public LocationMeta(String location) {
            this.location = location;
        }

        @Override
        public Object getValue() {
            return location;
        }

        @Override
        public void setValue(Object value) {
            this.location = value == null ? null : String.valueOf(value);
        }

        @Override
        public void serialize(KeyedKingdomsObject<?> container,
                              SerializationContext<SectionCreatableDataSetter> context) {
            context.getDataProvider().setString(location == null ? "" : location);
        }

        @Override
        public boolean shouldSave(KeyedKingdomsObject<?> container) {
            return location != null && !location.isEmpty();
        }

        @Override
        public String toString() {
            return "LocationMeta[" + location + ']';
        }
    }
}
