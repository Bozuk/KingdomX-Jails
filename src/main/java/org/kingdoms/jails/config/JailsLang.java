package org.kingdoms.jails.config;

import org.kingdoms.locale.LanguageEntry;
import org.kingdoms.locale.messenger.DefinedMessenger;

/**
 * Every message of the addon, in English.
 * <p>
 * These are the fallback texts. The actual text shown to a player comes from the per-language file
 * of {@code plugins/Kingdoms/jails/languages/}, see {@link org.kingdoms.jails.locale.JailsLanguages}.
 * Anything a language file doesn't override falls back to the value defined here.
 * <p>
 * Entries starting with {@code COMMAND_} are written to the global {@code command:} section of the
 * language files, everything else lands under {@code jails:}. The trailing numbers are the
 * positions at which the enum name is split into path elements.
 */
public enum JailsLang implements DefinedMessenger {

    // ------------------------------------------------------------------ /k jail
    COMMAND_JAIL_NAME("jail"),
    COMMAND_JAIL_ALIASES("jails prison"),
    COMMAND_JAIL_DESCRIPTION("{$s}Manage your kingdom's jail.", 1, 2),

    // ------------------------------------------------------------ /k jail location
    COMMAND_JAIL_LOCATION_DESCRIPTION("{$s}Sets the location of your kingdom's jail.", 1, 2, 3),
    COMMAND_JAIL_LOCATION_USAGE("{$usage}jail location {$p}[remove]", 1, 2, 3),
    COMMAND_JAIL_LOCATION_SET("{$p}The kingdom jail is now at {$s}%location%{$p}.", 1, 2, 3),
    COMMAND_JAIL_LOCATION_ANNOUNCEMENT("{$s}%player% {$p}moved the kingdom jail to {$s}%location%{$p}.", 1, 2, 3),
    COMMAND_JAIL_LOCATION_NOT_OWN_LAND("{$e}The jail has to be inside your kingdom's land.", 1, 2, 3),
    COMMAND_JAIL_LOCATION_DISABLED_WORLD("{$e}Jails are disabled in this world.", 1, 2, 3),
    COMMAND_JAIL_LOCATION_REMOVED("{$p}The kingdom jail was removed.", 1, 2, 3),
    COMMAND_JAIL_LOCATION_NOT_SET("{$e}Your kingdom has no jail. " +
            "hover:{{$es}/k jail location;{$p}Click to set it here;/k jail location}", 1, 2, 3),

