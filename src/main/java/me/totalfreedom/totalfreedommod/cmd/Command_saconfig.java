package me.totalfreedom.totalfreedommod.cmd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.cmd.internal.FuzzyMatch;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Callback;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Command;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Completer;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Permission;
import me.totalfreedom.totalfreedommod.cmd.internal.annotation.Subcommand;
import me.totalfreedom.totalfreedommod.player.FPlayer;
import me.totalfreedom.totalfreedommod.rank.CustomRank;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "saconfig", description = "Manage admins.",
    usage = "/<command> <list | clean | reload | setrank <username> <rank> | <add | remove | info> <username>>")
@Permission(level = Rank.OP, source = SourceType.BOTH, permission = "tfm.manage.saconfig")
public class Command_saconfig extends FCommand 
{

    @Callback
    @Subcommand("setrank")
    public boolean setRank(CommandSender sender, String username, String rankInput) 
    {
        checkConsole(sender);
        checkRank(sender, Rank.SENIOR_ADMIN);

        final CustomRank custom = plugin.rm != null ? plugin.rm.getCustomRank(rankInput) : null;
        final Rank rank;
        final String displayName;

        if (custom != null) 
        {
            if (custom.isConsoleOnly()) 
            {
                msg(sender, "You cannot set players to a console rank");
                return true;
            }
            if (!custom.isAdmin()) 
            {
                msg(sender, "Rank '" + custom.getName() + "' is not an admin rank.", NamedTextColor.RED);
                return true;
            }
            rank = resolveLegacyTier(custom);
            if (rank == null) 
            {
                msg(sender, "Rank '" + custom.getName() + "' has no legacy tier; set or inherit from super_admin or senior_admin.", NamedTextColor.RED);
                return true;
            }
            displayName = custom.getName();
        } 
        else 
        {
            try 
            {
                rank = Rank.valueOf(rankInput.toUpperCase());
            } 
            catch (IllegalArgumentException ignored) 
            {
                msg(sender, "Unknown rank: " + rankInput, NamedTextColor.RED);
                return true;
            }
            if (rank.isConsole()) 
            {
                msg(sender, "You cannot set players to a console rank");
                return true;
            }
            displayName = rank.getName();
        }

        if (!rank.isAtLeast(Rank.SUPER_ADMIN)) 
        {
            msg(sender, "Rank must be superadmin or higher.", NamedTextColor.RED);
            return true;
        }

        Admin admin = plugin.al.getEntryByName(username);
        if (admin == null) 
        {
            msg(sender, "Unknown admin: " + username);
            return true;
        }

        FUtil.adminAction(sender.getName(), "Setting " + admin.getName() + "'s rank to " + displayName, true);

        admin.setRank(rank);
        admin.setCustomRankId(custom != null ? custom.getId() : null);
        plugin.al.updateTables();
        plugin.al.saveAdminAsync(admin);

        Player player = getPlayer(admin.getName());
        if (player != null && plugin.rm != null) 
        {
            plugin.rm.updatePlayerTeam(player);
        }

        msg(sender, "Set " + admin.getName() + "'s rank to " + displayName);
        return true;
    }

    @Callback
    @Subcommand("info")
    public boolean getInfo(CommandSender sender, String username) 
    {
        checkRank(sender, Rank.SUPER_ADMIN);

        Admin admin = plugin.al.getEntryByName(username);

        if (admin == null) 
        {
            final Player player = getPlayer(username);
            if (player != null) 
            {
                admin = plugin.al.getAdmin(player);
            }
        }

        if (admin == null) 
        {
            msg(sender, "Superadmin not found: " + username);
        } 
        else 
        {
            msg(sender, admin.toString());
        }

        return true;
    }

    @Callback
    @Subcommand("add")
    public boolean addUser(CommandSender sender, String username) 
    {
        checkConsole(sender);
        checkRank(sender, Rank.SENIOR_ADMIN);

        final Player player = getPlayer(username);
        if (player != null && plugin.al.isAdmin(player)) 
        {
            msg(sender, "That player is already admin.");
            return true;
        }

        String name = player != null ? player.getName() : username;
        Admin admin = null;
        for (Admin loopAdmin : plugin.al.getAllAdmins().values()) 
        {
            if (loopAdmin.getName().equalsIgnoreCase(name)) 
            {
                admin = loopAdmin;
                break;
            }
        }

        if (admin == null)
        {
            if (player == null) 
            {
                msg(sender, PLAYER_NOT_FOUND);
                return true;
            }

            player.setOp(true);
            FUtil.adminAction(sender.getName(), "Adding " + player.getName() + " to the admin list", true);
            plugin.al.addAdmin(new Admin(player));
        } 
        else 
        {
            FUtil.adminAction(sender.getName(), "Re-adding " + admin.getName() + " to the admin list", true);

            if (player != null) 
            {
                player.setOp(true);
                admin.setName(player.getName());
                admin.addIp(player.getAddress().getAddress().getHostAddress());
            }

            admin.setActive(true);
            admin.setLastLogin(new Date());

            plugin.al.updateTables();
            plugin.al.saveAdminAsync(admin);
        }

        if (player != null) 
        {
            if (plugin.rm != null) 
            {
                plugin.rm.updatePlayerTeam(player);
            }

            final FPlayer fPlayer = plugin.pl.getPlayer(player);
            if (fPlayer.getFreezeData().isFrozen()) 
            {
                fPlayer.getFreezeData().setFrozen(false);
                msg(player, "You have been unfrozen.");
            }
        }

        return true;
    }

