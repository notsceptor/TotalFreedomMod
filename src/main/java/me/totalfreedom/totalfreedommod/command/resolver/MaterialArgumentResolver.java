package me.totalfreedom.totalfreedommod.command.resolver;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

public class MaterialArgumentResolver implements AbstractArgumentResolver<Material>
{
    @Override
    public String name()
    {
        return "Material";
    }

    @Override
    public Material resolve(String arg, String strategy)
    {
        try
        {
            final Key key = arg.contains(":") ? Key.key(arg.toLowerCase()) : NamespacedKey.minecraft(arg.toLowerCase()).key();
            final Material material = Registry.MATERIAL.get(key);

            if (material == null)
            {
                throw new ArgumentResolutionException("Invalid item: " + arg);
            }

            switch (strategy)
            {
                case "blocks" ->
                {
                    if (!material.isBlock())
                    {
                        throw new ArgumentResolutionException(material.key().asString() + " is a valid item, however it is not a block");
                    }
                }
                case "items" ->
                {
                    if (!material.isItem())
                    {
                        throw new ArgumentResolutionException(material.key().asString() + " is a valid block, however it is not an item");
                    }
                }
            }

            return material;
        }
        catch (InvalidKeyException | IllegalArgumentException ex)
        {
            throw new ArgumentResolutionException("Invalid key: " + arg);
        }
    }
}
