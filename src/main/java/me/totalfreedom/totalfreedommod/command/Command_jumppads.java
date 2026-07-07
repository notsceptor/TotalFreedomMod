package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.fun.Jumppads;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.fun.jumppads")
@CommandParameters(description = "Manage jumppads", usage = "/<command> <<on | off> | info | mode <mode> | strength <strength>>", aliases = "launchpads,jp")
public class Command_jumppads extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "<value:Boolean>")
    public boolean setEnabled(CommandContext ctx, Boolean status)
    {
        FUtil.adminAction(ctx.getSender().getName(), (status ? "En" : "Dis") + "abling Jumppads", false);
        plugin.jp.setMode(status ? Jumppads.JumpPadMode.NORMAL : Jumppads.JumpPadMode.OFF);
        return true;
    }

    @CommandDispatchTarget(pattern = "strength <value:Float>")
    public boolean setStrength(CommandContext ctx, Float value)
    {
        value = Math.clamp(value, 1F, 10F);

        FUtil.adminAction(ctx.getSender().getName(), "Setting Jumppads strength to: " + value, false);
        plugin.jp.setStrength((value / 10) + 0.1F);
        return true;
    }

    @CommandDispatchTarget(pattern = "mode")
    public boolean getModes(CommandContext ctx)
    {
        msg(ctx.getSender(), Component.text("The current Jumppads mode is ", NamedTextColor.GRAY)
                .append(Component.text(plugin.jp.getMode().name(), NamedTextColor.WHITE))
                .append(Component.text(".")));
        msg(ctx.getSender(), Component.text("Possible modes: ", NamedTextColor.GRAY)
                .append(Component.join(JoinConfiguration.commas(true),
                        Arrays.stream(Jumppads.JumpPadMode.values())
                                .map(mode -> Component.text(mode.name(), NamedTextColor.WHITE))
                                .toList())));
        return true;
    }

    @CommandDispatchTarget(pattern = "mode <status:Enum:class=me.totalfreedom.totalfreedommod.fun.Jumppads$JumpPadMode>")
    public boolean setMode(CommandContext ctx, Jumppads.JumpPadMode mode)
    {
        if (mode == Jumppads.JumpPadMode.OFF)
        {
            return setEnabled(ctx, false);
        }

        FUtil.adminAction(ctx.getSender().getName(), (plugin.jp.getMode().isOn() ? "S" : "Enabling and s") + "etting Jumppads mode to " + mode.getLabel(), false);
        plugin.jp.setMode(mode);
        return true;
    }

    @CommandDispatchTarget(pattern = "info")
    public boolean showInfo(CommandContext ctx)
    {
        final Jumppads.JumpPadMode mode = plugin.jp.getMode();

        msg(ctx.getSender(), "Jumppads: " + (mode.isOn() ? "Enabled" : "Disabled"), NamedTextColor.BLUE);
        msg(ctx.getSender(), "Sideways: " + (mode == Jumppads.JumpPadMode.NORMAL_AND_SIDEWAYS ? "Enabled" : "Disabled"), NamedTextColor.BLUE);
        msg(ctx.getSender(), "Strength: " + (plugin.jp.getStrength() * 10 - 1), NamedTextColor.BLUE);
        return true;
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}