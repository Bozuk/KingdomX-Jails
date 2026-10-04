package org.kingdoms.jails.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.jails.config.JailsLang;

import java.util.Locale;

/** Small helpers shared by the jail commands. */
final class JailCommands {
    private JailCommands() {}

    /**
     * An online player by name, else a player who has played before. Sends
     * {@code not-found} and returns {@code null} otherwise.
     */
    @SuppressWarnings("deprecation")
    static OfflinePlayer findPlayer(CommandContext context, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;

        for (OfflinePlayer known : Bukkit.getOfflinePlayers()) {
            if (known.getName() != null && known.getName().equalsIgnoreCase(name)) return known;
        }

        context.var("player", name);
        context.sendError(JailsLang.COMMAND_JAIL_MEMBER_NOT_FOUND);
        return null;
    }

    /** The bail argument, {@code null} when the text is not a number. */
    static Double parseAmount(String text) {
        if (text == null) return null;
        String raw = text.trim().replace(",", ".");
        if (raw.startsWith("$")) raw = raw.substring(1);
        try {
            double value = Double.parseDouble(raw);
            return Double.isNaN(value) || Double.isInfinite(value) ? null : value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static boolean isNoneKeyword(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase(Locale.ENGLISH);
        return lower.equals("-") || lower.equals("none") || lower.equals("no");
    }
}
