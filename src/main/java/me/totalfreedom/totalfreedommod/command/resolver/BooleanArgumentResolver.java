package me.totalfreedom.totalfreedommod.command.resolver;

public class BooleanArgumentResolver implements AbstractArgumentResolver<Boolean>
{

    @Override
    public String name()
    {
        return "Boolean";
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
