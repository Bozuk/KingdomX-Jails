package org.kingdoms.jails.commands;

import org.bukkit.OfflinePlayer;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.data.ReleaseReason;
import org.kingdoms.jails.managers.JailMessages;
import org.kingdoms.jails.managers.JailPermissions;
import org.kingdoms.jails.managers.JailService;

import java.util.ArrayList;
import java.util.List;

/** {@code /k jail release <player>} - lets a prisoner of the kingdom out early. */
public final class CommandJailRelease extends KingdomsCommand {
    public CommandJailRelease(KingdomsParentCommand parent) {
        super("release", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.assertPlayer();
        if (context.assertHasKingdom()) return CommandResult.FAILED;
        if (!context.requireArgs(1)) return CommandResult.FAILED;

        if (!JailsConfig.RELEASE_BY_MEMBERS.getBoolean()) return context.fail(JailsLang.COMMAND_JAIL_RELEASE_DISABLED);

        KingdomPlayer kingdomPlayer = context.getKingdomPlayer();
        Kingdom kingdom = kingdomPlayer.getKingdom();
        if (!JailPermissions.canManageJail(kingdomPlayer)) return context.fail(JailsLang.GENERAL_NO_PERMISSION);

        OfflinePlayer target = JailCommands.findPlayer(context, context.arg(0));
        if (target == null) return CommandResult.FAILED;
        context.var("player", target.getName());

        JailSession session = KingdomJails.getSession(target.getUniqueId());
        if (session == null || !session.getKingdom().equals(kingdom.getId())) {
            return context.fail(JailsLang.COMMAND_JAIL_RELEASE_NOT_JAILED_HERE);
        }

        if (!JailService.release(kingdom, session, ReleaseReason.RELEASED)) {
            return context.fail(JailsLang.COMMAND_JAIL_RELEASE_CANCELLED);
        }
        context.sendMessage(JailsLang.COMMAND_JAIL_RELEASE_RELEASED);
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(CommandTabContext context) {
        if (!context.isAtArg(0)) return context.emptyTab();

        KingdomPlayer kingdomPlayer = context.getKingdomPlayer();
        Kingdom kingdom = kingdomPlayer == null ? null : kingdomPlayer.getKingdom();
        if (kingdom == null) return context.emptyTab();

        List<String> names = new ArrayList<>();
        for (JailSession session : KingdomJails.getPrisoners(kingdom)) {
            names.add(JailMessages.playerName(session.getPrisoner()));
        }
        return context.suggest(0, names);
    }
}
