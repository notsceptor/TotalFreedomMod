package me.totalfreedom.totalfreedommod.world.stage;

import org.bukkit.generator.ChunkGenerator;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.base.Generator;
import me.totalfreedom.totalfreedommod.world.profile.Bounds;
import me.totalfreedom.totalfreedommod.world.profile.LayerStack;

/**
 * Flat mode. Samples no noise at all and writes each layer as a single setRegion call.
 */
public final class FlatGenerator implements Generator
{
    private final LayerStack layers;
    private final Bounds bounds;

    public FlatGenerator(final LayerStack layers, final Bounds bounds)
    {
        this.layers = layers;
        this.bounds = bounds;
    }

    @Override
    public void generateBase(final ChunkContext context, final ChunkGenerator.ChunkData data)
    {

    }

    @Override
    public int surfaceHeight(final int worldX, final int worldZ)
    {

    }
}
