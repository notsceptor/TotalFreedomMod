package me.totalfreedom.totalfreedommod.command;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.admin.wildcard")
@CommandParameters(description = "Run any command on all users, username placeholder = ?.", usage = "/<command> [fluff] ? [fluff] ?")
public class Command_wildcard extends FreedomCommand
{
    public static List<String> BLOCKED_COMMANDS = Arrays.asList(
            "wildcard",
            "gtfo",
            "doom",
            "saconfig",
            "crash"
    );

    @CommandDispatchTarget(pattern = "<command..>")
    public boolean wildcard(CommandContext ctx, String command)
    {
        final String[] splitCommand = command.split(" ");

        if (Arrays.stream(splitCommand).anyMatch(this::isBlacklistedCommand)
                || plugin.cb.isCommandBlocked(command, ctx.getSender()))
        {
            msg("Did you really think that was going to work?", NamedTextColor.RED);
            return true;
        }

        for (Player player : server.getOnlinePlayers())
        {
            String processedCommand = command.replaceAll("\\x3f", player.getName());
            msg(ctx.getSender(), "Running command: " + processedCommand);

            if (!server.dispatchCommand(ctx.getSender(), processedCommand))
            {
                msg(ctx.getSender(), "Failed to execute command. Are you sure you entered it correctly?",
                        NamedTextColor.RED);
            }
        }

        return true;
    }

    private boolean isBlacklistedCommand(String command)
    {
        return BLOCKED_COMMANDS.stream()
                .map(blocked -> server.getCommandMap().getCommand(blocked))
                .filter(Objects::nonNull)
                .anyMatch(cmd ->
                        cmd.getName().equalsIgnoreCase(command) || cmd.getAliases().contains(command));
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
