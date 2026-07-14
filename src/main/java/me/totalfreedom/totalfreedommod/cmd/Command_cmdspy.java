package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.cmd.internal.annotation.*;
import me.totalfreedom.totalfreedommod.player.CommandSpyMode;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@Command(name = "cmdspy", description = "Spy on commands", usage = "/<command> [admins | ops | all]", aliases = {"commandspy", "cspy"})
@Permission(permission = "tfm.admin.cmdspy", source = SourceType.ONLY_IN_GAME, level = Rank.SUPER_ADMIN)
public class Command_cmdspy extends FCommand
{
    // doing this to show ajax why i don't like using var keyword :)
    @Callback
    public void toggle(final Player player)
    {
        final var pd = plugin.pl.getPlayer(player);
        commandSpy(player, pd.cmdspyEnabled() ? CommandSpyMode.OFF : CommandSpyMode.ALL);
    }

    @Callback
    public void commandSpy(final Player player, final CommandSpyMode mode) // should auto resolve enums
    {
        final var fp = plugin.pl.getPlayer(player);
        final var pd = plugin.pl.getData(player); 
        
        fp.setCommandSpyMode(mode);
        pd.setCommandSpyMode(mode); // Why is this in two separate locations? This is unnecessary caching.
        plugin.pl.saveAsync();

        msg(
            player, 
            "CommandSpy enabled for <mode>.", 
            Placeholder.unparsed("mode", mode.getName())
        );
    }
}
