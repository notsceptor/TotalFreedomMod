package me.totalfreedom.totalfreedommod.rank;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.pravian.aero.util.ChatUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class RankManager extends FreedomService
{

    public RankManager(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
    }

    @Override
    protected void onStop()
    {
    }

    public Displayable getDisplay(CommandSender sender)
    {
        if (!(sender instanceof Player))
        {
            return getRank(sender); // Consoles don't have display ranks
        }

        final Player player = (Player) sender;

        // Display impostors
        if (plugin.al.isAdminImpostor(player))
        {
            return Rank.IMPOSTOR;
        }

        // Developers always show up
        if (FUtil.DEVELOPERS.contains(player.getName()))
        {
            return Title.DEVELOPER;
        }

        final Rank rank = getRank(player);

        // Non-admins don't have titles, display actual rank
        if (!rank.isAdmin())
        {
            return rank;
        }

        // If the player's an owner, display that
        if (ConfigEntry.SERVER_OWNERS.getList().contains(player.getName()))
        {
            return Title.OWNER;
        }

        return rank;
    }

    public Rank getRank(CommandSender sender)
    {
        if (sender instanceof Player)
        {
            return getRank((Player) sender);
        }

        // CONSOLE?
        if (sender.getName().equals("CONSOLE"))
        {
            return ConfigEntry.ADMINLIST_CONSOLE_IS_SENIOR.getBoolean() ? Rank.SENIOR_CONSOLE : Rank.TELNET_CONSOLE;
        }

        // Console admin, get by name
        Admin admin = plugin.al.getEntryByName(sender.getName());

        // Unknown console: RCON?
        if (admin == null)
        {
            return Rank.SENIOR_CONSOLE;
        }

        Rank rank = admin.getRank();

        // Get console
        if (rank.hasConsoleVariant())
        {
            rank = rank.getConsoleVariant();
        }
        return rank;
    }

    public Rank getRank(Player player)
    {
        if (plugin.al.isAdminImpostor(player))
        {
            return Rank.IMPOSTOR;
        }

        final Admin entry = plugin.al.getAdmin(player);
        if (entry != null)
        {
            return entry.getRank();
        }

        return player.isOp() ? Rank.OP : Rank.NON_OP;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        //plugin.pl.getData(player);
        final FPlayer fPlayer = plugin.pl.getPlayer(player);

        // Unban admins
        boolean isAdmin = plugin.al.isAdmin(player);
        if (isAdmin)
        {
            // Verify strict IP match
            if (!plugin.al.isIdentityMatched(player))
            {
                Component warningMsg = Component.text("Warning: " + player.getName() + " is an admin, but is using an account not registered to one of their ip-list.")
                        .color(NamedTextColor.RED);
                FUtil.bcastMsg(warningMsg);
                fPlayer.setSuperadminIdVerified(false);
            }
            else
            {
                fPlayer.setSuperadminIdVerified(true);
                plugin.al.updateLastLogin(player);
            }
        }

        // Handle impostors
        if (plugin.al.isAdminImpostor(player))
        {
            Component impostorMsg = Component.text(player.getName() + " is ")
                    .color(NamedTextColor.AQUA)
                    .append(Rank.IMPOSTOR.getColoredLoginMessage());
            FUtil.bcastMsg(impostorMsg);
            
            Component warningMsg = Component.text("Warning: " + player.getName() + " has been flagged as an impostor and has been frozen!")
                    .color(NamedTextColor.RED);
            FUtil.bcastMsg(warningMsg);
            
            player.getInventory().clear();
            player.setOp(false);
            player.setGameMode(GameMode.SURVIVAL);
            plugin.pl.getPlayer(player).getFreezeData().setFrozen(true);
            
            Component playerMsg = Component.text("You are marked as an impostor, please verify yourself!")
                    .color(NamedTextColor.RED);
            player.sendMessage(playerMsg);
            return;
        }

        // Auto-op players who join without op
        if (ConfigEntry.AUTO_OP_ENABLED.getBoolean() && !player.isOp() && !isAdmin)
        {
            player.setOp(true);
            try
            {
                player.recalculatePermissions();
            }
            catch (Exception ex) {}
            
            // Some plugins (such as Essentials) may cache permissions during the join event, so...
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    if (player.isOnline() && !plugin.al.isAdmin(player))
                    {
                        try
                        {
                            player.recalculatePermissions();
                        }
                        catch (Exception ex) {}
                    }
                }
            }.runTask(plugin);
            
            final int timeout = ConfigEntry.AUTO_OP_TIMEOUT.getInteger();
            if (timeout > 0)
            {
                new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                        if (player.isOnline() && !plugin.al.isAdmin(player))
                        {
                            player.setOp(false);
                        }
                    }
                }.runTaskLater(plugin, 20L * timeout);
            }
        }

        // Set display
        if (isAdmin || FUtil.DEVELOPERS.contains(player.getName()))
        {
            final Displayable display = getDisplay(player);
            Component loginMsg = display.getColoredLoginMessage();

            if (isAdmin)
            {
                Admin admin = plugin.al.getAdmin(player);
                if (admin.hasLoginMessage())
                {
                    // ChatUtils.colorize returns String, convert to Component
                    String legacyMsg = ChatUtils.colorize(admin.getLoginMessage());
                    loginMsg = AdventureUtil.legacyToComponent(legacyMsg);
                }
            }

            Component broadcastMsg = Component.text(player.getName() + " is ")
                    .color(NamedTextColor.AQUA)
                    .append(loginMsg);
            FUtil.bcastMsg(broadcastMsg);
            
            // setTag expects String, convert Component to legacy with § codes for chat format
            String tagLegacy = AdventureUtil.componentToLegacySection(display.getColoredTag());
            plugin.pl.getPlayer(player).setTag(tagLegacy);

            // setPlayerListName is deprecated but still used - convert Component to legacy string
            Component displayNameComponent = Component.text(player.getName()).color(display.getColor());
            String displayName = AdventureUtil.componentToLegacy(displayNameComponent);
            try
            {
                player.setPlayerListName(StringUtils.substring(displayName, 0, 16));
            }
            catch (IllegalArgumentException ex)
            {
            }
        }
    }
}
