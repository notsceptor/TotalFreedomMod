package me.totalfreedom.totalfreedommod.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import lombok.Getter;

public class CommandContext
{
    @Getter
    private final CommandSender sender;
    @Getter
    private final Player playerSender;
    @Getter
    private final Command command;
    @Getter
    private final String commandLabel;
    @Getter
    private final boolean senderConsole;

    protected CommandContext(CommandSender sender, Player playerSender, Command command, String commandLabel, boolean senderConsole)
    {
        this.sender = sender;
        this.playerSender = playerSender;
        this.command = command;
        this.commandLabel = commandLabel;
        this.senderConsole = senderConsole;
    }
}
