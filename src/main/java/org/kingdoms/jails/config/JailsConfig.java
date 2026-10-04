package org.kingdoms.jails.config;

import org.kingdoms.config.accessor.ConfigAccessor;
import org.kingdoms.config.accessor.EnumConfig;
import org.kingdoms.config.accessor.KeyedConfigAccessor;
import org.kingdoms.config.implementation.KeyedYamlConfigAccessor;
import org.kingdoms.config.managers.ConfigManager;
import org.kingdoms.jails.JailsAddon;
import org.kingdoms.jails.util.Durations;
import org.kingdoms.main.Kingdoms;
import org.kingdoms.utils.config.ConfigPath;
import org.kingdoms.utils.config.adapters.YamlResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Every option of {@code jails.yml}.
 * <p>
 * Each entry spells out its path, section by section, instead of relying on how the enum name
 * would be split: the file has deep sections and explicit paths keep the mapping obvious.
 * A value missing from the server's file falls back to the copy bundled in the jar.
 */
public enum JailsConfig implements EnumConfig {
    // ------------------------------------------------------------------ jail
    JAIL_PERMISSION("jail", "permission"),
    JAIL_LOCATION_REQUIRE_OWN_LAND("jail", "location", "require-own-land"),
    JAIL_LOCATION_ON_REMOVE("jail", "location", "on-remove"),
    JAIL_LOCATION_REMOVE_ON_UNCLAIM("jail", "location", "remove-on-unclaim"),
    JAIL_MAX_PRISONERS("jail", "max-prisoners"),
    JAIL_RESPAWN_IN_JAIL("jail", "respawn-in-jail"),
    JAIL_TELEPORT_ON_JOIN("jail", "teleport-on-join"),
    JAIL_EXEMPT_PERMISSION("jail", "exempt-permission"),
    JAIL_BYPASS_PERMISSION("jail", "bypass-permission"),
    JAIL_DISABLED_WORLDS("jail", "disabled-worlds"),

    // --------------------------------------------------------------- jailing
    JAILING_REQUIRE_LOCATION("jailing", "require-location"),
    JAILING_TELEPORT_TO_JAIL("jailing", "teleport-to-jail"),
    JAILING_COOLDOWN("jailing", "cooldown"),
    JAILING_COOLDOWN_PER_KINGDOM("jailing", "cooldown-per-kingdom"),
    JAILING_DURATION_DEFAULT("jailing", "duration", "default"),
    JAILING_DURATION_ALLOW_CUSTOM("jailing", "duration", "allow-custom"),
    JAILING_DURATION_MIN("jailing", "duration", "min"),
    JAILING_DURATION_MAX("jailing", "duration", "max"),
    JAILING_DURATION_ALLOW_PERMANENT("jailing", "duration", "allow-permanent"),
    JAILING_BAIL_ENABLED("jailing", "bail", "enabled"),
    JAILING_BAIL_DEFAULT("jailing", "bail", "default"),
    JAILING_BAIL_MIN("jailing", "bail", "min"),
    JAILING_BAIL_MAX("jailing", "bail", "max"),
    JAILING_REASON_REQUIRED("jailing", "reason", "required"),
    JAILING_REASON_DEFAULT("jailing", "reason", "default"),
    JAILING_REASON_MAX_LENGTH("jailing", "reason", "max-length"),
    JAILING_TARGETS_ONLINE_ONLY("jailing", "targets", "online-only"),
    JAILING_TARGETS_MUST_BE_IN_OWN_LAND("jailing", "targets", "must-be-in-own-land"),
    JAILING_TARGETS_MEMBERS("jailing", "targets", "members"),
    JAILING_TARGETS_RESPECT_RANKS("jailing", "targets", "respect-rank-hierarchy"),
    JAILING_TARGETS_NO_KINGDOM("jailing", "targets", "no-kingdom"),
    JAILING_TARGETS_RELATIONS("jailing", "targets", "relations"),

