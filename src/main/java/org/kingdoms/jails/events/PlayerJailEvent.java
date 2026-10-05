package org.kingdoms.jails.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.jails.data.JailSession;

/**
 * Called right before a player is put in a kingdom's jail. Cancelling it leaves the player free.
 */
public class PlayerJailEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Kingdom kingdom;
    private final JailSession session;
    private boolean cancelled;

    public PlayerJailEvent(Kingdom kingdom, JailSession session) {
        this.kingdom = kingdom;
        this.session = session;
    }

    public Kingdom getKingdom() {
        return kingdom;
    }

    public JailSession getSession() {
        return session;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
