package org.kingdoms.jails.commands;

import org.kingdoms.jails.util.Durations;

/**
 * {@code [bail] [duration] [reason...]}, in that order, each one optional: a number is the bail,
 * a duration carries its unit ({@code 30m}, {@code 2h}), and whatever follows is the reason.
 * {@code -} or {@code none} as the bail means "no bail", {@code permanent} as the duration means
 * "until released".
 */
final class SentenceArguments {
    Double bail;
    boolean noBail;
    long duration = -1;
    String reason;

    static SentenceArguments parse(String[] args, int from) {
        SentenceArguments parsed = new SentenceArguments();
        int index = from;

        if (index < args.length) {
            if (JailCommands.isNoneKeyword(args[index])) {
                parsed.noBail = true;
                index++;
            } else {
                Double bail = JailCommands.parseAmount(args[index]);
                if (bail != null && !Durations.hasUnit(args[index])) {
                    parsed.bail = bail;
                    index++;
                }
            }
        }

        if (index < args.length) {
            if (Durations.hasUnit(args[index])) {
                parsed.duration = Durations.parse(args[index]);
                index++;
            } else if (isPermanentKeyword(args[index])) {
                parsed.duration = 0;
                index++;
            }
        }

        if (index < args.length) {
            StringBuilder reason = new StringBuilder();
            for (int i = index; i < args.length; i++) {
                if (reason.length() > 0) reason.append(' ');
                reason.append(args[i]);
            }
            parsed.reason = reason.toString().trim();
        }
        return parsed;
    }

    static boolean isPermanentKeyword(String text) {
        String lower = text.toLowerCase(java.util.Locale.ENGLISH);
        return lower.equals("permanent") || lower.equals("perm") || lower.equals("forever");
    }
}
