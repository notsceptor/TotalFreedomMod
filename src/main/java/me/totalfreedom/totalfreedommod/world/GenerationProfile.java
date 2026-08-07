package me.totalfreedom.totalfreedommod.world;

import me.totalfreedom.totalfreedommod.world.profile.Bounds;
import me.totalfreedom.totalfreedommod.world.profile.GenerationMode;
import me.totalfreedom.totalfreedommod.world.profile.Palette;
import me.totalfreedom.totalfreedommod.world.profile.StageSet;
import me.totalfreedom.totalfreedommod.world.profile.WorldSettings;

/**
 * One world's compiled profile. Built by the compiler once the seed is known, immutable after that.
 * <p>
 * Holds only what more than one stage needs. Per-stage tuning belongs to the stage: terrain noise
 * and spline in the generator, surface rules in the designer, feature specs in the populator.
 */
public record GenerationProfile(String name,
                                GenerationMode mode,
                                Bounds bounds,
                                Palette palette,
                                StageSet stages,
                                WorldSettings world)
{
}
