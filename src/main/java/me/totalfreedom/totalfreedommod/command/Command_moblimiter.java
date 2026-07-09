package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameRules;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.BOTH, permission = "tfm.server.moblimiter")
@CommandParameters(description = "Control the MobLimiter.", usage = "/<command> <<on | off> | limit <limit> | <allow | block <type>>>")
public class Command_moblimiter extends FreedomCommand
{
    @CommandDispatchTarget
    public boolean showStatus(CommandContext ctx)
    {
        msg(ctx.getSender(), Component.text("MobLimiter ", NamedTextColor.GRAY)
                .append(ConfigEntry.MOB_LIMITER_ENABLED.getBoolean() ?
                        Component.text("enabled", NamedTextColor.GREEN)
                                .append(Component.text(" with a global limit of ", NamedTextColor.GRAY)
                                        .append(Component.text(ConfigEntry.MOB_LIMITER_MAX.getInteger(), NamedTextColor.WHITE))
                                        .append(Component.text(" entities"))) :
                        Component.text("disabled", NamedTextColor.RED))
                .append(Component.text(".", NamedTextColor.GRAY)));


        if (ConfigEntry.MOB_LIMITER_ENABLED.getBoolean())
        {
            msg(ctx.getSender(), "Currently blocked entity types:", NamedTextColor.GRAY);
            msg(ctx.getSender(), Component.join(JoinConfiguration.commas(true),
                            plugin.mb.getBlockedMobTypes().stream()
                                    .map(key -> Component.text(key.asString(), NamedTextColor.WHITE))
                                    .toList())
                    .colorIfAbsent(NamedTextColor.GRAY));
        }

        return true;
    }

    @CommandDispatchTarget(pattern = "allow <type:EntityType:mobs>")
    public boolean allowEntityType(CommandContext ctx, EntityType type)
    {
        if (!plugin.mb.isMobTypeBlocked(type))
        {
            msg(ctx.getSender(), type.key() + " is not a blocked mob type.");
            return true;
        }

        plugin.mb.allowMobType(type);
        return showStatus(ctx);
    }

    @CommandDispatchTarget(pattern = "block <type:EntityType:mobs>")
    public boolean blockEntityType(CommandContext ctx, EntityType type)
    {
        if (plugin.mb.isMobTypeBlocked(type))
        {
            msg(ctx.getSender(), type.key() + " is already a blocked mob type.");
            return true;
        }

        plugin.mb.blockMobType(type);
        return showStatus(ctx);
    }

    @CommandDispatchTarget(pattern = "limit <limit:Integer>")
    public boolean setMobLimiterLimit(CommandContext ctx, Integer limit)
    {
        limit = Math.clamp(limit, 1, 2000);
        ConfigEntry.MOB_LIMITER_MAX.setInteger(limit);
        return showStatus(ctx);
    }

    @CommandDispatchTarget(pattern = "<value:Boolean>")
    public boolean setMobLimiterState(CommandContext ctx, Boolean value)
    {
        ConfigEntry.MOB_LIMITER_ENABLED.setBoolean(value);
        plugin.gr.setGameRule(GameRules.SPAWN_MOBS, !ConfigEntry.MOB_LIMITER_ENABLED.getBoolean());
        return showStatus(ctx);
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
