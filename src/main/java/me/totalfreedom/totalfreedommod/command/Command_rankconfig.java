package me.totalfreedom.totalfreedommod.command;

import java.util.Arrays;
import java.util.List;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.dispatch.RemoteDispatchContext;
import me.totalfreedom.totalfreedommod.rank.CustomRank;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.rank.RankManager;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SENIOR_ADMIN, source = SourceType.ONLY_CONSOLE, permission = "tfm.manage.ranks")
@CommandParameters(description = "Configure custom ranks with an interactive menu.",
        usage = "/<command> [list | create | edit <rank> | delete <rank> | set <rank> <property> [value] | setrank <player> <rank> | reload | save]",
        aliases = "rankconf,rankcfg")
public class Command_rankconfig extends FreedomCommand
{
    // Available colors for rank configuration
    private static final List<String> AVAILABLE_COLORS = Arrays.asList(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white"
    );

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (RemoteDispatchContext.isActive() || !sender.getName().equalsIgnoreCase("CONSOLE"))
        {
            msg("This command can only be used from the server panel console.", NamedTextColor.RED);
            return true;
        }

        final RankManager rm = plugin.rm;
        
        // Check internal permission (not Bukkit)
        if (!rm.canManageRanks(sender))
        {
            msg("You do not have permission to manage ranks.", NamedTextColor.RED);
            return true;
        }

        // Default: show main menu
        if (args.length == 0 || args[0].equalsIgnoreCase("list"))
        {
            sender.sendMessage(rm.buildMainMenu());
            return true;
        }

