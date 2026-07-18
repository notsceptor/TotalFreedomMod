package me.totalfreedom.totalfreedommod.cmd;

import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "saconfig", description = "Manage admins.",
    usage = "/<command> <list | clean | reload | setrank <username> <rank> | <add | remove | info> <username>>")
@Permission(level = Rank.SUPER_ADMIN, permission = "tfm.manage.saconfig")
public class Command_saconfig extends FCommand 
{

    @Callback
    @Subcommand("setrank")
    @Permission(permission = "tfm.manage.saconfig", source = SourceType.ONLY_CONSOLE, level = Rank.SENIOR_ADMIN)
    public void setRank(CommandSender sender, Player target, String rankInput) 
    {
        final CustomRank custom = plugin().rm != null ? plugin().rm.getCustomRank(rankInput) : null;
        final Rank rank;
        final String displayName;

        if (custom != null) 
        {
            if (custom.isConsoleOnly()) 
            {
                msg(sender, "<red>You cannot set players to a console rank");
                return;
            }
            if (!custom.isAdmin()) 
            {
                msg(sender, "<red>Rank \"<rank>\" is not an admin rank.",
                        Placeholder.unparsed("rank", custom.getName()));
                return;
            }
            rank = resolveLegacyTier(custom);
            if (rank == null) 
            {
                msg(sender, "<red>Rank \"<rank>\" has no legacy tier; set or inherit from super_admin or senior_admin.",
                        Placeholder.unparsed("rank", custom.getName()));
                return;
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
                msg(sender, "<red>Unknown rank: <rank>", Placeholder.unparsed("rank", rankInput));
                return;
            }
            if (rank.isConsole()) 
            {
                msg(sender, "<red>You cannot set players to a console rank");
                return;
            }
            displayName = rank.getName();
        }

        if (!rank.isAtLeast(Rank.SUPER_ADMIN)) 
        {
            msg(sender, "<red>Rank must be superadmin or higher.");
            return;
        }

        Admin admin = plugin().al.getEntryByName(target.getName());
        if (admin == null) 
        {
            msg(sender, "<gray>Unknown admin: <player>",
                    Placeholder.unparsed("player", target.getName()));
            return;
        }

        adminAction(sender, "<red>Setting <player>'s rank to <rank>",
                Placeholder.unparsed("player", admin.getName()),
                Placeholder.unparsed("rank", displayName));

        admin.setRank(rank);
        admin.setCustomRankId(custom != null ? custom.getId() : null);
        plugin().al.updateTables();
        plugin().al.saveAdminAsync(admin);
        plugin().rm.updatePlayerTeam(target);

        msg(sender, "<gray>Set <player>'s rank to <rank>.",
                Placeholder.unparsed("player", admin.getName()),
                Placeholder.unparsed("rank", displayName));
    }

    @Callback
    @Subcommand("info")
    public boolean getInfo(CommandSender sender, Player target) 
    {
        Admin admin = plugin().al.getAdmin(target);

        if (admin == null) 
        {
            msg(sender, "<gray>Admin not found: <player>",
                    Placeholder.unparsed("player", target.getName()));
        } 
        else 
        {
            msg(sender, admin.toString());
        }

        return true;
    }

    @Callback
    @Subcommand("add")
    @Permission(permission = "tfm.manage.saconfig", source = SourceType.ONLY_CONSOLE, level = Rank.SENIOR_ADMIN)
    public boolean addUser(CommandSender sender, Player target) 
    {
        if (plugin().al.isAdmin(target)) 
        {
            msg(sender, "<gray>That player is already admin.");
            return true;
        }

        String name = target.getName();
        Admin admin = null;
        for (Admin loopAdmin : plugin().al.getAllAdmins().values()) 
        {
            if (loopAdmin.getName().equalsIgnoreCase(name)) 
            {
                admin = loopAdmin;
                break;
            }
        }

        adminAction(sender, "<red><new_entry:A:Re-a>dding <player> to the admin list",
                Formatter.booleanChoice("new_entry", admin == null),
                Placeholder.unparsed("player", target.getName()));

        target.setOp(true);

        if (admin == null)
        {
            plugin().al.addAdmin(new Admin(target));
        } 
        else 
        {
            admin.setName(target.getName());
            admin.addIp(target.getAddress().getAddress().getHostAddress());

            admin.setActive(true);
            admin.setLastLogin(new Date());

            plugin().al.updateTables();
            plugin().al.saveAdminAsync(admin);
        }

        
        if (plugin().rm != null) 
        {
            plugin().rm.updatePlayerTeam(target);
        }

        final FPlayer fPlayer = plugin().pl.getPlayer(target);
        if (fPlayer.getFreezeData().isFrozen()) 
        {
            fPlayer.getFreezeData().setFrozen(false);
            msg(target, "<green>You have been unfrozen.");
        }

        return true;
    }

