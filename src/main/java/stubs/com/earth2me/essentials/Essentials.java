package com.earth2me.essentials;

import org.bukkit.plugin.Plugin;

// Stub class for compilation - Essentials is a soft dependency
public class Essentials implements Plugin {
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
    public String getName() { return "Essentials"; }
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
    public String namespace() { return "essentials"; }
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) { return false; }
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) { return null; }
    public User getUser(String name) { return null; }
    public com.earth2me.essentials.api.IUserMap getUserMap() { return null; }
}

