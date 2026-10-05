package org.kingdoms.jails.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.managers.JailMessages;

import java.util.List;

/**
 * {@code /k jail status [player]} - sentence, time served and left, bail and reason. Looking at
 * someone else's status is optional, see {@code status.others}.
 */
public final class CommandJailStatus extends KingdomsCommand {
    public CommandJailStatus(KingdomsParentCommand parent) {
        super("status", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        CommandSender sender = context.getMessageReceiver();
        OfflinePlayer target;

        if (context.hasArgs(1)) {
            target = JailCommands.findPlayer(context, context.arg(0));
            if (target == null) return CommandResult.FAILED;

            boolean self = sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId());
            if (!self && !canSeeOthers(sender)) return context.fail(JailsLang.COMMAND_JAIL_STATUS_OTHERS_DISABLED);

            JailSession session = KingdomJails.getSession(target.getUniqueId());
            if (session == null) {
                context.var("player", target.getName());
                return context.fail(self ? JailsLang.COMMAND_JAIL_STATUS_NOT_JAILED : JailsLang.COMMAND_JAIL_STATUS_NOT_JAILED_OTHER);
            }
            JailsLang.COMMAND_JAIL_STATUS_DISPLAY.sendMessage(sender, JailMessages.placeholders(session, sender));
            return CommandResult.SUCCESS;
        }

        context.assertPlayer();
        Player player = context.senderAsPlayer();
        JailSession session = KingdomJails.getSession(player.getUniqueId());
        if (session == null) return context.fail(JailsLang.COMMAND_JAIL_STATUS_NOT_JAILED);

        JailsLang.COMMAND_JAIL_STATUS_DISPLAY.sendMessage(player, JailMessages.placeholders(session, player));
        return CommandResult.SUCCESS;
    }

    private static boolean canSeeOthers(CommandSender sender) {
        if (!(sender instanceof Player)) return true;
        if (!JailsConfig.STATUS_OTHERS_ENABLED.getBoolean()) return false;

        String permission = JailsConfig.STATUS_OTHERS_PERMISSION.getString().trim();
        return permission.isEmpty() || sender.hasPermission(permission);
    }

    @Override
    public List<String> tabComplete(CommandTabContext context) {
        return context.isAtArg(0) ? context.getPlayers(0) : context.emptyTab();
    }
}
