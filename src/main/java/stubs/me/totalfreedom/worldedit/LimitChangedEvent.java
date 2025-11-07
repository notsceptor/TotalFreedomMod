package me.totalfreedom.worldedit;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Stub class for compilation
// This event comes from TF-WorldEdit extension plugin (https://github.com/tfreedomorg/TF-WorldEdit)
// TF-WorldEdit is a WorldEdit extension that adds SelectionChangedEvent and LimitChangedEvent
// Requires: WorldEdit 7.3.x + TF-WorldEdit extension plugin installed
public class LimitChangedEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    public Player getPlayer()
    {
        return null;
    }

    public Player getTarget()
    {
        return null;
    }

    public int getLimit()
    {
        return 0;
    }

    public void setLimit(int limit)
    {
    }

    public void setCancelled(boolean cancelled)
    {
    }

    public boolean isCancelled()
    {
        return false;
    }
}

