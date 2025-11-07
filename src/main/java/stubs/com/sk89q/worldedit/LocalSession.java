package com.sk89q.worldedit;

import com.sk89q.worldedit.bukkit.BukkitPlayer;

// Stub class for compilation - WorldEdit is a soft dependency
// WorldEdit 7 API: Uses Actor/Player instead of BukkitPlayer directly
// NOTE: The actual usage code in WorldEditBridge may need updates for WorldEdit 7 compatibility
// WorldEdit 7 uses com.sk89q.worldedit.entity.Player (which extends Actor) instead of BukkitPlayer
public class LocalSession {
    // WorldEdit 6 API (legacy - may still work in WE7 for backward compatibility)
    // Current code uses this signature: session.undo(session.getBlockBag(bukkitPlayer), bukkitPlayer)
    public void undo(Object blockBag, BukkitPlayer player) {}
    // WorldEdit 7 API: undo may take just Player (Actor) - keeping both for compatibility
    public void undo(Object player) {}
    // WorldEdit 6/7: getBlockBag - keeping for compatibility
    public Object getBlockBag(BukkitPlayer player) { return null; }
    public Object getBlockBag(Object player) { return null; }
    public void setBlockChangeLimit(int limit) {}
}

