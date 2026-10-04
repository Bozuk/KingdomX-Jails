package org.kingdoms.jails.data;

import java.util.Locale;

/**
 * How a prisoner ended up in jail. Its display name is the {@code jails.types.<type>} message.
 */
public enum JailType {
    /** {@code /k jail member} - by a member holding the jail permission. */
    MANUAL,
    /** Automatically, after a failed invasion. The bail is set by the server. */
    INVASION,
    /** {@code /k admin jail member}. */
    ADMIN;

    public static JailType fromString(String name) {
        if (name == null) return MANUAL;
        try {
            return valueOf(name.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            return MANUAL;
        }
    }
}
