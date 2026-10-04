package org.kingdoms.jails.commands.admin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.data.ReleaseReason;
import org.kingdoms.jails.managers.JailMessages;
import org.kingdoms.jails.managers.JailService;

import java.util.ArrayList;
import java.util.List;

/** {@code /k admin jail release <player>} - whatever the kingdom holding them. */
public final class CommandAdminJailRelease extends KingdomsCommand {
    public CommandAdminJailRelease(KingdomsParentCommand parent) {
        super("release", parent);
    }

    @Override
    @SuppressWarnings("deprecation")
    public CommandResult execute(CommandContext context) {
        if (!context.requireArgs(1)) return CommandResult.FAILED;

        OfflinePlayer target = Bukkit.getOfflinePlayer(context.arg(0));
        context.var("player", target.getName() == null ? context.arg(0) : target.getName());

        JailSession session = KingdomJails.getSession(target.getUniqueId());
        if (session == null) return context.fail(JailsLang.GENERAL_NOT_JAILED);

        Kingdom kingdom = Kingdom.getKingdom(session.getKingdom());
        context.var("kingdom", kingdom == null ? "?" : kingdom.getName());
        if (!JailService.release(kingdom, session, ReleaseReason.ADMIN)) {
            return context.fail(JailsLang.COMMAND_JAIL_RELEASE_CANCELLED);
        }

        context.sendMessage(JailsLang.COMMAND_ADMIN_JAIL_RELEASE_RELEASED);
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(CommandTabContext context) {
        if (!context.isAtArg(0)) return context.emptyTab();

        List<String> names = new ArrayList<>();
        for (JailSession session : KingdomJails.getAllSessions()) names.add(JailMessages.playerName(session.getPrisoner()));
        return context.suggest(0, names);
    }
}
