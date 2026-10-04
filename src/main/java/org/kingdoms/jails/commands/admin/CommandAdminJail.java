package org.kingdoms.jails.commands.admin;

import org.kingdoms.commands.KingdomsParentCommand;

/** {@code /k admin jail} */
public final class CommandAdminJail extends KingdomsParentCommand {
    @SuppressWarnings("this-escape")
    public CommandAdminJail(KingdomsParentCommand parent) {
        super("jail", parent);
        if (isDisabled()) return;

        new CommandAdminJailMember(this);
        new CommandAdminJailRelease(this);
        new CommandAdminJailResetCooldown(this);
    }
}
