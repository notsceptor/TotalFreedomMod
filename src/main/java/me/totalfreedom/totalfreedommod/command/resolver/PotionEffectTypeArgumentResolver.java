package me.totalfreedom.totalfreedommod.command.resolver;

import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectTypeArgumentResolver implements AbstractArgumentResolver<PotionEffectType>
{
    @Override
    public String name()
    {
        return "PotionEffectType";
    }

    @Override
    public PotionEffectType resolve(String arg, String strategy)
    {
        PotionEffectType type;

        // Try with numerical ID - this is probably going to stop working when Mojang eventually moves everything to
        //  namespaces like they did with Enchantments and Attributes
        try
        {
            final Integer id = Integer.parseInt(arg);
            type = PotionEffectType.getById(id);
        }
        catch (NumberFormatException ignored)
        {
            final Key key = arg.contains(":") ? Key.key(arg.toLowerCase()) : NamespacedKey.minecraft(arg.toLowerCase()).key();
            type = Registry.POTION_EFFECT_TYPE.get(key);
        }

        if (type == null)
        {
            throw new ArgumentResolutionException("Invalid potion type: " + arg);
        }

        return type;
    }
}