        switch (args[0].toLowerCase())
        {
            case "create":
                return handleCreate(sender, playerSender, args);

            case "edit":
                return handleEdit(sender, playerSender, args);

            case "delete":
                return handleDelete(sender, playerSender, args);

            case "set":
                return handleSet(sender, playerSender, args);

            case "setrank":
                return handleSetRank(sender, args);

            case "reload":
                return handleReload(sender);

            case "save":
                return handleSave(sender);

            default:
                return false;
        }
    }

    private boolean handleCreate(CommandSender sender, Player playerSender, String[] args)
    {
        final RankManager rm = plugin.rm;
        
        if (!(sender instanceof Player))
        {
            msg("This command must be run as a player for interactive input.", NamedTextColor.RED);
            return true;
        }

        // Start interactive creation wizard
        sender.sendMessage(Component.text("\n"));
        sender.sendMessage(Component.text("═══ CREATE NEW RANK ═══").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        
        Component prompt = Component.text("Enter the rank ID (lowercase, no spaces):").color(NamedTextColor.YELLOW);
        
        rm.getChatInputHandler().awaitInput(playerSender, prompt, input -> {
            String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "");
            
            if (id.isEmpty())
            {
                playerSender.sendMessage(Component.text("Invalid ID. Only lowercase letters, numbers, and underscores are allowed.").color(NamedTextColor.RED));
                return;
            }
            
            if (rm.hasCustomRank(id))
            {
                playerSender.sendMessage(Component.text("A rank with that ID already exists!").color(NamedTextColor.RED));
                return;
            }
            
            // Create the rank and continue with name prompt
            final CustomRank newRank = new CustomRank(id);
            
            Component namePrompt = Component.text("Enter the display name for this rank:").color(NamedTextColor.YELLOW)
                    .append(Component.text("\n(Tip: Use & color codes like &a, &b, &c)").color(NamedTextColor.GRAY));
            
            rm.getChatInputHandler().awaitInput(playerSender, namePrompt, name -> {
                newRank.setName(name);
                
                Component abbrevPrompt = Component.text("Enter the tag/abbreviation (e.g., SA, OP):").color(NamedTextColor.YELLOW)
                        .append(Component.text("\n(Tip: Use & color codes like &6S&eA)").color(NamedTextColor.GRAY));
                
                rm.getChatInputHandler().awaitInput(playerSender, abbrevPrompt, abbrev -> {
                    newRank.setAbbreviation(abbrev);
                    
                    Component levelPrompt = Component.text("Enter the level (number, higher = more authority):").color(NamedTextColor.YELLOW);
                    
                    rm.getChatInputHandler().awaitInput(playerSender, levelPrompt, levelStr -> {
                        try
                        {
                            int level = Integer.parseInt(levelStr);
                            newRank.setLevel(level);
                        }
                        catch (NumberFormatException e)
                        {
                            playerSender.sendMessage(Component.text("Invalid number. Using default level 0.").color(NamedTextColor.YELLOW));
                        }
                        
                        Component colorPrompt = Component.text("Enter the color (e.g., gold, red, dark_aqua):").color(NamedTextColor.YELLOW)
                                .append(Component.text("\nAvailable: " + String.join(", ", AVAILABLE_COLORS)).color(NamedTextColor.GRAY));
                        
                        rm.getChatInputHandler().awaitInput(playerSender, colorPrompt, colorStr -> {
                            NamedTextColor color = parseColor(colorStr);
                            newRank.setColor(color);
                            
                            Component adminPrompt = Component.text("Is this an admin rank? (yes/no):").color(NamedTextColor.YELLOW);
                            
                            rm.getChatInputHandler().awaitInput(playerSender, adminPrompt, adminStr -> {
                                boolean isAdmin = adminStr.equalsIgnoreCase("yes") || adminStr.equalsIgnoreCase("y") || adminStr.equalsIgnoreCase("true");
                                newRank.setAdmin(isAdmin);
                                
                                // Save the rank
                                rm.setCustomRank(newRank);
                                
                                FUtil.adminAction(sender.getName(), "Created new rank: " + newRank.getName(), false);
                                
                                playerSender.sendMessage(Component.text("\n"));
                                playerSender.sendMessage(Component.text("✓ Rank created successfully!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
                                playerSender.sendMessage(rm.buildEditMenu(newRank));
                                
                            }, 60);
                        }, 60);
                    }, 60);
                }, 60);
            }, 60);
        }, 60);
        
        return true;
    }

    private boolean handleEdit(CommandSender sender, Player playerSender, String[] args)
    {
        final RankManager rm = plugin.rm;
        
        if (args.length < 2)
        {
            msg("Usage: /rankconfig edit <rank>", NamedTextColor.RED);
            return true;
        }

        String id = args[1].toLowerCase();
        CustomRank rank = rm.getCustomRank(id);
        
        if (rank == null)
        {
            msg("Rank not found: " + id, NamedTextColor.RED);
            return true;
        }

        sender.sendMessage(rm.buildEditMenu(rank));
        return true;
    }

    private boolean handleDelete(CommandSender sender, Player playerSender, String[] args)
    {
        final RankManager rm = plugin.rm;
        
        if (args.length < 2)
        {
            msg("Usage: /rankconfig delete <rank>", NamedTextColor.RED);
            return true;
        }

        String id = args[1].toLowerCase();
        
        if (!rm.hasCustomRank(id))
        {
            msg("Rank not found: " + id, NamedTextColor.RED);
            return true;
        }

        CustomRank rank = rm.getCustomRank(id);
        
        // Confirm deletion for players
        if (sender instanceof Player)
        {
            Component prompt = Component.text("Are you sure you want to delete rank '")
                    .color(NamedTextColor.RED)
                    .append(rank.getColoredTag())
                    .append(Component.text(" ").append(rank.getColoredName()))
                    .append(Component.text("'? Type 'confirm' to proceed:").color(NamedTextColor.RED));
            
            rm.getChatInputHandler().awaitInput(playerSender, prompt, input -> {
                if (input.equalsIgnoreCase("confirm"))
                {
                    rm.removeCustomRank(id);
                    FUtil.adminAction(sender.getName(), "Deleted rank: " + rank.getName(), false);
                    playerSender.sendMessage(Component.text("Rank deleted successfully.").color(NamedTextColor.GREEN));
                    playerSender.sendMessage(rm.buildMainMenu());
                }
                else
                {
                    playerSender.sendMessage(Component.text("Deletion cancelled.").color(NamedTextColor.YELLOW));
                }
            }, 30);
        }
        else
        {
            rm.removeCustomRank(id);
            FUtil.adminAction(sender.getName(), "Deleted rank: " + rank.getName(), false);
            msg("Rank deleted: " + rank.getName(), NamedTextColor.GREEN);
        }
        
        return true;
    }

    private boolean handleSet(CommandSender sender, Player playerSender, String[] args)
    {
        final RankManager rm = plugin.rm;
        
        if (args.length < 3)
        {
            msg("Usage: /rankconfig set <rank> <property> [value]", NamedTextColor.RED);
            return true;
        }

        String id = args[1].toLowerCase();
        CustomRank rank = rm.getCustomRank(id);
        
        if (rank == null)
        {
            msg("Rank not found: " + id, NamedTextColor.RED);
            return true;
        }

        String property = args[2].toLowerCase();
        
        // Handle properties that need interactive input
        if (sender instanceof Player && args.length == 3)
        {
            return handleInteractiveSet(playerSender, rank, property);
        }
        
        // Handle properties with value provided
        if (args.length >= 4)
        {
            String value = args[3];
            return handleDirectSet(sender, rank, property, value, args);
        }
        
        return false;
    }

    private boolean handleInteractiveSet(Player player, CustomRank rank, String property)
    {
        final RankManager rm = plugin.rm;
        Component prompt;
        
        switch (property)
        {
            case "name":
                prompt = Component.text("Enter the new name for this rank:").color(NamedTextColor.YELLOW)
                        .append(Component.text("\n(Tip: Use & color codes like &a, &b, &c)").color(NamedTextColor.GRAY));
                rm.getChatInputHandler().awaitInput(player, prompt, input -> {
                    rank.setName(input);
                    rm.setCustomRank(rank);
                    player.sendMessage(Component.text("Name updated to: ", NamedTextColor.GREEN)
                            .append(FUtil.colorizeWithLinks(input, NamedTextColor.GREEN)));
                    player.sendMessage(rm.buildEditMenu(rank));
                }, 60);
                return true;

            case "abbreviation":
            case "abbrev":
            case "tag":
                prompt = Component.text("Enter the new abbreviation/tag:").color(NamedTextColor.YELLOW)
                        .append(Component.text("\n(Tip: Use & color codes like &6S&eA)").color(NamedTextColor.GRAY));
                rm.getChatInputHandler().awaitInput(player, prompt, input -> {
                    rank.setAbbreviation(input);
                    rm.setCustomRank(rank);
                    player.sendMessage(Component.text("Abbreviation updated to: ", NamedTextColor.GREEN)
                            .append(FUtil.colorizeWithLinks(input, NamedTextColor.GREEN)));
                    player.sendMessage(rm.buildEditMenu(rank));
                }, 60);
                return true;

            case "level":
                prompt = Component.text("Enter the new level (number):").color(NamedTextColor.YELLOW);
                rm.getChatInputHandler().awaitInput(player, prompt, input -> {
                    try
                    {
                        int level = Integer.parseInt(input);
                        rank.setLevel(level);
                        rm.setCustomRank(rank);
                        player.sendMessage(Component.text("Level updated to: " + level).color(NamedTextColor.GREEN));
                    }
                    catch (NumberFormatException e)
                    {
                        player.sendMessage(Component.text("Invalid number!").color(NamedTextColor.RED));
                    }
                    player.sendMessage(rm.buildEditMenu(rank));
                }, 60);
                return true;

            case "color":
                prompt = Component.text("Enter the new color:").color(NamedTextColor.YELLOW)
                        .append(Component.text("\nAvailable: " + String.join(", ", AVAILABLE_COLORS)).color(NamedTextColor.GRAY));
                rm.getChatInputHandler().awaitInput(player, prompt, input -> {
                    NamedTextColor color = parseColor(input);
                    rank.setColor(color);
                    rm.setCustomRank(rank);
                    player.sendMessage(Component.text("Color updated to: ").color(NamedTextColor.GREEN)
                            .append(Component.text(color.toString()).color(color)));
                    player.sendMessage(rm.buildEditMenu(rank));
                }, 60);
                return true;

            case "determiner":
                prompt = Component.text("Enter the new determiner (a/an):").color(NamedTextColor.YELLOW);
                rm.getChatInputHandler().awaitInput(player, prompt, input -> {
                    rank.setDeterminer(input);
                    rm.setCustomRank(rank);
                    player.sendMessage(Component.text("Determiner updated to: " + input).color(NamedTextColor.GREEN));
                    player.sendMessage(rm.buildEditMenu(rank));
                }, 60);
                return true;

            case "admin":
                prompt = Component.text("Is this an admin rank? (yes/no):").color(NamedTextColor.YELLOW);
                rm.getChatInputHandler().awaitInput(player, prompt, input -> {
                    boolean isAdmin = input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y") || input.equalsIgnoreCase("true");
                    rank.setAdmin(isAdmin);
                    rm.setCustomRank(rank);
                    player.sendMessage(Component.text("Admin status updated to: " + isAdmin).color(NamedTextColor.GREEN));
                    player.sendMessage(rm.buildEditMenu(rank));
                }, 60);
                return true;

            case "console":
                prompt = Component.text("Is this a console-only rank? (yes/no):").color(NamedTextColor.YELLOW);
                rm.getChatInputHandler().awaitInput(player, prompt, input -> {
                    boolean isConsole = input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y") || input.equalsIgnoreCase("true");
                    rank.setConsoleOnly(isConsole);
                    rm.setCustomRank(rank);
                    player.sendMessage(Component.text("Console-only status updated to: " + isConsole).color(NamedTextColor.GREEN));
                    player.sendMessage(rm.buildEditMenu(rank));
                }, 60);
                return true;

            case "prefix":
                prompt = Component.text("Enter the new prefix (e.g., &8[&6SrA&8] ):").color(NamedTextColor.YELLOW)
                        .append(Component.text("\n(Use & color codes, include trailing space if desired)").color(NamedTextColor.GRAY));
                rm.getChatInputHandler().awaitInput(player, prompt, input -> {
                    rank.setPrefix(input);
                    rm.setCustomRank(rank);
                    player.sendMessage(Component.text("Prefix updated to: ", NamedTextColor.GREEN)
                            .append(FUtil.colorizeWithLinks(input, NamedTextColor.GREEN)));
                    player.sendMessage(rm.buildEditMenu(rank));
                }, 60);
                return true;

            case "inherit":
                prompt = Component.text("Enter the rank ID to inherit from (or 'none' to remove):").color(NamedTextColor.YELLOW);
                rm.getChatInputHandler().awaitInput(player, prompt, input -> {
                    if (input.equalsIgnoreCase("none") || input.isEmpty())
                    {
                        rank.setInheritFrom(null);
                    }
                    else
                    {
                        if (rm.hasCustomRank(input))
                        {
                            rank.setInheritFrom(input.toLowerCase());
                        }
                        else
                        {
                            player.sendMessage(Component.text("Rank '" + input + "' does not exist!").color(NamedTextColor.RED));
                            player.sendMessage(rm.buildEditMenu(rank));
                            return;
                        }
                    }
                    rm.setCustomRank(rank);
                    player.sendMessage(Component.text("Inheritance updated").color(NamedTextColor.GREEN));
                    player.sendMessage(rm.buildEditMenu(rank));
                }, 60);
                return true;

            case "addperm":
                prompt = Component.text("Enter the permission to add (e.g., tfm.admin.ban):").color(NamedTextColor.YELLOW);
                rm.getChatInputHandler().awaitInput(player, prompt, input -> {
                    rank.addPermission(input);
                    rm.setCustomRank(rank);
                    player.sendMessage(Component.text("Permission added: " + input).color(NamedTextColor.GREEN));
                    player.sendMessage(rm.buildEditMenu(rank));
                }, 60);
                return true;

            default:
                player.sendMessage(Component.text("Unknown property: " + property).color(NamedTextColor.RED));
                player.sendMessage(Component.text("Valid properties: name, abbreviation, prefix, level, color, determiner, admin, console, inherit, addperm").color(NamedTextColor.GRAY));
                return true;
        }
    }

    private boolean handleDirectSet(CommandSender sender, CustomRank rank, String property, String value, String[] args)
    {
        final RankManager rm = plugin.rm;
        
        switch (property)
        {
            case "name":
                rank.setName(value);
                break;

            case "abbreviation":
            case "abbrev":
            case "tag":
                rank.setAbbreviation(value);
                break;

            case "level":
                try
                {
                    rank.setLevel(Integer.parseInt(value));
                }
                catch (NumberFormatException e)
                {
                    msg("Invalid number: " + value, NamedTextColor.RED);
                    return true;
                }
                break;

            case "color":
                rank.setColor(parseColor(value));
                break;

            case "determiner":
                rank.setDeterminer(value);
                break;

            case "admin":
                rank.setAdmin(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
                break;

            case "console":
                rank.setConsoleOnly(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
                break;

            case "prefix":
                rank.setPrefix(value);
                break;

            case "inherit":
                if (value.equalsIgnoreCase("none") || value.isEmpty())
                {
                    rank.setInheritFrom(null);
                }
                else
                {
                    if (!rm.hasCustomRank(value))
                    {
                        msg("Rank '" + value + "' does not exist!", NamedTextColor.RED);
                        return true;
                    }
                    rank.setInheritFrom(value.toLowerCase());
                }
                break;

            case "addperm":
                rank.addPermission(value);
                break;

            case "remperm":
                rank.removePermission(value);
                break;

            default:
                msg("Unknown property: " + property, NamedTextColor.RED);
                return true;
        }
        
        rm.setCustomRank(rank);
        msg("Updated " + property + " for rank " + rank.getName(), NamedTextColor.GREEN);
        
        // Show edit menu if player
        if (sender instanceof Player)
        {
            sender.sendMessage(rm.buildEditMenu(rank));
        }
        
        return true;
    }

    private boolean handleReload(CommandSender sender)
    {
        plugin.rm.loadRanks();
        FUtil.adminAction(sender.getName(), "Reloaded rank configuration", false);
        msg("Ranks reloaded from file.", NamedTextColor.GREEN);
        sender.sendMessage(plugin.rm.buildMainMenu());
        return true;
    }

    private boolean handleSave(CommandSender sender)
    {
        plugin.rm.saveRanks();
        msg("Ranks saved to file.", NamedTextColor.GREEN);
        return true;
    }

    private boolean handleSetRank(CommandSender sender, String[] args)
    {
        if (args.length < 3)
        {
            msg("Usage: /rankconfig setrank <player> <rank>", NamedTextColor.RED);
            return true;
        }

        Player target = getPlayer(args[1]);
        if (target == null)
        {
            msg("Player not found: " + args[1], NamedTextColor.RED);
            return true;
        }

        String rankId = args[2].toLowerCase();
        if (rankId.equalsIgnoreCase("none") || rankId.equalsIgnoreCase("clear"))
        {
            Admin admin = plugin.al.getAdmin(target);
            if (admin != null)
            {
                admin.setCustomRankId(null);
                plugin.al.save();
                msg("Cleared custom rank for " + target.getName(), NamedTextColor.GREEN);
            }
            else
            {
                msg("Player is not an admin, no custom rank to clear.", NamedTextColor.RED);
            }
            return true;
        }

        if (!plugin.rm.hasCustomRank(rankId))
        {
            msg("Rank not found: " + rankId, NamedTextColor.RED);
            return true;
        }

        Admin admin = plugin.al.getAdmin(target);
        if (admin == null)
        {
            msg("Player must be an admin to have a custom rank assigned.", NamedTextColor.RED);
            msg("Add them to the admin list first using /saconfig add " + target.getName(), NamedTextColor.GRAY);
            return true;
        }

        admin.setCustomRankId(rankId);
        plugin.al.save();
        
        CustomRank rank = plugin.rm.getCustomRank(rankId);
        msg(Component.text("Set " + target.getName() + "'s custom rank to: ").color(NamedTextColor.GREEN)
                .append(rank.getColoredTag())
                .append(Component.text(" ").append(rank.getColoredName())));
        
        FUtil.adminAction(sender.getName(), "Set " + target.getName() + "'s custom rank to " + rank.getName(), false);
        return true;
    }

    /**
     * Parse a color name to NamedTextColor.
     */
    private NamedTextColor parseColor(String name)
    {
        switch (name.toLowerCase().replace("_", ""))
        {
            case "black":
                return NamedTextColor.BLACK;
            case "darkblue":
                return NamedTextColor.DARK_BLUE;
            case "darkgreen":
                return NamedTextColor.DARK_GREEN;
            case "darkaqua":
            case "darkcyan":
                return NamedTextColor.DARK_AQUA;
            case "darkred":
                return NamedTextColor.DARK_RED;
            case "darkpurple":
            case "purple":
                return NamedTextColor.DARK_PURPLE;
            case "gold":
            case "orange":
                return NamedTextColor.GOLD;
            case "gray":
            case "grey":
                return NamedTextColor.GRAY;
            case "darkgray":
            case "darkgrey":
                return NamedTextColor.DARK_GRAY;
            case "blue":
                return NamedTextColor.BLUE;
            case "green":
                return NamedTextColor.GREEN;
            case "aqua":
            case "cyan":
                return NamedTextColor.AQUA;
            case "red":
                return NamedTextColor.RED;
            case "lightpurple":
            case "pink":
            case "magenta":
                return NamedTextColor.LIGHT_PURPLE;
            case "yellow":
                return NamedTextColor.YELLOW;
            case "white":
            default:
                return NamedTextColor.WHITE;
        }
    }
}
