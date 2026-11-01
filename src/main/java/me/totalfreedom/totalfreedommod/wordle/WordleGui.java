package me.totalfreedom.totalfreedommod.wordle;

import org.bukkit.inventory.Inventory;

final class WordleGui implements WordleInventoryHolder
{
    private Inventory inventory;

    @Override
    public Inventory getInventory()
    {
        return inventory;
    }

    void setInventory(final Inventory inventory)
    {
        this.inventory = inventory;
    }
}