    // -------------------------------------------------------------- /k jail member
    COMMAND_JAIL_MEMBER_DESCRIPTION("{$s}Puts a player in your kingdom's jail.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_USAGE("{$usage}jail member {$p}<player> [bail] [duration] [reason]", 1, 2, 3),
    COMMAND_JAIL_MEMBER_NOT_FOUND("{$es}%player% {$e}has never played on this server.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_SELF("{$e}You can't jail yourself.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_ALREADY_JAILED("{$es}%player% {$e}is already in the jail of {$es}%kingdom%{$e}.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_EXEMPT("{$es}%player% {$e}can't be jailed.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_OFFLINE("{$es}%player% {$e}has to be online to be jailed.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_NOT_IN_LAND("{$es}%player% {$e}has to stand in your kingdom's land to be jailed.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_MEMBERS_DISABLED("{$e}You can't jail members of your own kingdom.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_SAME_PERMISSION("{$es}%player% {$e}can manage the jail too, you can't jail them.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_HIGHER_RANK("{$es}%player% {$e}doesn't rank below you, you can't jail them.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_NO_KINGDOM_DISABLED("{$e}You can't jail players who don't belong to a kingdom.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_RELATION_DISABLED("{$e}You can't jail members of {$es}%target_kingdom% {$e}({$es}%relation%{$e}).", 1, 2, 3),
    COMMAND_JAIL_MEMBER_COOLDOWN("{$es}%player% {$e}was jailed recently. Try again in {$es}%time%{$e}.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_MAX_PRISONERS("{$e}Your jail is full: {$es}%max% {$e}prisoners at most.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_DISABLED_WORLD("{$e}Jails are disabled in this world.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_INVALID_BAIL("{$es}%bail% {$e}is not a valid bail. It must be between {$es}%min% {$e}and {$es}%max%{$e}.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_BAIL_DISABLED("{$e}Bails are disabled.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_INVALID_DURATION("{$e}The sentence must last between {$es}%min% {$e}and {$es}%max%{$e}.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_CUSTOM_DURATION_DISABLED("{$e}You can't choose the length of the sentence.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_REASON_REQUIRED("{$e}You have to give a reason.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_REASON_TOO_LONG("{$e}The reason can't be longer than {$es}%max% {$e}characters.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_CANCELLED("{$e}The jailing was cancelled.", 1, 2, 3),
    COMMAND_JAIL_MEMBER_JAILED("{$p}You jailed {$s}%player%{$p}. Sentence{$colon} {$s}%duration%{$p}, bail{$colon} {$s}%bail%", 1, 2, 3),

    // -------------------------------------------------------------- /k jail status
    COMMAND_JAIL_STATUS_DESCRIPTION("{$s}Shows how long a prisoner still has to serve.", 1, 2, 3),
    COMMAND_JAIL_STATUS_USAGE("{$usage}jail status {$p}[player]", 1, 2, 3),
    COMMAND_JAIL_STATUS_NOT_JAILED("{$p}You are not in jail.", 1, 2, 3),
    COMMAND_JAIL_STATUS_NOT_JAILED_OTHER("{$s}%player% {$p}is not in jail.", 1, 2, 3),
    COMMAND_JAIL_STATUS_OTHERS_DISABLED("{$e}You can only see your own jail status.", 1, 2, 3),
    COMMAND_JAIL_STATUS_DISPLAY("{$sep}-=[ {$p}Jail status of {$s}%player% {$sep}]=-\n" +
            "{$p}Jail{$colon} {$s}%kingdom%\n" +
            "{$p}Jailed by{$colon} {$s}%jailer% {$sep}({$s}%type%{$sep})\n" +
            "{$p}Reason{$colon} {$s}%reason%\n" +
            "{$p}Served{$colon} {$s}%served%\n" +
            "{$p}Sentence{$colon} {$s}%duration%\n" +
            "{$p}Remaining{$colon} {$s}%remaining%\n" +
            "{$p}Bail{$colon} {$s}%bail%", 1, 2, 3),

    // ------------------------------------------------------------- /k jail paybail
    COMMAND_JAIL_PAYBAIL_DESCRIPTION("{$s}Pays a bail to get out of jail.", 1, 2, 3),
    COMMAND_JAIL_PAYBAIL_USAGE("{$usage}jail paybail {$p}[player]", 1, 2, 3),
    COMMAND_JAIL_PAYBAIL_DISABLED("{$e}Bails can't be paid on this server.", 1, 2, 3),
    COMMAND_JAIL_PAYBAIL_NOT_JAILED("{$e}You are not in jail.", 1, 2, 3),
    COMMAND_JAIL_PAYBAIL_NOT_JAILED_OTHER("{$es}%player% {$e}is not in jail.", 1, 2, 3),
    COMMAND_JAIL_PAYBAIL_OTHERS_DISABLED("{$e}You can only pay your own bail.", 1, 2, 3),
    COMMAND_JAIL_PAYBAIL_NO_BAIL("{$e}There is no bail for this sentence: it has to be served.", 1, 2, 3),
    COMMAND_JAIL_PAYBAIL_NO_ECONOMY("{$e}No economy plugin was found, bails can't be paid.", 1, 2, 3),
    COMMAND_JAIL_PAYBAIL_NOT_ENOUGH_MONEY("{$e}The bail is {$es}%bail%{$e}, you only have {$es}%balance%{$e}.", 1, 2, 3),
    COMMAND_JAIL_PAYBAIL_PAID_OTHER("{$p}You paid the bail of {$s}%player%{$p}{$colon} {$s}%bail%", 1, 2, 3),
    COMMAND_JAIL_PAYBAIL_CANCELLED("{$e}The release was cancelled, your money was given back.", 1, 2, 3),

    // ------------------------------------------------------------- /k jail release
    COMMAND_JAIL_RELEASE_DESCRIPTION("{$s}Lets a prisoner out of your kingdom's jail.", 1, 2, 3),
    COMMAND_JAIL_RELEASE_USAGE("{$usage}jail release {$p}<player>", 1, 2, 3),
    COMMAND_JAIL_RELEASE_DISABLED("{$e}Prisoners can't be released early on this server.", 1, 2, 3),
    COMMAND_JAIL_RELEASE_NOT_JAILED_HERE("{$es}%player% {$e}is not held in your kingdom's jail.", 1, 2, 3),
    COMMAND_JAIL_RELEASE_RELEASED("{$p}You released {$s}%player%{$p}.", 1, 2, 3),
    COMMAND_JAIL_RELEASE_CANCELLED("{$e}The release was cancelled.", 1, 2, 3),

    // ---------------------------------------------------------------- /k jail list
    COMMAND_JAIL_LIST_DESCRIPTION("{$s}Lists the prisoners of your kingdom's jail.", 1, 2, 3),
    COMMAND_JAIL_LIST_EMPTY("{$p}Your kingdom's jail is empty.", 1, 2, 3),
    COMMAND_JAIL_LIST_HEADER("{$sep}-=[ {$p}Prisoners {$sep}({$s}%count%{$sep}) ]=-\n{$p}Jail{$colon} {$s}%location%", 1, 2, 3),
    COMMAND_JAIL_LIST_ENTRY("{$sep}- hover:{{$s}%player%;{$p}Jailed by {$s}%jailer%\n{$p}Reason{$colon} {$s}%reason%;/k jail status %player%} " +
            "{$sep}| {$p}%remaining% {$sep}| {$p}bail {$s}%bail%", 1, 2, 3),

    // ------------------------------------------------------------ /k admin jail
    COMMAND_ADMIN_JAIL_DESCRIPTION("{$s}Manage the jails of every kingdom.", 1, 2, 3),
    COMMAND_ADMIN_JAIL_MEMBER_DESCRIPTION("{$s}Puts a player in a kingdom's jail, no requirement checked.", 1, 2, 3, 4),
    COMMAND_ADMIN_JAIL_MEMBER_USAGE("{$usage}admin jail member {$p}<kingdom> <player> [bail] [duration] [reason]", 1, 2, 3, 4),
    COMMAND_ADMIN_JAIL_MEMBER_JAILED("{$p}{$s}%player% {$p}is now in the jail of {$s}%kingdom%{$p}.", 1, 2, 3, 4),
    COMMAND_ADMIN_JAIL_RELEASE_DESCRIPTION("{$s}Lets a player out of jail, whatever the kingdom.", 1, 2, 3, 4),
    COMMAND_ADMIN_JAIL_RELEASE_USAGE("{$usage}admin jail release {$p}<player>", 1, 2, 3, 4),
    COMMAND_ADMIN_JAIL_RELEASE_RELEASED("{$p}{$s}%player% {$p}was released from the jail of {$s}%kingdom%{$p}.", 1, 2, 3, 4),
    COMMAND_ADMIN_JAIL_RESETCOOLDOWN_DESCRIPTION("{$s}Clears the jailing cooldown of a player.", 1, 2, 3, 4),
    COMMAND_ADMIN_JAIL_RESETCOOLDOWN_USAGE("{$usage}admin jail resetcooldown {$p}<player>", 1, 2, 3, 4),
    COMMAND_ADMIN_JAIL_RESETCOOLDOWN_DONE("{$p}The jailing cooldown of {$s}%player% {$p}was cleared.", 1, 2, 3, 4),

    // ------------------------------------------------------------------ general
    GENERAL_NO_PERMISSION("{$e}You don't have the kingdom permission to manage the jail.", 1),
    GENERAL_NOT_JAILED("{$es}%player% {$e}is not in jail.", 1),

    // ------------------------------------------------------------ notifications
    NOTIFICATIONS_JAILED_PRISONER("{$e}You have been jailed by {$es}%kingdom%{$e}.\n" +
            "{$e}Reason{$colon} {$es}%reason%\n" +
            "{$e}Sentence{$colon} {$es}%duration%{$e}, bail{$colon} {$es}%bail%\n" +
            "hover:{{$es}/k jail status;{$p}Click to see your status;/k jail status}", 1, 2),
    NOTIFICATIONS_JAILED_JAILER_KINGDOM("{$s}%jailer% {$p}jailed {$s}%player%{$p}. Reason{$colon} {$s}%reason%", 1, 2),
    NOTIFICATIONS_JAILED_PRISONER_KINGDOM("{$e}Your fellow member {$es}%player% {$e}was jailed by {$es}%kingdom%{$e}.", 1, 2),
    NOTIFICATIONS_JAILED_BROADCAST("{$s}%player% {$p}was thrown in the jail of {$s}%kingdom%{$p}.", 1, 2),
    NOTIFICATIONS_REMINDER("{$e}You are in the jail of {$es}%kingdom%{$e}. Remaining{$colon} {$es}%remaining%{$e}, bail{$colon} {$es}%bail%", 1),
    NOTIFICATIONS_RELEASED_PRISONER("{$p}You are out of the jail of {$s}%kingdom%{$colon} {$s}%release_reason%{$p}.", 1, 2),
    NOTIFICATIONS_RELEASED_KINGDOM("{$s}%player% {$p}is out of the jail{$colon} {$s}%release_reason%{$p}.", 1, 2),
    NOTIFICATIONS_ESCAPED_KINGDOM("{$es}%player% {$e}escaped from the jail!", 1, 2),
    NOTIFICATIONS_ESCAPE_BLOCKED("{$e}You can't leave the jail.", 1, 2),

    // --------------------------------------------------------- release reasons
    RELEASE_REASONS_TIME_SERVED("sentence served", 1, 2),
    RELEASE_REASONS_BAIL_PAID("bail paid", 1, 2),
    RELEASE_REASONS_RELEASED("released by the kingdom", 1, 2),
    RELEASE_REASONS_ADMIN("released by an admin", 1, 2),
    RELEASE_REASONS_LEFT_KINGDOM("left the kingdom", 1, 2),
    RELEASE_REASONS_ESCAPED("escaped", 1, 2),
    RELEASE_REASONS_KINGDOM_DISBANDED("the kingdom was disbanded", 1, 2),
    RELEASE_REASONS_JAIL_REMOVED("the jail was removed", 1, 2),
    RELEASE_REASONS_PLUGIN("released", 1, 2),

    // -------------------------------------------------------------- jail types
    TYPES_MANUAL("jailed by a member", 1),
    TYPES_INVASION("failed invasion", 1),
    TYPES_ADMIN("jailed by an admin", 1),

    // ------------------------------------------------------------------ values
    VALUES_NO_BAIL("none", 1),
    VALUES_BAIL("$%amount%", 1),
    VALUES_PERMANENT("until released", 1),
    VALUES_SERVER("the server", 1),
    VALUES_NO_REASON("-", 1),
    VALUES_NO_LOCATION("not set", 1),

    // -------------------------------------------------------------------- time
    TIME_DAYS("%amount%d", 1),
    TIME_HOURS("%amount%h", 1),
    TIME_MINUTES("%amount%m", 1),
    TIME_SECONDS("%amount%s", 1),
    TIME_SEPARATOR(" ", 1),
    TIME_ZERO("0s", 1),

    // ------------------------------------------------------------ restrictions
    RESTRICTIONS_TELEPORT("{$e}You can't teleport while in jail.", 1),
    RESTRICTIONS_ITEM("{$e}You can't use that while in jail.", 1),
    RESTRICTIONS_DROP("{$e}You can't drop items while in jail.", 1),
    RESTRICTIONS_ATTACK("{$e}Prisoners can't fight.", 1),
    RESTRICTIONS_PROTECTED_PRISONER("{$e}This prisoner is under the protection of the jail.", 1),
    RESTRICTIONS_BLOCK_BREAK("{$e}You can't break blocks while in jail.", 1, 2, 3),
    RESTRICTIONS_BLOCK_PLACE("{$e}You can't place blocks while in jail.", 1, 2, 3),
    RESTRICTIONS_BLOCK_INTERACT("{$e}You can't use that while in jail.", 1, 2, 3),
    RESTRICTIONS_ENTITY_INTERACT("{$e}You can't do that while in jail.", 1, 2, 3),
    RESTRICTIONS_CHAT("{$e}Prisoners can't talk in this channel. " +
            "hover:{{$es}/k chat kingdom;{$p}Switch to the kingdom chat;/k chat kingdom}", 1),
    RESTRICTIONS_COMMAND("{$e}You can't use this command while in jail.", 1),
    ;

    private final LanguageEntry languageEntry;
    private final String defaultValue;

    JailsLang(String defaultValue, int... group) {
        this.defaultValue = defaultValue;
        this.languageEntry = DefinedMessenger.getEntry("jails", this, group);
    }

    @Override
    public LanguageEntry getLanguageEntry() {
        return languageEntry;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }
}
