package me.totalfreedom.totalfreedommod.world.stage;

import java.util.List;

import org.bukkit.generator.ChunkGenerator;

import me.totalfreedom.totalfreedommod.world.base.ChunkContext;
import me.totalfreedom.totalfreedommod.world.base.Designer;
import me.totalfreedom.totalfreedommod.world.profile.Materials;
import me.totalfreedom.totalfreedommod.world.profile.SurfaceRule;

/**
 * Applies the profile's surface rules. Walks each column down from the context's column top with a
 * depth counter that resets on air gaps, so cave floors get their own treatment.
 */
public final class RuleDesigner implements Designer
{
    private final List<SurfaceRule> rules;
    private final Materials materials;

    public RuleDesigner(final List<SurfaceRule> rules, final Materials materials)
    {
        this.rules = rules;
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
