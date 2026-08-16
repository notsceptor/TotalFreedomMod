package me.totalfreedom.totalfreedommod.cmd.resolver;

import org.bukkit.NamespacedKey;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;

import me.totalfreedom.api.cmd.resolver.AbstractArgumentResolver;
import me.totalfreedom.api.cmd.resolver.ArgumentResolutionException;

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
