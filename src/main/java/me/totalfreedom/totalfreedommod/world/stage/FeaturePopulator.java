package me.totalfreedom.totalfreedommod.world.stage;

import java.util.List;

import org.bukkit.generator.LimitedRegion;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.base.Populator;
import me.totalfreedom.totalfreedommod.world.profile.FeatureSpec;
import me.totalfreedom.totalfreedommod.world.stage.feature.FeatureRegistry;

/**
 * Rolls each feature in the profile against the chunk and hands off the hits. This only decides
 * what gets placed and where; the features do the placing.
 */
public final class FeaturePopulator implements Populator
{
    private final List<FeatureSpec> specs;
    private final FeatureRegistry registry;

    public FeaturePopulator(final List<FeatureSpec> specs, final FeatureRegistry registry)
    {
        this.specs = specs;
        this.registry = registry;
    }

    @Override
    public void populate(final ChunkContext context, final LimitedRegion data)
    {

    }
}
