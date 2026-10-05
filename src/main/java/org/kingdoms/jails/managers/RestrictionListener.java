package org.kingdoms.jails.managers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.kingdoms.constants.player.KingdomPlayer;
import org.kingdoms.constants.player.KingdomsChatChannel;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.KingdomJails;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * What a prisoner can't do. Every rule can be switched off in {@code restrictions} of
 * {@code jails.yml}, and {@code jail.bypass-permission} lifts all of them.
 */
public final class RestrictionListener implements Listener {

    /** @return the session of a prisoner subject to the restrictions, {@code null} otherwise. */
    private static JailSession restricted(Player player) {
        if (player == null) return null;
        JailSession session = KingdomJails.getSession(player.getUniqueId());
        if (session == null || JailPermissions.bypassesRestrictions(player)) return null;
        return session;
    }

    // ------------------------------------------------------------------ teleport

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        handleTeleport(event);
    }

    /** Portals have their own handler list: listening to the parent event doesn't cover them. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        handleTeleport(event);
    }

    private static void handleTeleport(PlayerTeleportEvent event) {
        if (!JailsConfig.RESTRICTIONS_TELEPORT_ENABLED.getBoolean()) return;

        Player player = event.getPlayer();
        if (JailService.isTeleporting(player.getUniqueId())) return;
        if (restricted(player) == null) return;

        String cause = event.getCause() == null ? "UNKNOWN" : event.getCause().name();
        if (!JailsConfig.RESTRICTIONS_TELEPORT_BLOCKED_CAUSES.getUpperCaseList().contains(cause)) return;

        event.setCancelled(true);
        JailMessages.restricted(player, JailsLang.RESTRICTIONS_TELEPORT);
    }

    // --------------------------------------------------------------------- items

    /**
     * {@code restrictions.items.mode}: {@code ALL} blocks every item except the {@code allowed}
     * ones, {@code LIST} only blocks the {@code blocked} ones.
     */
    static boolean isItemBlocked(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String material = item.getType().name();

        if ("LIST".equals(JailsConfig.RESTRICTIONS_ITEMS_MODE.getEnumName())) {
            return matches(material, JailsConfig.RESTRICTIONS_ITEMS_BLOCKED.getUpperCaseList());
        }
        return !matches(material, JailsConfig.RESTRICTIONS_ITEMS_ALLOWED.getUpperCaseList());
    }

    /** Material patterns: an exact name, {@code *_BED}, {@code DIAMOND_*} or {@code *SHULKER*}. */
    static boolean matches(String material, List<String> patterns) {
        for (String pattern : patterns) {
            if (pattern.isEmpty()) continue;
            boolean starts = pattern.startsWith("*");
            boolean ends = pattern.endsWith("*") && pattern.length() > 1;
            String core = pattern.substring(starts ? 1 : 0, pattern.length() - (ends ? 1 : 0));

            if (starts && ends) {
                if (material.contains(core)) return true;
            } else if (starts) {
                if (material.endsWith(core)) return true;
            } else if (ends) {
                if (material.startsWith(core)) return true;
            } else if (material.equals(core)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (restricted(player) == null) return;

        Action action = event.getAction();
        Block block = event.getClickedBlock();

        if (action == Action.PHYSICAL) {
            if (JailsConfig.RESTRICTIONS_BLOCKS_PHYSICAL.getBoolean()) event.setCancelled(true);
            return;
        }

        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        if (rightClick && JailsConfig.RESTRICTIONS_ITEMS_ENABLED.getBoolean() && isItemBlocked(event.getItem())) {
            event.setUseItemInHand(Event.Result.DENY);
            if (action == Action.RIGHT_CLICK_AIR) event.setCancelled(true);
            JailMessages.restricted(player, JailsLang.RESTRICTIONS_ITEM);
        }

        if (action == Action.RIGHT_CLICK_BLOCK && block != null && JailsConfig.RESTRICTIONS_BLOCKS_INTERACT.getBoolean()
                && !matches(block.getType().name(), JailsConfig.RESTRICTIONS_BLOCKS_INTERACT_WHITELIST.getUpperCaseList())) {
            event.setUseInteractedBlock(Event.Result.DENY);
            if (event.useItemInHand() == Event.Result.DENY) event.setCancelled(true);
            JailMessages.restricted(player, JailsLang.RESTRICTIONS_BLOCK_INTERACT);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!JailsConfig.RESTRICTIONS_ITEMS_ENABLED.getBoolean() || !JailsConfig.RESTRICTIONS_ITEMS_CONSUME.getBoolean()) return;
        if (restricted(event.getPlayer()) == null || !isItemBlocked(event.getItem())) return;

        event.setCancelled(true);
        JailMessages.restricted(event.getPlayer(), JailsLang.RESTRICTIONS_ITEM);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player) || !JailsConfig.RESTRICTIONS_ITEMS_ENABLED.getBoolean()) return;
        Player player = (Player) event.getEntity();
        if (restricted(player) == null || !isItemBlocked(event.getBow())) return;

        event.setCancelled(true);
        JailMessages.restricted(player, JailsLang.RESTRICTIONS_ITEM);
    }

    /** Ender pearls, tridents, snowballs... thrown with an item the prisoner may not use. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!JailsConfig.RESTRICTIONS_ITEMS_ENABLED.getBoolean()) return;
        ProjectileSource shooter = event.getEntity().getShooter();
        if (!(shooter instanceof Player)) return;

        Player player = (Player) shooter;
        if (restricted(player) == null) return;

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (isItemBlocked(main) || isItemBlocked(off)) {
            event.setCancelled(true);
            JailMessages.restricted(player, JailsLang.RESTRICTIONS_ITEM);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!JailsConfig.RESTRICTIONS_ITEMS_DROP.getBoolean() || restricted(event.getPlayer()) == null) return;
        event.setCancelled(true);
        JailMessages.restricted(event.getPlayer(), JailsLang.RESTRICTIONS_DROP);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player) || !JailsConfig.RESTRICTIONS_ITEMS_PICKUP.getBoolean()) return;
        if (restricted((Player) event.getEntity()) != null) event.setCancelled(true);
    }

    // ------------------------------------------------------------------- blocks

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!JailsConfig.RESTRICTIONS_BLOCKS_BREAK.getBoolean() || restricted(event.getPlayer()) == null) return;
        event.setCancelled(true);
        JailMessages.restricted(event.getPlayer(), JailsLang.RESTRICTIONS_BLOCK_BREAK);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!JailsConfig.RESTRICTIONS_BLOCKS_PLACE.getBoolean() || restricted(event.getPlayer()) == null) return;
        event.setCancelled(true);
        JailMessages.restricted(event.getPlayer(), JailsLang.RESTRICTIONS_BLOCK_PLACE);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!JailsConfig.RESTRICTIONS_BLOCKS_PLACE.getBoolean() || restricted(event.getPlayer()) == null) return;
        event.setCancelled(true);
        JailMessages.restricted(event.getPlayer(), JailsLang.RESTRICTIONS_BLOCK_PLACE);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!JailsConfig.RESTRICTIONS_BLOCKS_BREAK.getBoolean() || restricted(event.getPlayer()) == null) return;
        event.setCancelled(true);
        JailMessages.restricted(event.getPlayer(), JailsLang.RESTRICTIONS_BLOCK_BREAK);
    }

    /** Paintings and item frames count as blocks. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Player player = playerOf(event.getRemover());
        if (!JailsConfig.RESTRICTIONS_BLOCKS_BREAK.getBoolean() || restricted(player) == null) return;
        event.setCancelled(true);
        JailMessages.restricted(player, JailsLang.RESTRICTIONS_BLOCK_BREAK);
    }

    // ----------------------------------------------------------------- entities

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        cancelEntityInteraction(event, event.getPlayer());
    }

    /** Armor stands: a separate handler list from the parent event. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        cancelEntityInteraction(event, event.getPlayer());
    }

    private static void cancelEntityInteraction(Cancellable event, Player player) {
        if (!JailsConfig.RESTRICTIONS_ENTITIES_INTERACT.getBoolean() || restricted(player) == null) return;
        event.setCancelled(true);
        JailMessages.restricted(player, JailsLang.RESTRICTIONS_ENTITY_INTERACT);
    }

    // ---------------------------------------------------------------------- pvp

    /**
     * Prisoners can't fight. Prisoners who are members of the jailing kingdom are protected; the
     * others can be hit, by anyone or only by the jailing kingdom.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = playerOf(event.getDamager());
        Entity victim = event.getEntity();

        if (attacker != null && restricted(attacker) != null) {
            boolean blocked = victim instanceof Player
                    ? !JailsConfig.RESTRICTIONS_PVP_PRISONERS_CAN_ATTACK.getBoolean()
                    : JailsConfig.RESTRICTIONS_ENTITIES_DAMAGE.getBoolean();
            if (blocked) {
                event.setCancelled(true);
                JailMessages.restricted(attacker, JailsLang.RESTRICTIONS_ATTACK);
                return;
            }
        }

        // Only hits from players: mobs, arrows from dispensers and the like keep their usual rules.
        if (attacker == null || !(victim instanceof Player)) return;
        JailSession session = KingdomJails.getSession(victim.getUniqueId());
        if (session == null) return;

        if (!mayBeHit(session, (Player) victim, attacker)) {
            event.setCancelled(true);
            JailMessages.restricted(attacker, JailsLang.RESTRICTIONS_PROTECTED_PRISONER);
        }
    }

    /**
     * {@code restrictions.pvp.force-allow}: hits on prisoners that this addon allows go through
     * even if KingdomsX (relations, PvP toggles) or another plugin cancelled them.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageForce(EntityDamageByEntityEvent event) {
        if (!event.isCancelled() || !JailsConfig.RESTRICTIONS_PVP_FORCE.getBoolean()) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player attacker = playerOf(event.getDamager());
        if (attacker == null || restricted(attacker) != null) return;

        Player victim = (Player) event.getEntity();
        JailSession session = KingdomJails.getSession(victim.getUniqueId());
        if (session != null && !isMember(session, victim) && mayBeHit(session, victim, attacker)) {
            event.setCancelled(false);
        }
    }

    private static boolean mayBeHit(JailSession session, Player victim, Player attacker) {
        if (isMember(session, victim)) return !JailsConfig.RESTRICTIONS_PVP_MEMBERS_PROTECTED.getBoolean();
        if (!JailsConfig.RESTRICTIONS_PVP_FOREIGN_ATTACKABLE.getBoolean()) return false;

        if ("JAILER_KINGDOM".equals(JailsConfig.RESTRICTIONS_PVP_FOREIGN_ATTACKERS.getEnumName())) {
            KingdomPlayer kingdomPlayer = KingdomPlayer.getKingdomPlayer(attacker);
            UUID kingdom = kingdomPlayer == null ? null : kingdomPlayer.getKingdomId();
            return session.getKingdom().equals(kingdom);
        }
        return true;
    }

    /** Whether the prisoner belongs to the kingdom that jailed them. */
    private static boolean isMember(JailSession session, Player prisoner) {
        KingdomPlayer kingdomPlayer = KingdomPlayer.getKingdomPlayer(prisoner);
        return kingdomPlayer != null && session.getKingdom().equals(kingdomPlayer.getKingdomId());
    }

    private static Player playerOf(Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) entity).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }

    // --------------------------------------------------------------------- chat

    /**
     * Prisoners can't talk in the KingdomsX channels of {@code restrictions.chat.blocked-channels}.
     * {@code GLOBAL} stands for the public chat.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!JailsConfig.RESTRICTIONS_CHAT_ENABLED.getBoolean()) return;
        Player player = event.getPlayer();
        if (restricted(player) == null) return;

        if (isBlockedChannel(player, event.getMessage())) {
            event.setCancelled(true);
            JailMessages.restricted(player, JailsLang.RESTRICTIONS_CHAT);
        }
    }

    /**
     * The channel is the player's KingdomsX channel. A message starting with that channel's
     * bypass prefix ({@code ranged-bypass-prefix}, {@code !} by default) escapes the range of the
     * ranged channel and reaches everyone: it counts as a global message.
     */
    private static boolean isBlockedChannel(Player player, String message) {
        List<String> blocked = JailsConfig.RESTRICTIONS_CHAT_BLOCKED_CHANNELS.getUpperCaseList();
        if (blocked.isEmpty()) return false;

        KingdomsChatChannel channel;
        try {
            KingdomPlayer kingdomPlayer = KingdomPlayer.getKingdomPlayer(player);
            channel = kingdomPlayer == null ? null : kingdomPlayer.getChatChannel();
            if (channel != null && !channel.isGlobal()) {
                String prefix = channel.getBypassPrefix();
                if (prefix != null && !prefix.isEmpty() && message.startsWith(prefix)) {
                    return blocked.contains("GLOBAL") || blocked.contains(idOf(channel));
                }
            }
        } catch (RuntimeException | LinkageError ex) {
            // A chat API that moved: treat the message as a global one.
            return blocked.contains("GLOBAL");
        }

        if (channel == null || channel.isGlobal()) return blocked.contains("GLOBAL");
        return blocked.contains(idOf(channel));
    }

    private static String idOf(KingdomsChatChannel channel) {
        return channel.getId() == null ? "" : channel.getId().toUpperCase(Locale.ENGLISH);
    }

    // ----------------------------------------------------------------- commands

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!JailsConfig.RESTRICTIONS_COMMANDS_ENABLED.getBoolean()) return;
        Player player = event.getPlayer();
        if (restricted(player) == null) return;

        String command = normalize(event.getMessage());
        if (command.isEmpty()) return;
        if (matchesCommand(command, JailsConfig.RESTRICTIONS_COMMANDS_ALWAYS_ALLOWED.getStringList())) return;

        boolean listed = matchesCommand(command, JailsConfig.RESTRICTIONS_COMMANDS_LIST.getStringList());
        boolean whitelist = "WHITELIST".equals(JailsConfig.RESTRICTIONS_COMMANDS_MODE.getEnumName());
        if (whitelist != listed) {
            event.setCancelled(true);
            JailMessages.restricted(player, JailsLang.RESTRICTIONS_COMMAND);
        }
    }

    /** {@code /Essentials:Home  bed} becomes {@code home bed}. */
    static String normalize(String command) {
        String text = command.trim();
        while (text.startsWith("/")) text = text.substring(1);
        text = text.toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ").trim();

        int space = text.indexOf(' ');
        String label = space < 0 ? text : text.substring(0, space);
        int namespace = label.indexOf(':');
        if (namespace >= 0) text = text.substring(namespace + 1);
        return text;
    }

    /** {@code k home} matches the entries {@code k home} and {@code k}, not {@code k ho}. */
    static boolean matchesCommand(String command, List<String> entries) {
        for (String entry : entries) {
            if (entry == null) continue;
            String normalized = normalize(entry);
            if (normalized.isEmpty()) continue;
            if (command.equals(normalized) || command.startsWith(normalized + ' ')) return true;
        }
        return false;
    }
}
