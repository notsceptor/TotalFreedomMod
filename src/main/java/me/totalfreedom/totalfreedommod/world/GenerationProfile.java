package me.totalfreedom.totalfreedommod.world;

import java.util.List;

import me.totalfreedom.totalfreedommod.world.profile.Bounds;
import me.totalfreedom.totalfreedommod.world.profile.FeatureSpec;
import me.totalfreedom.totalfreedommod.world.profile.Palette;
import me.totalfreedom.totalfreedommod.world.profile.Shape;
import me.totalfreedom.totalfreedommod.world.profile.WorldSettings;

/**
 * One world's profile. Pure data, and every field is already checked, so anything reading this can
 * take it at face value.
 * <p>
 * Holds no stage objects. The chunk generator pattern matches {@link Shape} once to pick its
 * stages, which keeps this package free of any dependency on the generation code.
 */
public record GenerationProfile(String name,
                                Bounds bounds,
                                Shape shape,
                                Palette palette,
                                List<FeatureSpec> features,
                                WorldSettings world)
{
}
