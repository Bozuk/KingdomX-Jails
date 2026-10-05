package org.kingdoms.jails.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.jails.data.JailSession;
import org.kingdoms.jails.data.ReleaseReason;

/**
 * Called right before a prisoner leaves a kingdom's jail.
 * <p>
 * Can be cancelled, except when the release can't be undone - the kingdom is being disbanded, or
 * the prisoner is leaving the kingdom: see {@link #isCancellable()}.
 */
public class PlayerUnjailEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Kingdom kingdom;
    private final JailSession session;
    private final ReleaseReason reason;
    private boolean cancelled;

    public PlayerUnjailEvent(Kingdom kingdom, JailSession session, ReleaseReason reason) {
        this.kingdom = kingdom;
        this.session = session;
        this.reason = reason;
    }

    /** The kingdom holding the prisoner, {@code null} if it no longer exists. */
    public Kingdom getKingdom() {
        return kingdom;
    }

    public JailSession getSession() {
        return session;
    }

    public ReleaseReason getReason() {
        return reason;
    }

    public boolean isCancellable() {
        return reason != ReleaseReason.KINGDOM_DISBANDED && reason != ReleaseReason.LEFT_KINGDOM;
    }

    @Override
    public boolean isCancelled() {
        return cancelled && isCancellable();
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
