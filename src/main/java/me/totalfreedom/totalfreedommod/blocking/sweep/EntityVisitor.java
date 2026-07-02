package me.totalfreedom.totalfreedommod.blocking.sweep;

import org.bukkit.entity.Entity;

public interface EntityVisitor
{
    boolean enabled();

    long sweepIntervalTicks();

    void visit(Entity entity, SweepContext context);
}
