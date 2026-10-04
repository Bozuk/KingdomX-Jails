package org.kingdoms.jails.commands;

import org.kingdoms.commands.KingdomsParentCommand;

/** {@code /k jail} */
public final class CommandJail extends KingdomsParentCommand {
    @SuppressWarnings("this-escape")
    public CommandJail() {
        super("jail");
        if (isDisabled()) return;

        new CommandJailLocation(this);
        new CommandJailMember(this);
        new CommandJailStatus(this);
        new CommandJailPayBail(this);
        new CommandJailRelease(this);
        new CommandJailList(this);
    }
}
