package org.kingdoms.jails.commands;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.kingdoms.commands.CommandContext;
import org.kingdoms.commands.CommandResult;
import org.kingdoms.commands.CommandTabContext;
import org.kingdoms.commands.KingdomsCommand;
import org.kingdoms.commands.KingdomsParentCommand;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.managers.JailPermissions;
import org.kingdoms.jails.managers.JailService;
import org.kingdoms.jails.util.Locations;

import java.util.List;
import java.util.Locale;

/** {@code /k jail location [remove]} - one jail per kingdom, set where the player stands. */
public final class CommandJailLocation extends KingdomsCommand {
    private static final String REMOVE = "remove";

    public CommandJailLocation(KingdomsParentCommand parent) {
        super("location", parent);
    }

    @Override
    public CommandResult execute(CommandContext context) {
        context.assertPlayer();
        if (context.assertHasKingdom()) return CommandResult.FAILED;

        Player player = context.senderAsPlayer();
        KingdomPlayer kingdomPlayer = context.getKingdomPlayer();
        Kingdom kingdom = kingdomPlayer.getKingdom();
        if (!JailPermissions.canManageJail(kingdomPlayer)) return context.fail(JailsLang.GENERAL_NO_PERMISSION);

        if (context.hasArgs(1)) {
            String action = context.arg(0).toLowerCase(Locale.ENGLISH);
            if (!action.equals(REMOVE) && !action.equals("delete") && !action.equals("unset")) {
                return context.fail(JailsLang.COMMAND_JAIL_LOCATION_USAGE);
            }
            if (!KingdomJails.hasJailLocation(kingdom)) return context.fail(JailsLang.COMMAND_JAIL_LOCATION_NOT_SET);

            JailService.removeJailLocation(kingdom);
            context.sendMessage(JailsLang.COMMAND_JAIL_LOCATION_REMOVED);
            return CommandResult.SUCCESS;
        }

        Location location = player.getLocation();
        if (JailPermissions.isDisabledWorld(location.getWorld())) return context.fail(JailsLang.COMMAND_JAIL_LOCATION_DISABLED_WORLD);

        if (JailsConfig.JAIL_LOCATION_REQUIRE_OWN_LAND.getBoolean()) {
            Land land = Land.getLand(location);
            if (land == null || !kingdom.getId().equals(land.getKingdomId())) {
                return context.fail(JailsLang.COMMAND_JAIL_LOCATION_NOT_OWN_LAND);
            }
        }

        KingdomJails.setJailLocation(kingdom, location);
        String described = Locations.describe(location);
        context.var("location", described);
        context.sendMessage(JailsLang.COMMAND_JAIL_LOCATION_SET);

        for (Player member : kingdom.getOnlineMembers()) {
            if (member.getUniqueId().equals(player.getUniqueId())) continue;
            JailsLang.COMMAND_JAIL_LOCATION_ANNOUNCEMENT.sendMessage(member,
                    "player", player.getName(), "location", described);
        }
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(CommandTabContext context) {
        return context.isAtArg(0) ? context.suggest(0, REMOVE) : context.emptyTab();
    }
}
