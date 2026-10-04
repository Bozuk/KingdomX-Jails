package org.kingdoms.jails.commands.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.jails.commands.AdminJailing;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.managers.JailMessages;

import java.util.List;

/**
 * {@code /k admin jail member <kingdom> <player> [bail] [duration] [reason]} - jails without any
 * of the member requirements: no permission, cooldown, target or range check.
 */
public final class CommandAdminJailMember extends KingdomsCommand {
    public CommandAdminJailMember(KingdomsParentCommand parent) {
        super("member", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        if (!context.requireArgs(2)) return CommandResult.FAILED;

        Kingdom kingdom = context.getKingdom(0);
        if (kingdom == null) return CommandResult.FAILED;

        CommandSender sender = context.getMessageReceiver();
        JailSession session = AdminJailing.jail(context, kingdom, context.arg(1),
                sender instanceof Player ? ((Player) sender).getUniqueId() : null);
        if (session == null) return CommandResult.FAILED;

        JailsLang.COMMAND_ADMIN_JAIL_MEMBER_JAILED.sendMessage(sender, JailMessages.placeholders(session, sender));
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(CommandTabContext context) {
        if (context.isAtArg(0)) return context.getKingdoms(0);
        if (context.isAtArg(1)) return context.getPlayers(1);
        return context.emptyTab();
    }
}
