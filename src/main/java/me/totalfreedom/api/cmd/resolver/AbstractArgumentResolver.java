package me.totalfreedom.api.cmd.resolver;

import java.util.List;

public interface AbstractArgumentResolver<T>
{
    String name();

    T resolve(String arg, String strategy);

    /**
     * This is the fallback used when the parameter has no {@code @Completer} and the
     * {@link me.totalfreedom.totalfreedommod.cmd.internal.ResolverRegistry ResolverRegistry}
     * registration supplied no type-bound candidate list. Implement it on resolvers reached by
     * {@code @Resolve("<name>")}, since those have no parameter type to key a registration off.
     *
     * @return candidates, or an empty list when the argument has no enumerable value set
     */
    default List<String> suggestions()
    {
        return List.of();
    }
}