    // ----------------------------------------------------------- notifications
    NOTIFICATIONS_JAILER_KINGDOM("notifications", "jailer-kingdom"),
    NOTIFICATIONS_PRISONER_KINGDOM("notifications", "prisoner-kingdom"),
    NOTIFICATIONS_BROADCAST("notifications", "broadcast"),
    NOTIFICATIONS_REMINDER_INTERVAL("notifications", "reminder-interval"),

    // ---------------------------------------------------------------- release
    RELEASE_TIME_ENABLED("release", "time", "enabled"),
    RELEASE_TIME_COUNT_OFFLINE("release", "time", "count-offline-time"),
    RELEASE_BAIL_ENABLED("release", "bail", "enabled"),
    RELEASE_BAIL_RECEIVER("release", "bail", "receiver"),
    RELEASE_BAIL_ALLOW_PAYING_FOR_OTHERS("release", "bail", "allow-paying-for-others"),
    RELEASE_LEAVE_KINGDOM_ENABLED("release", "leave-kingdom", "enabled"),
    RELEASE_LEAVE_KINGDOM_REASONS("release", "leave-kingdom", "reasons"),
    RELEASE_ESCAPE_ENABLED("release", "escape", "enabled"),
    RELEASE_ESCAPE_MODE("release", "escape", "mode"),
    RELEASE_ESCAPE_WHEN_DISABLED("release", "escape", "when-disabled"),
    RELEASE_BY_MEMBERS("release", "by-members"),
    RELEASE_TELEPORT_DESTINATIONS("release", "teleport", "destinations"),
    RELEASE_TELEPORT_REASONS("release", "teleport", "reasons"),

    // -------------------------------------------------------------- invasions
    INVASIONS_AUTO_JAIL_ENABLED("invasions", "auto-jail", "enabled"),
    INVASIONS_AUTO_JAIL_RESULTS("invasions", "auto-jail", "results"),
    INVASIONS_AUTO_JAIL_BAIL("invasions", "auto-jail", "bail"),
    INVASIONS_AUTO_JAIL_BAIL_RECEIVER("invasions", "auto-jail", "bail-receiver"),
    INVASIONS_AUTO_JAIL_DURATION("invasions", "auto-jail", "duration"),
    INVASIONS_AUTO_JAIL_REASON("invasions", "auto-jail", "reason"),
    INVASIONS_AUTO_JAIL_DELAY("invasions", "auto-jail", "delay"),
    INVASIONS_AUTO_JAIL_REQUIRE_LOCATION("invasions", "auto-jail", "require-jail-location"),
    INVASIONS_AUTO_JAIL_IGNORE_COOLDOWN("invasions", "auto-jail", "ignore-cooldown"),
    INVASIONS_AUTO_JAIL_IGNORE_MAX_PRISONERS("invasions", "auto-jail", "ignore-max-prisoners"),
    INVASIONS_AUTO_JAIL_ATTACKERS_IN_LAND("invasions", "auto-jail", "attackers-in-invaded-land"),

