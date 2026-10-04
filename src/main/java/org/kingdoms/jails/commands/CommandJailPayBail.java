package org.kingdoms.jails.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.JailType;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.data.ReleaseReason;
import org.kingdoms.jails.managers.JailMessages;
import org.kingdoms.jails.managers.JailService;
import org.kingdoms.services.ServiceVault;

import java.util.List;

/**
 * {@code /k jail paybail [player]} - money, through Vault, never resource points. The money goes
 * to the jailing kingdom's bank or vanishes, see {@code release.bail.receiver} (and
 * {@code invasions.auto-jail.bail-receiver} for a bail set by the server).
 */
public final class CommandJailPayBail extends KingdomsCommand {
    public CommandJailPayBail(KingdomsParentCommand parent) {
        super("paybail", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.assertPlayer();
        Player payer = context.senderAsPlayer();

        if (!JailsConfig.RELEASE_BAIL_ENABLED.getBoolean()) return context.fail(JailsLang.COMMAND_JAIL_PAYBAIL_DISABLED);

        OfflinePlayer prisoner = payer;
        if (context.hasArgs(1)) {
            prisoner = JailCommands.findPlayer(context, context.arg(0));
            if (prisoner == null) return CommandResult.FAILED;
        }

        boolean self = prisoner.getUniqueId().equals(payer.getUniqueId());
        context.var("player", prisoner.getName());
        if (!self && !JailsConfig.RELEASE_BAIL_ALLOW_PAYING_FOR_OTHERS.getBoolean()) {
            return context.fail(JailsLang.COMMAND_JAIL_PAYBAIL_OTHERS_DISABLED);
        }

        JailSession session = KingdomJails.getSession(prisoner.getUniqueId());
        if (session == null) {
            return context.fail(self ? JailsLang.COMMAND_JAIL_PAYBAIL_NOT_JAILED : JailsLang.COMMAND_JAIL_PAYBAIL_NOT_JAILED_OTHER);
        }
        if (!session.hasBail()) return context.fail(JailsLang.COMMAND_JAIL_PAYBAIL_NO_BAIL);

        if (!ServiceVault.isAvailable(ServiceVault.Component.ECO)) return context.fail(JailsLang.COMMAND_JAIL_PAYBAIL_NO_ECONOMY);

        double bail = session.getBail();
        context.var("bail", JailMessages.bail(bail, payer));
        if (!ServiceVault.hasMoney(payer, bail)) {
            context.var("balance", JailMessages.money(ServiceVault.getMoney(payer), payer));
            return context.fail(JailsLang.COMMAND_JAIL_PAYBAIL_NOT_ENOUGH_MONEY);
        }

        Kingdom kingdom = Kingdom.getKingdom(session.getKingdom());
        ServiceVault.withdraw(payer, bail);
        if (!JailService.release(kingdom, session, ReleaseReason.BAIL_PAID)) {
            ServiceVault.deposit(payer, bail);
            return context.fail(JailsLang.COMMAND_JAIL_PAYBAIL_CANCELLED);
        }

        String receiver = session.getType() == JailType.INVASION
                ? JailsConfig.INVASIONS_AUTO_JAIL_BAIL_RECEIVER.getEnumName()
                : JailsConfig.RELEASE_BAIL_RECEIVER.getEnumName();
        if ("KINGDOM_BANK".equals(receiver) && kingdom != null) kingdom.getBank().add(bail);

        if (!self) context.sendMessage(JailsLang.COMMAND_JAIL_PAYBAIL_PAID_OTHER);
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(CommandTabContext context) {
        return context.isAtArg(0) ? context.getPlayers(0) : context.emptyTab();
    }
}