    @Callback
    @Subcommand("remove")
    public boolean removeUser(CommandSender sender, String username) 
    {
        checkConsole(sender);
        checkRank(sender, Rank.SENIOR_ADMIN);

        Player player = getPlayer(username);
        Admin admin = player != null ? plugin.al.getAdmin(player) : plugin.al.getEntryByName(username);

        if (admin == null) 
        {
            msg(sender, "Superadmin not found: " + username);
            return true;
        }

        FUtil.adminAction(sender.getName(), "Removing " + admin.getName() + " from the admin list", true);
        admin.setActive(false);
        plugin.al.updateTables();
        plugin.al.saveAdminAsync(admin);

        if (player != null && plugin.rm != null) 
        {
            plugin.rm.updatePlayerTeam(player);
        }

        return true;
    }

    @Callback
    @Subcommand("reload")
    public boolean reload(CommandSender sender) 
    {
        checkRank(sender, Rank.SUPER_ADMIN);

        FUtil.adminAction(sender.getName(), "Reloading the admin list", true);
        plugin.al.load();
        plugin.csr.load();
        msg(sender, "Admin list reloaded!");
        return true;
    }

    @Callback
    @Subcommand("clean")
    public boolean clean(CommandSender sender) 
    {
        checkConsole(sender);
        checkRank(sender, Rank.SENIOR_ADMIN);

        FUtil.adminAction(sender.getName(), "Cleaning admin list", true);
        plugin.al.deactivateOldEntries(true);
        getAdminList(sender);
        return true;
    }

    @Callback
    @Subcommand("list")
    public boolean list(CommandSender sender) 
    {
        getAdminList(sender);
        return true;
    }

    @Completer(value = "setrank", position = 0)
    public List<String> completeSetrankAdmin(CommandSender sender, String partial) 
    {
        return adminNames(partial);
    }

    @Completer(value = "setrank", position = 1)
    public List<String> completeSetrankRank(CommandSender sender, String partial) 
    {
        List<String> candidates = new ArrayList<>();
        for (Rank rank : Rank.values()) 
        {
            if (rank.isAdmin() && !rank.isConsole()) 
            {
                candidates.add(rank.name().toLowerCase());
            }
        }
        for (CustomRank custom : plugin.rm.getCustomRanks().values()) 
        {
            if (custom.isAdmin() && !custom.isConsoleOnly()) 
            {
                candidates.add(custom.getId());
            }
        }
        return FuzzyMatch.filter(candidates, partial);
    }

    @Completer(value = "info", position = 0)
    public List<String> completeInfoAdmin(CommandSender sender, String partial) 
    {
        return adminNames(partial);
    }

    @Completer(value = "remove", position = 0)
    public List<String> completeRemoveAdmin(CommandSender sender, String partial) 
    {
        return adminNames(partial);
    }

    @Completer(value = "add", position = 0)
    public List<String> completeAddPlayer(CommandSender sender, String partial) 
    {
        List<String> names = server.getOnlinePlayers().stream()
            .map(Player::getName)
            .toList();
        return FuzzyMatch.filter(names, partial);
    }

    private List<String> adminNames(String partial) 
    {
        List<String> names = plugin.al.getActiveAdmins().stream()
            .map(Admin::getName)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        return FuzzyMatch.filter(names, partial);
    }

    private void getAdminList(CommandSender sender) 
    {
        if (plugin.al.getActiveAdmins().isEmpty()) 
        {
            msg(sender, "No active admins.", NamedTextColor.GRAY);
            return;
        }

        Map<Rank, List<Admin>> byRank = new EnumMap<>(Rank.class);
        for (Admin admin : plugin.al.getActiveAdmins()) 
        {
            byRank.computeIfAbsent(admin.getRank(), r -> new ArrayList<>()).add(admin);
        }

        List<Rank> order = new ArrayList<>();
        for (Rank r : Rank.values()) 
        {
            if (r.isAdmin() && !r.isConsole()) 
            {
                order.add(r);
            }
        }
        order.sort(Comparator.comparingInt(Rank::getLevel).reversed());

        for (Rank rank : order) 
        {
            List<Admin> bucket = byRank.get(rank);
            if (bucket == null || bucket.isEmpty()) 
            {
                continue;
            }

            bucket.sort(Comparator.comparing(a -> a.getName().toLowerCase()));

            Component line = Component.text(rank.getName() + "s: ").color(rank.getColor());
            for (int i = 0; i < bucket.size(); i++) 
            {
                if (i > 0) 
                {
                    line = line.append(Component.text(", ").color(NamedTextColor.WHITE));
                }
                Admin admin = bucket.get(i);
                CustomRank customRank = admin.getCustomRankId() == null
                        ? null
                        : plugin.rm.getCustomRank(admin.getCustomRankId());
                if (customRank != null) 
                {
                    line = line.append(customRank.getColoredTag())
                            .append(Component.text(" "))
                            .append(Component.text(admin.getName()).color(NamedTextColor.WHITE));
                } 
                else 
                {
                    line = line.append(Component.text(admin.getName()).color(NamedTextColor.WHITE));
                }
            }
            msg(sender, line);
        }
    }

    private Rank resolveLegacyTier(CustomRank custom) 
    {
        CustomRank current = custom;
        int safety = 32;
        while (current != null && safety-- > 0) {
            try 
            {
                return Rank.valueOf(current.getId().toUpperCase());
            } 
            catch (IllegalArgumentException ignored) {}

            String parentId = current.getInheritFrom();
            if (parentId == null) 
            {
                return null;
            }
            current = plugin.rm.getCustomRank(parentId);
        }
        return null;
    }
}
