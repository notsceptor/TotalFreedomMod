package me.totalfreedom.totalfreedommod;

import me.totalfreedom.api.FreedomAPI;

import java.util.Collections;
import java.util.List;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FTask;
import me.totalfreedom.totalfreedommod.util.FUtil;

import com.google.common.collect.Lists;

public class Announcer extends FreedomService
{

    private final List<String> announcements = Lists.newArrayList();
    private boolean enabled;
    private long interval;
    private String prefix;
    private BukkitTask announcerTask;

    public Announcer(FreedomAPI plugin)
    {
        super(plugin);
    }

    @Override
    public void onStart()
    {
        enabled = ConfigEntry.ANNOUNCER_ENABLED.getBoolean();
        interval = ConfigEntry.ANNOUNCER_INTERVAL.getInteger() * 20L;
        prefix = ConfigEntry.ANNOUNCER_PREFIX.getString();

        announcements.clear();
        for (Object announcement : ConfigEntry.ANNOUNCER_ANNOUNCEMENTS.getList())
            announcements.add((String) announcement);

        if (!enabled)
            return;

        announcerTask = new BukkitRunnable()
        {
            private int current = 0;

            @Override
            public void run()
            {
                FTask.run("Announcer/announce", () ->
                {
                    current++;

                    if (current >= announcements.size())
                        current = 0;

                    announce(announcements.get(current));
                });
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    @Override
    public void onStop()
    {
        if (announcerTask == null)
            return;

        FUtil.cancel(announcerTask);
        announcerTask = null;
    }

    public List<String> getAnnouncements()
    {
        return Collections.unmodifiableList(announcements);
    }

    public long getInterval()
    {
        return interval;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void announce(String message)
    {
        FUtil.bcastMsg(prefix + message);
    }

}
