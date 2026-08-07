package me.totalfreedom.totalfreedommod.world.profile.json;

/**
 * A profile record that can fill in its own missing fields.
 * <p>
 * Return a copy with every null replaced, including whole sections that were absent. Gson leaves
 * absent fields null and never runs compact constructors, so defaults cannot live in the record.
 *
 * @param <T> the implementing record's own type
 */
public interface Defaulted<T>
{
    T withDefaults();
}
