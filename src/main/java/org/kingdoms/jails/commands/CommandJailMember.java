package org.kingdoms.jails.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.JailType;
import org.kingdoms.jails.managers.JailMessages;
import org.kingdoms.jails.managers.JailPermissions;
import org.kingdoms.jails.managers.JailService;

import java.util.List;

/**
 * {@code /k jail member <player> [bail] [duration] [reason]}
 * <p>
 * Jailing doesn't touch the prisoner's inventory in any way.
 */
public final class CommandJailMember extends KingdomsCommand {
    public CommandJailMember(KingdomsParentCommand parent) {
        super("member", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.assertPlayer();
        if (context.assertHasKingdom()) return CommandResult.FAILED;
        context.requireArgs(1); // Throws the usage error when missing.

        Player player = context.senderAsPlayer();
        KingdomPlayer jailer = context.getKingdomPlayer();
        Kingdom kingdom = jailer.getKingdom();

        if (!JailPermissions.canManageJail(jailer)) return context.fail(JailsLang.GENERAL_NO_PERMISSION);
        if (JailPermissions.isDisabledWorld(player.getWorld())) return context.fail(JailsLang.COMMAND_JAIL_MEMBER_DISABLED_WORLD);

        OfflinePlayer target = JailCommands.findPlayer(context, context.arg(0));
        if (target == null) return CommandResult.FAILED;

        SentenceArguments arguments = SentenceArguments.parse(context.args, 1);
        JailingChecks.Sentence sentence = JailingChecks.check(context, kingdom, jailer, target, arguments);
        if (sentence == null) return CommandResult.FAILED;

        JailSession session = JailService.jail(kingdom, target, player.getUniqueId(), JailType.MANUAL,
                sentence.duration, sentence.bail, sentence.reason);
        if (session == null) return context.fail(JailsLang.COMMAND_JAIL_MEMBER_CANCELLED);

        JailsLang.COMMAND_JAIL_MEMBER_JAILED.sendMessage(player, JailMessages.placeholders(session, player));
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(CommandTabContext context) {
        return context.isAtArg(0) ? context.getPlayers(0) : context.emptyTab();
    }
}
