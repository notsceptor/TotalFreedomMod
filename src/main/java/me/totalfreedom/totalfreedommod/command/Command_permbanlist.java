package me.totalfreedom.totalfreedommod.command;

import java.util.ArrayList;
import java.util.List;
import me.totalfreedom.totalfreedommod.rank.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.admin.banlist")
@CommandParameters(description = "Shows all permanently banned players and IP addresses.", usage = "/<command> [page]", aliases = "pbanlist")
public class Command_permbanlist extends FreedomCommand
{

    private static final int ENTRIES_PER_PAGE = 10;

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length > 1)
        {
            return false;
        }

        int page = 1;

        if (args.length == 1)
        {
            try
            {
                page = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException ex)
            {
                msg(Component.text("The page must be a valid number.", NamedTextColor.RED));
                return true;
            }
        }

        getBanList(page);
        return true;
    }

    private void getBanList(int page)
    {
        // Permbans: usernames from PermbanList.
        List<String> permbanNames = new ArrayList<>(plugin.pm.getPermbannedNames());
        permbanNames.sort(String.CASE_INSENSITIVE_ORDER);

        if (permbanNames.isEmpty())
        {
            msg(Component.text("There are currently no permanently banned players.", NamedTextColor.RED));
            return;
        }

        int totalPages = (int) Math.ceil((double) permbanNames.size() / ENTRIES_PER_PAGE);

        if (page < 1 || page > totalPages)
        {
            msg(Component.text("Invalid page. Please choose a page from 1 to " + totalPages + ".", NamedTextColor.RED));
            return;
        }

        int startIndex = (page - 1) * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, permbanNames.size());

        List<String> pageEntries = permbanNames.subList(startIndex, endIndex);

        msg(buildSection("Permbans (" + page + "/" + totalPages + "): ", NamedTextColor.DARK_RED, pageEntries));

        if (page < totalPages)
        {
            msg(Component.text("Use /permbanlist " + (page + 1) + " to view the next page.", NamedTextColor.GRAY));
        }
    }

    private Component buildSection(String header, NamedTextColor headerColor, List<String> entries)
    {
        Component line = Component.text(header).color(headerColor);

        for (int i = 0; i < entries.size(); i++)
        {
            if (i > 0)
            {
                line = line.append(Component.text(", ").color(NamedTextColor.WHITE));
            }

            line = line.append(Component.text(entries.get(i)).color(NamedTextColor.WHITE));
        }

        return line;
    }
}