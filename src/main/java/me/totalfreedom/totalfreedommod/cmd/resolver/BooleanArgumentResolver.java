package me.totalfreedom.totalfreedommod.cmd.resolver;

import java.util.List;

public class BooleanArgumentResolver implements AbstractArgumentResolver<Boolean>
{

    @Override
    public String name()
    {
        return "Boolean";
    }

    /**
     * Only the canonical spellings; {@link #resolve} accepts the abbreviations too, but suggesting
     * all ten forms buries the two anyone types.
     */
    @Override
    public List<String> suggestions()
    {
        return List.of("true", "false");
    }

    @Override
    public Boolean resolve(String arg, String strategy)
    {
        // DAMN IT
        return switch (arg.toLowerCase())
        {
            case "t", "true", "on", "yes", "1" -> true;
            case "f", "false", "off", "no", "0" -> false;
            default -> throw new ArgumentResolutionException("Expected a true/false value or something similar, got " + arg);
        };
    }
}
