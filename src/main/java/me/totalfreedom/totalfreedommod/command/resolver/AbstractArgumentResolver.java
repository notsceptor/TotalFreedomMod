package me.totalfreedom.totalfreedommod.command.resolver;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Map;

public interface AbstractArgumentResolver<T>
{
    String name();
    T resolve(String arg, String strategy);

    static Map<String, Object> readStrategyArguments(String strategy)
    {
        return Arrays.stream(strategy.split(","))
                .map(entry -> entry.split("="))
                .filter(split -> split.length > 0 && split.length < 3)
                .collect(ImmutableMap.toImmutableMap(split -> split[0],
                        split -> split.length == 1 ? true : split[1]));
    }
}
