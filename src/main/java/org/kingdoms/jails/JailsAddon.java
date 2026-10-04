package org.kingdoms.jails;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.kingdoms.addons.Addon;
import org.kingdoms.commands.admin.CommandAdmin;
import org.kingdoms.constants.metadata.KingdomMetadataHandler;
import org.kingdoms.constants.metadata.KingdomMetadataRegistry;
import org.kingdoms.jails.commands.CommandJail;
import org.kingdoms.jails.commands.admin.CommandAdminJail;
import org.kingdoms.jails.config.JailsConfig;
import org.kingdoms.jails.config.JailsLang;
import org.kingdoms.jails.data.CooldownsMetaHandler;
import org.kingdoms.jails.data.JailLocationMetaHandler;
import org.kingdoms.jails.data.KingdomJails;
import org.kingdoms.jails.data.PendingReleaseMetaHandler;
import org.kingdoms.jails.data.PrisonersMetaHandler;
import org.kingdoms.jails.locale.JailsLanguages;
import org.kingdoms.jails.managers.EscapeListener;
import org.kingdoms.jails.managers.InvasionListener;
import org.kingdoms.jails.managers.JailTimer;
import org.kingdoms.jails.managers.MembershipListener;
import org.kingdoms.jails.managers.RestrictionListener;
import org.kingdoms.jails.util.Scheduling;
import org.kingdoms.locale.LanguageManager;
import org.kingdoms.main.Kingdoms;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * KingdomsX addon: every kingdom gets a jail. Members holding the JAIL permission lock players
 * in it, invaders who fail can be thrown in it automatically, and prisoners get out by serving
 * their sentence, paying their bail, leaving the kingdom or escaping to the wilderness.
 */
public final class JailsAddon extends JavaPlugin implements Addon {
    private static JailsAddon instance;
    private static boolean loaded;

    private final Set<KingdomMetadataHandler> metadataHandlers = new HashSet<>();

    public JailsAddon() {
        instance = this;
    }

    public static JailsAddon get() {
        return instance;
    }

    @Override
    public void onLoad() {
        if (!isKingdomsLoaded()) return;

        getLogger().info("Registering the metadata handlers...");
        metadataHandlers.addAll(Arrays.asList(
                PrisonersMetaHandler.INSTANCE,
                JailLocationMetaHandler.INSTANCE,
                CooldownsMetaHandler.INSTANCE,
                PendingReleaseMetaHandler.INSTANCE));
        for (KingdomMetadataHandler handler : metadataHandlers) {
            Kingdoms.get().getMetadataRegistry().register(handler);
        }

        LanguageManager.registerMessenger(JailsLang.class);
        JailsConfig.init();
    }

    @Override
    public void onEnable() {
        if (!isKingdomsEnabled()) {
            getLogger().severe("Kingdoms didn't load correctly. Disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Before the commands: their names, aliases and descriptions come from the messages.
        JailsLanguages.reload();

        PluginManager plugins = Bukkit.getPluginManager();
        plugins.registerEvents(new RestrictionListener(), this);
        plugins.registerEvents(new EscapeListener(), this);
        plugins.registerEvents(new MembershipListener(), this);
        plugins.registerEvents(new InvasionListener(), this);

        new CommandJail();
        new CommandAdminJail(CommandAdmin.getInstance());

        // KingdomsX may still be loading its data: index once it's done, the timer re-indexes anyway.
        Scheduling.runGlobalLater(Duration.ofSeconds(1), KingdomJails::reindex);
        JailTimer.start();

        registerAddon();
        loaded = true;
    }

    @Override
    public void onDisable() {
        JailTimer.stop();
        if (!loaded) return;
        signalDisable();
        disableAddon();
    }

    @Override
    public void reloadAddon() {
        JailsConfig.getConfig().reload();
        JailsLanguages.reload();
        new CommandJail();
        new CommandAdminJail(CommandAdmin.getInstance());
        KingdomJails.reindex();
        JailTimer.start();
    }

    @Override
    public void uninstall() {
        getLogger().info("Removing the jails metadata...");
        KingdomMetadataRegistry.removeMetadata(Kingdoms.get().getDataCenter().getKingdomManager(), metadataHandlers);
    }

    @Override
    public String getAddonName() {
        return "jails";
    }

    @Override
    public File getFile() {
        return super.getFile();
    }
}
