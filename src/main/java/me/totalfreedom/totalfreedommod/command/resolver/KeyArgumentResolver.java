package me.totalfreedom.totalfreedommod.command.resolver;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;

public class KeyArgumentResolver implements AbstractArgumentResolver<Key>
{
    @Override
    public String name()
    {
        return "Key";
    }

    @Override
    public Key resolve(String arg, String strategy)
    {
        try
        {
            return arg.contains(":") ? Key.key(arg.toLowerCase()) : NamespacedKey.minecraft(arg.toLowerCase()).key();
        }
        catch (InvalidKeyException | IllegalArgumentException ex)
        {
            throw new ArgumentResolutionException("Invalid key: " + arg);
        }
    }
}
