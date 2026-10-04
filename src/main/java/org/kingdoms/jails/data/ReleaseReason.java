package org.kingdoms.jails.data;

/**
 * Why a prisoner left the jail. Its display name is the {@code jails.release.reasons.<reason>}
 * message.
 */
public enum ReleaseReason {
    /** The sentence is over. */
    TIME_SERVED,
    /** {@code /k jail paybail}. */
    BAIL_PAID,
    /** {@code /k jail release} by a member of the jailing kingdom. */
    RELEASED,
    /** {@code /k admin jail release}. */
    ADMIN,
    /** The prisoner was a member of the jailing kingdom and left it. */
    LEFT_KINGDOM,
    /** The prisoner reached the wilderness. */
    ESCAPED,
    /** The jailing kingdom no longer exists. */
    KINGDOM_DISBANDED,
    /** The jail location was removed while {@code jail.location.on-remove} is {@code RELEASE}. */
    JAIL_REMOVED,
    /** Another plugin, through the API. */
    PLUGIN
}
