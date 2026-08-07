package me.totalfreedom.totalfreedommod.world.profile;

import me.totalfreedom.totalfreedommod.world.base.Carver;
import me.totalfreedom.totalfreedommod.world.base.Designer;
import me.totalfreedom.totalfreedommod.world.base.Generator;
import me.totalfreedom.totalfreedommod.world.base.Populator;

/**
 * The four stages a profile runs, picked from the mode at compile time.
 * <p>
 * Also how the chunk context reaches the generator and carver for its column heights.
 */
public record StageSet(Generator generator,
                       Designer designer,
                       Carver carver,
                       Populator populator)
{
}
