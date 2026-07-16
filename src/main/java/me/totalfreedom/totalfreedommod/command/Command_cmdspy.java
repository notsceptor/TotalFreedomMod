package me.totalfreedom.totalfreedommod.command;

import java.util.List;
import java.util.Locale;
import me.totalfreedom.totalfreedommod.player.CommandSpyMode;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.admin.cmdspy")
@CommandParameters(description = "Spy on commands", usage = "/<command> [admins | ops | all]", aliases = "commandspy")
public class Command_cmdspy extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean toggleCommandSpy(CommandContext ctx)
    {
        final FPlayer playerData = plugin.pl.getPlayer(ctx.getPlayerSender());
        final CommandSpyMode mode = playerData.cmdspyEnabled()
                ? CommandSpyMode.OFF
                : CommandSpyMode.ALL;

        return setCommandSpyMode(ctx, mode);
    }

    @CommandDispatchTarget(pattern = "<mode:Enum:class=me.totalfreedom.totalfreedommod.player.CommandSpyMode,mode=UPPERCASE>")
    public boolean setCommandSpyMode(CommandContext ctx, CommandSpyMode mode)
    {
        final Player player = ctx.getPlayerSender();
        final FPlayer playerData = plugin.pl.getPlayer(player);
        final PlayerData data = plugin.pl.getData(player);

        playerData.setCommandSpyMode(mode);
        data.setCommandSpyMode(mode);
        plugin.pl.saveAsync();

        final Component message = switch (mode)
        {
            case OFF -> Component.text("CommandSpy disabled.", NamedTextColor.RED);
            case ADMINS -> Component.text("CommandSpy set to ", NamedTextColor.GRAY)
                    .append(Component.text("ADMINS", NamedTextColor.GREEN))
                    .append(Component.text(" mode. You will only see admins' commands.", NamedTextColor.GRAY));
            case OPS -> Component.text("CommandSpy set to ", NamedTextColor.GRAY)
                    .append(Component.text("OPS", NamedTextColor.GREEN))
                    .append(Component.text(" mode. You will only see OPs' commands.", NamedTextColor.GRAY));
            case ALL -> Component.text("CommandSpy set to ", NamedTextColor.GRAY)
                    .append(Component.text("ALL", NamedTextColor.GREEN))
                    .append(Component.text(" mode. You will see both OPs' and admins' commands.", NamedTextColor.GRAY));
        };

        ctx.getSender().sendMessage(message);
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, String[] args)
    {
        if (args.length != 1)
        {
            return List.of();
        }

        final String partial = args[0].toLowerCase(Locale.ROOT);

        return List.of("admins", "ops", "all").stream()
                .filter(mode -> mode.startsWith(partial))
                .toList();
    }
}