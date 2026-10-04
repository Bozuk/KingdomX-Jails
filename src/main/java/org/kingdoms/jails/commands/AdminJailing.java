package org.kingdoms.jails.commands;

import org.bukkit.OfflinePlayer;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.JailType;
import org.kingdoms.jails.managers.JailService;

import java.util.UUID;

/** The admin jailing, sharing the argument parsing and sentence checks of the member command. */
public final class AdminJailing {
    private AdminJailing() {}

    /** @return the new session, or {@code null} once an error was sent. */
    public static JailSession jail(CommandContext context, Kingdom kingdom, String targetName, UUID issuer) {
        OfflinePlayer target = JailCommands.findPlayer(context, targetName);
        if (target == null) return null;

        SentenceArguments arguments = SentenceArguments.parse(context.args, 2);
        JailingChecks.Sentence sentence = JailingChecks.check(context, kingdom, null, target, arguments);
        if (sentence == null) return null;

        JailSession session = JailService.jail(kingdom, target, issuer, JailType.ADMIN,
                sentence.duration, sentence.bail, sentence.reason);
        if (session == null) context.sendError(JailsLang.COMMAND_JAIL_MEMBER_CANCELLED);
        return session;
    }
}
