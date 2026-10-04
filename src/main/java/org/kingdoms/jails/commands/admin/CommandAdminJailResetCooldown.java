package org.kingdoms.jails.commands.admin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.KingdomJails;

import java.util.List;

/** {@code /k admin jail resetcooldown <player>} - in every kingdom. */
public final class CommandAdminJailResetCooldown extends KingdomsCommand {
    public CommandAdminJailResetCooldown(KingdomsParentCommand parent) {
        super("resetcooldown", parent);
    }

    @Override
    @SuppressWarnings("deprecation")
    public CommandResult execute(CommandContext context) {
        context.requireArgs(1); // Throws the usage error when missing.

        OfflinePlayer target = Bukkit.getOfflinePlayer(context.arg(0));
        KingdomJails.clearCooldown(target.getUniqueId());

        context.var("player", target.getName() == null ? context.arg(0) : target.getName());
        context.sendMessage(JailsLang.COMMAND_ADMIN_JAIL_RESETCOOLDOWN_DONE);
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(CommandTabContext context) {
        return context.isAtArg(0) ? context.getPlayers(0) : context.emptyTab();
    }
}