    // ----------------------------------------------------------- restrictions
    RESTRICTIONS_TELEPORT_ENABLED("restrictions", "teleport", "enabled"),
    RESTRICTIONS_TELEPORT_BLOCKED_CAUSES("restrictions", "teleport", "blocked-causes"),
    RESTRICTIONS_ITEMS_ENABLED("restrictions", "items", "enabled"),
    RESTRICTIONS_ITEMS_MODE("restrictions", "items", "mode"),
    RESTRICTIONS_ITEMS_ALLOWED("restrictions", "items", "allowed"),
    RESTRICTIONS_ITEMS_BLOCKED("restrictions", "items", "blocked"),
    RESTRICTIONS_ITEMS_CONSUME("restrictions", "items", "consume"),
    RESTRICTIONS_ITEMS_DROP("restrictions", "items", "drop"),
    RESTRICTIONS_ITEMS_PICKUP("restrictions", "items", "pickup"),
    RESTRICTIONS_PVP_PRISONERS_CAN_ATTACK("restrictions", "pvp", "prisoners-can-attack"),
    RESTRICTIONS_PVP_MEMBERS_PROTECTED("restrictions", "pvp", "member-prisoners-protected"),
    RESTRICTIONS_PVP_FOREIGN_ATTACKABLE("restrictions", "pvp", "foreign-prisoners-attackable"),
    RESTRICTIONS_PVP_FOREIGN_ATTACKERS("restrictions", "pvp", "foreign-prisoners-attackers"),
    RESTRICTIONS_PVP_FORCE("restrictions", "pvp", "force-allow"),
    RESTRICTIONS_BLOCKS_BREAK("restrictions", "blocks", "break"),
    RESTRICTIONS_BLOCKS_PLACE("restrictions", "blocks", "place"),
    RESTRICTIONS_BLOCKS_INTERACT("restrictions", "blocks", "interact"),
    RESTRICTIONS_BLOCKS_PHYSICAL("restrictions", "blocks", "physical"),
    RESTRICTIONS_BLOCKS_INTERACT_WHITELIST("restrictions", "blocks", "interact-whitelist"),
    RESTRICTIONS_ENTITIES_INTERACT("restrictions", "entities", "interact"),
    RESTRICTIONS_ENTITIES_DAMAGE("restrictions", "entities", "damage"),
    RESTRICTIONS_CHAT_ENABLED("restrictions", "chat", "enabled"),
    RESTRICTIONS_CHAT_BLOCKED_CHANNELS("restrictions", "chat", "blocked-channels"),
    RESTRICTIONS_COMMANDS_ENABLED("restrictions", "commands", "enabled"),
    RESTRICTIONS_COMMANDS_MODE("restrictions", "commands", "mode"),
    RESTRICTIONS_COMMANDS_LIST("restrictions", "commands", "list"),
    RESTRICTIONS_COMMANDS_ALWAYS_ALLOWED("restrictions", "commands", "always-allowed"),
    RESTRICTIONS_MESSAGE_COOLDOWN("restrictions", "message-cooldown"),

    // ----------------------------------------------------------------- status
    STATUS_OTHERS_ENABLED("status", "others", "enabled"),
    STATUS_OTHERS_PERMISSION("status", "others", "permission"),
    LIST_REQUIRE_PERMISSION("list", "require-permission"),

    // ------------------------------------------------------------------ timer
    TIMER_INTERVAL("timer", "interval"),
    ;

    private static final YamlResource JAILS = new YamlResource(
            JailsAddon.get(),
            Kingdoms.getPath("jails.yml").toFile(),
            "jails.yml"
    ).load();

    static {
        ConfigManager.registerAsMainConfig(JAILS);
        ConfigManager.watch(JAILS);
    }

    /** Triggers the static initializer. Called from the addon's {@code onLoad()}. */
    public static void init() {}

    private final ConfigPath option;

    JailsConfig(String... path) {
        this.option = ConfigPath.of(path);
    }

    @Override
    public KeyedConfigAccessor getManager() {
        return new KeyedYamlConfigAccessor(JAILS, option);
    }

    public static YamlResource getConfig() {
        return JAILS;
    }

    public static ConfigAccessor accessor() {
        return JAILS.accessor();
    }

    // ------------------------------------------------------------ typed access

    public boolean getBoolean() {
        return getManager().getBoolean();
    }

    public int getInt() {
        return getManager().getInt();
    }

    public double getDouble() {
        return getManager().getDouble();
    }

    public String getString() {
        String value = getManager().getString();
        return value == null ? "" : value;
    }

    /** The value as an upper case constant name, e.g. {@code KINGDOM_BANK}. */
    public String getEnumName() {
        return getString().trim().toUpperCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_');
    }

    public List<String> getStringList() {
        List<String> list = getManager().getStringList();
        return list == null ? Collections.<String>emptyList() : list;
    }

    /** Every entry upper cased, for comparisons with enum constants. */
    public List<String> getUpperCaseList() {
        List<String> upper = new ArrayList<>();
        for (String entry : getStringList()) {
            if (entry != null) upper.add(entry.trim().toUpperCase(Locale.ENGLISH).replace('-', '_'));
        }
        return upper;
    }

    /** A duration such as {@code 10m}, in milliseconds. Anything invalid counts as {@code 0}. */
    public long getMillis() {
        return Math.max(0, Durations.parseOr(getString(), 0));
    }
}
