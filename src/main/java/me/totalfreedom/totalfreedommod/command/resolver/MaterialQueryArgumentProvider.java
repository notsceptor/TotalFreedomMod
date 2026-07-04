package me.totalfreedom.totalfreedommod.command.resolver;

import org.bukkit.Material;
import org.bukkit.Registry;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MaterialQueryArgumentProvider implements AbstractArgumentResolver<List<Material>>
{
    @Override
    public String name()
    {
        return "MaterialQuery";
    }

    @Override
    public List<Material> resolve(String arg, String strategy)
    {
        final Pattern pattern;

        try
        {
            pattern = Pattern.compile(arg.toLowerCase());
        }
        catch (PatternSyntaxException ex)
        {
            throw new ArgumentResolutionException(ex);
        }

        return Registry.MATERIAL.stream().filter(material -> pattern.matcher(material.key().asString()).find()).toList();
    }
}
