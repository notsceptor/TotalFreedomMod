package me.totalfreedom.totalfreedommod.command.resolver;

public interface AbstractArgumentResolver<T>
{
    String name();
    T resolve(String arg, String strategy);
}
