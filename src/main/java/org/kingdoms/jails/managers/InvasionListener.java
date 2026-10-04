package org.kingdoms.jails.managers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.events.invasion.KingdomInvadeEndEvent;
import org.kingdoms.jails.JailsAddon;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.data.JailType;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.util.Durations;
import org.kingdoms.jails.util.Scheduling;
import org.kingdoms.managers.invasions.Invasion;
import org.kingdoms.utils.MathUtils;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * {@code invasions.auto-jail}: the invader of a failed invasion lands in the defender's jail, with
 * a bail set by the server rather than by the players.
 * <p>
 * The result is set before {@link KingdomInvadeEndEvent} fires, but KingdomsX still has work to do
 * afterwards - among which sending the invader back to where the invasion started. The jailing
 * therefore happens after {@code invasions.auto-jail.delay}, once KingdomsX is done.
 */
public final class InvasionListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInvasionEnd(KingdomInvadeEndEvent event) {
        if (!JailsConfig.INVASIONS_AUTO_JAIL_ENABLED.getBoolean()) return;

        Invasion invasion = event.getInvasion();
        Invasion.Result result = invasion.getResult();
        if (result == null || result.isSuccessful()) return;
        if (!JailsConfig.INVASIONS_AUTO_JAIL_RESULTS.getUpperCaseList().contains(result.name())) return;

        Kingdom defender = invasion.getDefender();
        if (defender == null) return;

        Set<UUID> targets = new LinkedHashSet<>();
        KingdomPlayer invader = invasion.getInvader();
        if (invader != null) targets.add(invader.getId());

        if (JailsConfig.INVASIONS_AUTO_JAIL_ATTACKERS_IN_LAND.getBoolean()) {
            Kingdom attacker = invasion.getAttacker();
            if (attacker != null) {
                for (Player member : attacker.getOnlineMembers()) {
                    Land land = Land.getLand(member.getLocation());
                    if (land != null && defender.getId().equals(land.getKingdomId())) targets.add(member.getUniqueId());
                }
            }
        }
        if (targets.isEmpty()) return;

        long delay = JailsConfig.INVASIONS_AUTO_JAIL_DELAY.getMillis();
        Scheduling.runGlobalLater(Duration.ofMillis(delay), () -> {
            for (UUID target : targets) autoJail(defender, target);
        });
    }

    private static void autoJail(Kingdom defender, UUID target) {
        if (Kingdom.getKingdom(defender.getId()) == null) return; // Disbanded in the meantime.
        if (KingdomJails.isJailed(target)) return;

        if (JailsConfig.INVASIONS_AUTO_JAIL_REQUIRE_LOCATION.getBoolean() && KingdomJails.getJailLocation(defender) == null) {
            return;
        }
        if (!JailsConfig.INVASIONS_AUTO_JAIL_IGNORE_COOLDOWN.getBoolean() && JailRules.cooldownLeft(defender, target) > 0) {
            return;
        }
        if (!JailsConfig.INVASIONS_AUTO_JAIL_IGNORE_MAX_PRISONERS.getBoolean() && JailRules.isFull(defender)) {
            return;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(target);
        if (JailPermissions.isExempt(player.getPlayer())) return;

        long duration = Durations.parseOr(JailsConfig.INVASIONS_AUTO_JAIL_DURATION.getString(), 0);
        String reason = JailsConfig.INVASIONS_AUTO_JAIL_REASON.getString();
        JailService.jail(defender, player, null, JailType.INVASION, Math.max(0, duration), bail(player),
                reason.isEmpty() ? null : reason);
    }

    /** {@code invasions.auto-jail.bail}, a math expression with the invader's placeholders. */
    private static double bail(OfflinePlayer player) {
        String expression = JailsConfig.INVASIONS_AUTO_JAIL_BAIL.getString().trim();
        if (expression.isEmpty()) return 0;

        try {
            return Math.max(0, Double.parseDouble(expression));
        } catch (NumberFormatException notANumber) {
            try {
                return Math.max(0, MathUtils.eval(expression, player));
            } catch (RuntimeException ex) {
                JailsAddon.get().getLogger().warning("Invalid invasions.auto-jail.bail '" + expression + "': " + ex);
                return 0;
            }
        }
    }
}
