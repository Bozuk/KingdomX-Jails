package org.kingdoms.jails.commands;

import org.bukkit.command.CommandSender;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.managers.JailMessages;
import org.kingdoms.jails.managers.JailPermissions;

import java.util.Comparator;
import java.util.List;

/** {@code /k jail list} - the prisoners of the kingdom's jail, oldest first. */
public final class CommandJailList extends KingdomsCommand {
    public CommandJailList(KingdomsParentCommand parent) {
        super("list", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.assertPlayer();
        if (context.assertHasKingdom()) return CommandResult.FAILED;

        KingdomPlayer kingdomPlayer = context.getKingdomPlayer();
        Kingdom kingdom = kingdomPlayer.getKingdom();
        if (JailsConfig.LIST_REQUIRE_PERMISSION.getBoolean() && !JailPermissions.canManageJail(kingdomPlayer)) {
            return context.fail(JailsLang.GENERAL_NO_PERMISSION);
        }

        List<JailSession> prisoners = KingdomJails.getPrisoners(kingdom);
        if (prisoners.isEmpty()) {
            context.sendMessage(JailsLang.COMMAND_JAIL_LIST_EMPTY);
            return CommandResult.SUCCESS;
        }
        prisoners.sort(Comparator.comparingLong(JailSession::getSince));

        CommandSender sender = context.getMessageReceiver();
        JailsLang.COMMAND_JAIL_LIST_HEADER.sendMessage(sender,
                "count", prisoners.size(),
                "location", JailMessages.location(kingdom, sender));
        for (JailSession session : prisoners) {
            JailsLang.COMMAND_JAIL_LIST_ENTRY.sendMessage(sender, JailMessages.placeholders(session, sender));
        }
        return CommandResult.SUCCESS;
    }
}
