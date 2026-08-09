package me.totalfreedom.totalfreedommod.world.stage;

import org.bukkit.generator.ChunkGenerator;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.base.Designer;
import me.totalfreedom.totalfreedommod.world.profile.LayerStack;
import me.totalfreedom.totalfreedommod.world.profile.Materials;

/**
 * Flat mode's designer. The layer stack already named every block, so surface is a no-op and only
 * bedrock gets written here.
 */
public final class LayerDesigner implements Designer
{
    private final LayerStack layers;
    private final Materials materials;

    public LayerDesigner(final LayerStack layers, final Materials materials)
    {
        this.layers = layers;
        this.materials = materials;
    }

    @Override
    public void surface(final ChunkContext context, final ChunkGenerator.ChunkData data)
    {

    }

    @Override
    public void bedrock(final ChunkContext context, final ChunkGenerator.ChunkData data)
    {

    }
}
