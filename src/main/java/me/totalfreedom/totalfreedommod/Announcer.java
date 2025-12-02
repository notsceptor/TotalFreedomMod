package me.totalfreedom.totalfreedommod;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Announcer extends FreedomService
{

    private final List<String> announcements = Lists.newArrayList();
    @Getter
    private boolean enabled;
    @Getter
    private long interval;
    @Getter
    private String prefix;
    private BukkitTask announcer;

    public Announcer(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        enabled = ConfigEntry.ANNOUNCER_ENABLED.getBoolean();
        interval = ConfigEntry.ANNOUNCER_INTERVAL.getInteger() * 20L;
        prefix = AdventureUtil.componentToLegacy(FUtil.colorize(ConfigEntry.ANNOUNCER_PREFIX.getString()));

        announcements.clear();
        for (Object announcement : ConfigEntry.ANNOUNCER_ANNOUNCEMENTS.getList())
        {
            announcements.add(AdventureUtil.componentToLegacy(FUtil.colorize((String) announcement)));
        }

        if (!enabled)
        {
            return;
        }

        announcer = new BukkitRunnable()
        {
            private int current = 0;

            @Override
            public void run()
            {
                current++;

                if (current >= announcements.size())
                {
                    current = 0;
                }

                announce(announcements.get(current));
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    @Override
    protected void onStop()
    {
        if (announcer == null)
        {
            return;
        }

        FUtil.cancel(announcer);
        announcer = null;
    }

    public List<String> getAnnouncements()
    {
        return Collections.unmodifiableList(announcements);
    }

    public void announce(String message)
    {
        // Convert legacy string with & codes to Component for proper color rendering
        String fullMessage = prefix + message;
        FUtil.bcastMsg(AdventureUtil.legacyToComponent(fullMessage));
    }

}
