package me.totalfreedom.totalfreedommod.util;

import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FSync
{

    public static void playerMsg(final Player player, final String message)
    {
        final TotalFreedomMod plugin = TotalFreedomMod.plugin();
        plugin.getServer().getScheduler().runTask(plugin, () -> FUtil.playerMsg(player, message));
    }

    public static void playerKick(final Player player, final Component reason)
    {
        final TotalFreedomMod plugin = TotalFreedomMod.plugin();
        plugin.getServer().getScheduler().runTask(plugin, () -> player.kick(reason));
    }

    public static void playerKick(final Player player, final String reason)
    {
        playerKick(player, Component.text(reason));
    }

    public static void adminChatMessage(final CommandSender sender, final String message)
    {
        final TotalFreedomMod plugin = TotalFreedomMod.plugin();
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.cm.adminChat(sender, message));
    }

    public static void autoEject(final Player player, final String kickMessage)
    {
        final TotalFreedomMod plugin = TotalFreedomMod.plugin();
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.ae.autoEject(player, kickMessage));
    }

    public static void bcastMsg(final String message, final NamedTextColor color)
    {
        final TotalFreedomMod plugin = TotalFreedomMod.plugin();
        plugin.getServer().getScheduler().runTask(plugin, () -> FUtil.bcastMsg(message, color));
    }

    @Deprecated
    public static void bcastMsg(final String message, final ChatColor color)
    {
        bcastMsg(message, color != null ? AdventureUtil.chatColorToNamedTextColor(color) : null);
    }
}
