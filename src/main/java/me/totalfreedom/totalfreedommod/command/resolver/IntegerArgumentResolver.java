package me.totalfreedom.totalfreedommod.command.resolver;

public class IntegerArgumentResolver implements AbstractArgumentResolver<Integer>
{
    @Override
    public String name()
    {
        return "Integer";
    }

    @Override
    public Integer resolve(String arg, String strategy)
    {
        try
        {
            return Integer.parseInt(arg);
        }
        catch (NumberFormatException ex)
        {
            throw new ArgumentResolutionException(ex);
        }
    }
}
