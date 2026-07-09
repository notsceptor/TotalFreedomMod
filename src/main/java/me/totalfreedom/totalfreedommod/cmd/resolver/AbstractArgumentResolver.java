package me.totalfreedom.totalfreedommod.cmd.resolver;

public interface AbstractArgumentResolver<T>
{
    String name();
    T resolve(String arg, String strategy);
}
