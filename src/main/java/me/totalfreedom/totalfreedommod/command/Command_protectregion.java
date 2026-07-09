package me.totalfreedom.totalfreedommod.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.ProtectArea.ProtectedRegion;
import me.totalfreedom.totalfreedommod.rank.Rank;

@CommandPermissions(level = Rank.SUPER_ADMIN, source = SourceType.ONLY_IN_GAME, permission = "tfm.admin.protectregion")
@CommandParameters(description = "Manages protected regions.", usage = "/<command> <create | info | update | delete> <name>", aliases = "protect,protectarea")
public class Command_protectregion extends FreedomCommand
{
    @CommandDispatchTarget(pattern = "create <name>")
    public boolean createRegion(final CommandContext ctx, final String name)
    {
        if (!plugin.web.isWorldEditEnabled())
        {
            msg(ctx.getSender(), "WorldEdit must be available in order to create new protected regions.");
            return true;
        }
        final var region = plugin.web.getSelection(playerSender);
        if (region == null)
        {
            msg(ctx.getSender(), "Please select a region to protect using WorldEdit.");
            return true;
        }
        final ProtectedRegion protectedRegion = plugin.pa.addProtectedArea(name, region.min(), region.max(), region.world());
        if (protectedRegion == null)
        {
            msg(ctx.getSender(), String.format("Could not create a protected region with the name '%s'. Are you sure it doesn't already exist?", name));
            return true;
        }
        msg(ctx.getSender(), protectedRegion.toString());
        return true;
    }

    @CommandDispatchTarget(pattern = "update <region:ProtectedRegion>")
    public boolean updateRegion(final CommandContext ctx, final ProtectedRegion region)
    {
        if (!plugin.web.isWorldEditEnabled())
        {
            msg(ctx.getSender(), "WorldEdit must be available in order to update protected regions.");
            return true;
        }
        final var weRegion = plugin.web.getSelection(playerSender);
        if (weRegion == null)
        {
            msg(ctx.getSender(), "Please select a region to protect using WorldEdit.");
            return true;
        }
        msg(ctx.getSender(), plugin.pa.updateProtectedRegion(region, weRegion.min(), weRegion.max(), weRegion.world()).toString());
        return true;
    }

    @CommandDispatchTarget(pattern = "info <region:ProtectedRegion>")
    public boolean infoRegion(final CommandContext ctx, final ProtectedRegion region)
    {
        msg(ctx.getSender(), region.toString());
        return true;
    }

    @CommandDispatchTarget(pattern = "delete <region:ProtectedRegion>")
    public boolean deleteRegion(final CommandContext ctx, final ProtectedRegion region)
    {
        plugin.pa.removeProtectedArea(region);
        msg(ctx.getSender(), String.format("'%s' has been deleted.", region.getName()));
        return true;
    }

    @Override
    protected boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        return false;
    }
}
