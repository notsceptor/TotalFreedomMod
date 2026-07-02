package me.totalfreedom.totalfreedommod.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class DetectionReporter
{

    @FunctionalInterface
    public interface Summary
    {
        String format(long count, String reason, long maxObservedSize, String sample);
    }

    private final long intervalTicks;
    private final LongSupplier clock;
    private final Summary summary;
    private final Consumer<String> sink;

    private long lastSummaryTick;
    private long count;
    private long maxObservedSize;
    private String dominantReason;
    private String sample;

    public DetectionReporter(long intervalTicks, LongSupplier clock, Summary summary, Consumer<String> sink)
    {
        this.intervalTicks = Math.max(1L, intervalTicks);
        this.clock = Objects.requireNonNull(clock);
        this.summary = Objects.requireNonNull(summary);
        this.sink = Objects.requireNonNull(sink);
    }

    public synchronized void record(String sample)
    {
        record(null, 0L, sample);
    }

    public synchronized void record(String reason, String sample)
    {
        record(reason, 0L, sample);
    }

    public synchronized void record(String reason, long observedSize, String sample)
    {
        count++;
        maxObservedSize = Math.max(maxObservedSize, observedSize);
        if (dominantReason == null && reason != null)
        {
            dominantReason = reason;
        }
        if (this.sample == null)
        {
            this.sample = sample;
        }

        long now = clock.getAsLong();
        if (lastSummaryTick != 0L && now - lastSummaryTick < intervalTicks)
        {
            return;
        }

        sink.accept(summary.format(count, dominantReason, maxObservedSize, this.sample));
        lastSummaryTick = now;
        count = 0L;
        maxObservedSize = 0L;
        dominantReason = null;
        this.sample = null;
    }

    public static Consumer<String> warnOnly()
    {
        return FLog::warning;
    }

    public static Consumer<String> warnAndBroadcastAdmins(TotalFreedomMod plugin)
    {
        return message ->
        {
            FLog.warning(message);
            Component component = Component.text(message, NamedTextColor.RED);
            for (Player player : plugin.getServer().getOnlinePlayers())
            {
                if (plugin.al != null && plugin.al.isAdmin(player))
                {
                    player.sendMessage(component);
                }
            }
        };
    }
}
