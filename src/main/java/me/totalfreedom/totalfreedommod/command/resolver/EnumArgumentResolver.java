package me.totalfreedom.totalfreedommod.command.resolver;

import com.google.common.base.Enums;
import com.google.common.base.Optional;

import java.util.Map;
import java.util.function.Function;

public class EnumArgumentResolver implements AbstractParameterizedArgumentResolver<Enum<?>>
{

    @Override
    public String name()
    {
        return "Enum";
    }

    @Override
    public Enum<?> resolve(String arg, Map<String, Object> parameters)
    {
        final String clazzString = (String) parameters.get("class");
        final EnumCapitalization mode = EnumCapitalization.fromString((String) parameters.getOrDefault("mode", "none"));

        if (clazzString == null)
        {
            throw new IllegalArgumentException("Missing required 'class' parameter");
        }

        try
        {
            return fetchEnumObject(clazzString, mode.acceptInput(arg));
        }
        catch (ClassCastException ex)
        {
            throw new IllegalArgumentException("Enum class was found, but it is not what we're looking for", ex);
        }
        catch (ClassNotFoundException ex)
        {
            throw new IllegalArgumentException("Unable to find enum class " + clazzString, ex);
        }
    }

    public <T extends Enum<T>> Enum<T> fetchEnumObject(String clazz, String arg)
            throws ClassNotFoundException
    {
        final Class<T> enumClazz = (Class<T>) Class.forName(clazz);
        final Optional<T> object = Enums.getIfPresent(enumClazz, arg);

        if (!object.isPresent())
        {
            throw new ArgumentResolutionException("Invalid mode: " + arg);
        }

        return object.get();
    }

    public enum EnumCapitalization
    {
        LOWERCASE(String::toLowerCase),
        UPPERCASE(String::toUpperCase),
        NONE;

        private final Function<String, String> capitalizer;

        EnumCapitalization()
        {
            this.capitalizer = null;
        }

        EnumCapitalization(Function<String, String> action)
        {
            this.capitalizer = action;
        }

        public String acceptInput(String input)
        {
            if (capitalizer == null)
            {
                return input;
            }

            return capitalizer.apply(input);
        }

        public static EnumCapitalization fromString(String value)
        {
            try
            {
                return valueOf(value.toUpperCase());
            }
            catch (IllegalArgumentException _)
            {
                throw new IllegalArgumentException("Invalid capitalization mode: " + value);
            }
        }
    }
}