    @Callback
    @Subcommand("remove")
    @Permission(permission = "tfm.manage.saconfig", source = SourceType.ONLY_CONSOLE, level = Rank.SENIOR_ADMIN)
    public boolean removeUser(CommandSender sender, String username) 
    {
        Admin admin = plugin().al.getEntryByName(username);

        if (admin == null) 
        {
            msg(sender, "<gray>Admin not found: " + username);
            return true;
        }

        adminAction(sender, "<red>Removing <player> from the admin list",
                Placeholder.unparsed("player", admin.getName()));
        admin.setActive(false);
        plugin().al.updateTables();
        plugin().al.saveAdminAsync(admin);

        Player player = Bukkit.getPlayer(username);
        if (player != null && plugin().rm != null) 
        {
            plugin().rm.updatePlayerTeam(player);
        }

        return true;
    }

    @Callback
    @Subcommand("reload")
    public boolean reload(CommandSender sender) 
    {
        adminAction(sender, "<red>Reloading the admin list");
        plugin().al.load();
        plugin().csr.load();
        msg(sender, "<gray>Admin list reloaded!");
        return true;
    }

    @Callback
    @Subcommand("clean")
    @Permission(permission = "tfm.manage.saconfig", level = Rank.SENIOR_ADMIN, source = SourceType.ONLY_CONSOLE)
    public boolean clean(CommandSender sender) 
    {
        adminAction(sender, "<red>Cleaning admin list");
        plugin().al.deactivateOldEntries(true);
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
        Stream<String> rankNames = Stream.of(Rank.values())
                                         .filter(rank -> rank.isAdmin() && !rank.isConsole())
                                         .map(rank -> rank.name().toLowerCase(Locale.ROOT));

        Stream<String> customRankIds = plugin().rm.getCustomRanks()
                                                .values()
                                                .stream()
                                                .filter(custom -> custom.isAdmin() && !custom.isConsoleOnly())
                                                .map(r -> r.getId());

        List<String> candidates = Stream.concat(rankNames, customRankIds).toList();

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
        List<String> names = server().getOnlinePlayers().stream()
            .map(p -> p.getName())
            .toList();
        return FuzzyMatch.filter(names, partial);
    }

    private List<String> adminNames(String partial) 
    {
        List<String> names = plugin().al.getActiveAdmins().stream()
            .map(a -> a.getName())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        return FuzzyMatch.filter(names, partial);
    }

    private void getAdminList(CommandSender sender) 
    {
        Set<Admin> activeAdmins = plugin().al.getActiveAdmins();
        if (activeAdmins.isEmpty()) 
        {
            msg(sender, "<gray>No active admins.");
            return;
        }

        Map<Rank, List<Admin>> byRank = activeAdmins.stream().collect(
            Collectors.groupingBy(a -> a.getRank(), () -> new EnumMap<>(Rank.class), Collectors.toList())
        );

        Stream.of(Rank.values())
              .filter(r -> r.isAdmin() && !r.isConsole())
              .filter(byRank::containsKey)
              .sorted(Comparator.comparingInt(Rank::getLevel).reversed())
              .forEach(rank -> {
                List<Admin> bucket = byRank.get(rank);
                String joinedAdmins = bucket.stream()
                        .sorted(Comparator.comparing(a -> a.getName().toLowerCase()))
                        .map(admin -> {
                            CustomRank customRank = admin.getCustomRankId() == null
                                    ? null
                                    : plugin().rm.getCustomRank(admin.getCustomRankId());
                                    
                            if (customRank != null) {
                                String tagText = customRank.getName();
                                String colorTag = customRank.getColor().asHexString();
                                return String.format("<%s>%s <white>%s", colorTag, tagText, admin.getName());
                            }
                            return String.format("<white>%s", admin.getName());
                        })
                        .collect(Collectors.joining("<white>, "));

                String rankColorTag = "<" + rank.getColor().asHexString() + ">";
                String line = rankColorTag + rank.getName() + "s: " + joinedAdmins;

                msg(sender, line);
            });
    }

    private Rank resolveLegacyTier(CustomRank custom) 
    {
        return Stream.iterate(custom, Objects::nonNull, current -> plugin().rm.getCustomRank(current.getInheritFrom()))
            .limit(32)
            .map(current -> {
                try {
                    return Rank.valueOf(current.getId().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}
