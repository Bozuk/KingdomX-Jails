package org.kingdoms.jails.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.kingdoms.jails.config.JailsLang;

import java.util.Locale;

/**
 * Durations as they are written in {@code jails.yml} and in commands: {@code 30s}, {@code 10m},
 * {@code 1h30m}, {@code 2d 12h}, {@code 1w}. A bare number is a number of seconds.
 * <p>
 * Parsed here rather than by KingdomsX so that the configuration and the commands accept exactly
 * the same syntax, whatever the KingdomsX version.
 */
public final class Durations {
    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;

    private Durations() {}

    /**
     * @return the duration in milliseconds, or {@code -1} when the text is not a duration.
     */
    public static long parse(String text) {
        if (text == null) return -1;
        String raw = text.trim().toLowerCase(Locale.ENGLISH).replace(" ", "");
        if (raw.isEmpty()) return -1;

        long total = 0;
        long number = -1;
        boolean any = false;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                number = (number < 0 ? 0 : number) * 10 + (c - '0');
                if (number > Integer.MAX_VALUE) return -1;
                continue;
            }

            if (number < 0) return -1;
            long unit;
            switch (c) {
                case 's': unit = SECOND; break;
                case 'm': unit = MINUTE; break;
                case 'h': unit = HOUR; break;
                case 'd': unit = DAY; break;
                case 'w': unit = WEEK; break;
                default: return -1;
            }
            total += number * unit;
            number = -1;
            any = true;
        }

        if (number >= 0) {
            total += number * SECOND;
            any = true;
        }
        return any ? total : -1;
    }

    /** @return {@code true} if the text explicitly carries a time unit, e.g. {@code 10m}. */
    public static boolean hasUnit(String text) {
        if (text == null || parse(text) < 0) return false;
        for (char c : text.toLowerCase(Locale.ENGLISH).toCharArray()) {
            if (c == 's' || c == 'm' || c == 'h' || c == 'd' || c == 'w') return true;
        }
        return false;
    }

    /** Parses a configuration value, falling back to a default for anything invalid. */
    public static long parseOr(String text, long fallback) {
        long parsed = parse(text);
        return parsed < 0 ? fallback : parsed;
    }

    /**
     * Formats a duration with the units of the receiver's language, e.g. {@code 1d 2h 5m}.
     * Seconds are only shown below one hour, they would be noise otherwise.
     */
    public static String format(long millis, CommandSender receiver) {
        if (receiver == null) receiver = Bukkit.getConsoleSender();
        if (millis <= 0) return JailsLang.TIME_ZERO.parse(receiver);

        long days = millis / DAY;
        long hours = (millis % DAY) / HOUR;
        long minutes = (millis % HOUR) / MINUTE;
        long seconds = (millis % MINUTE) / SECOND;

        String separator = JailsLang.TIME_SEPARATOR.parse(receiver);
        StringBuilder builder = new StringBuilder();
        append(builder, separator, days, JailsLang.TIME_DAYS, receiver);
        append(builder, separator, hours, JailsLang.TIME_HOURS, receiver);
        append(builder, separator, minutes, JailsLang.TIME_MINUTES, receiver);
        if (days == 0 && hours == 0) append(builder, separator, seconds, JailsLang.TIME_SECONDS, receiver);

        if (builder.length() == 0) append(builder, separator, 1, JailsLang.TIME_SECONDS, receiver);
        return builder.toString();
    }

    private static void append(StringBuilder builder, String separator, long amount, JailsLang unit,
                               CommandSender receiver) {
        if (amount <= 0) return;
        if (builder.length() > 0) builder.append(separator);
        builder.append(unit.parse(receiver, "amount", amount));
    }
}
