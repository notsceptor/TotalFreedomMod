package com.sk89q.worldedit.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import com.sk89q.worldedit.LocalSession;

// Stub class for compilation - WorldEdit is a soft dependency
// WorldEdit 7 API: Uses BukkitAdapter for conversions
public class WorldEditPlugin implements Plugin {
    // WorldEdit 7: getSession still exists but may use Actor internally
    public LocalSession getSession(org.bukkit.entity.Player player) { return null; }
    // WorldEdit 7: wrapPlayer may be deprecated in favor of BukkitAdapter.adapt()
    public BukkitPlayer wrapPlayer(org.bukkit.entity.Player player) { return null; }
    public void onEnable() {}
    public void onDisable() {}
    public boolean isEnabled() { return false; }
    public org.bukkit.plugin.PluginDescriptionFile getDescription() { return null; }
    public org.bukkit.configuration.file.FileConfiguration getConfig() { return null; }
    public org.bukkit.configuration.file.FileConfiguration getDefaultConfig() { return null; }
    public void saveConfig() {}
    public void reloadConfig() {}
    public org.bukkit.plugin.PluginLoader getPluginLoader() { return null; }
    public org.bukkit.Server getServer() { return null; }
    public java.util.logging.Logger getLogger() { return null; }
    public String getName() { return "WorldEdit"; }
    public io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager<org.bukkit.plugin.Plugin> getLifecycleManager() { return null; }
    public io.papermc.paper.plugin.configuration.PluginMeta getPluginMeta() { return null; }
    public java.io.File getDataFolder() { return null; }
    public java.io.InputStream getResource(String path) { return null; }
    public void saveDefaultConfig() {}
    public void saveResource(String path, boolean replace) {}
    public void onLoad() {}
    public boolean isNaggable() { return false; }
    public void setNaggable(boolean nag) {}
    public org.bukkit.generator.ChunkGenerator getDefaultWorldGenerator(String world, String id) { return null; }
    public org.bukkit.generator.BiomeProvider getDefaultBiomeProvider(String world, String id) { return null; }
    public org.bukkit.command.TabExecutor getTabCompleter() { return null; }
    public net.kyori.adventure.key.Key key() { return null; }
    public String namespace() { return "worldedit"; }
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) { return false; }
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) { return null; }
}

