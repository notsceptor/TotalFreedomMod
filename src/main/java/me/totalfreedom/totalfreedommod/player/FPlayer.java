package me.totalfreedom.totalfreedommod.player;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import me.totalfreedom.totalfreedommod.caging.CageData;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.freeze.FreezeData;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class FPlayer
{

    public static final long AUTO_PURGE_TICKS = 5L * 60L * 20L;
    private static final long COUNTER_WINDOW_MS = 1000L;

    @Getter
    private final TotalFreedomMod plugin;
    @Getter
    private final String name;
    @Getter
    private final String ip;
    //
    @Setter
    private Player player;
    //
    private BukkitTask unmuteTask;
    @Getter
    private final FreezeData freezeData = new FreezeData(this);
    @Getter
    private double fuckoffRadius = 0;
    private int messageCount = 0;
    private long messageCountWindowStart = 0L;
    private int totalBlockDestroy = 0;
    private long totalBlockDestroyWindowStart = 0L;
    private int totalBlockPlace = 0;
    private long totalBlockPlaceWindowStart = 0L;
    private int freecamDestroyCount = 0;
    private int freecamPlaceCount = 0;
    @Getter
    private final CageData cageData = new CageData(this);
    private boolean isOrbiting = false;
    private double orbitStrength = 10.0;
    private boolean mobThrowerEnabled = false;
    private EntityType mobThrowerEntity = EntityType.PIG;
    private double mobThrowerSpeed = 4.0;
    private final List<LivingEntity> mobThrowerQueue = new ArrayList<>();
    private BukkitTask mp44ScheduleTask = null;
    private boolean mp44Armed = false;
    private boolean mp44Firing = false;
    private BukkitTask lockupScheduleTask = null;
    private String lastMessage = "";
    private boolean inAdminchat = false;
    private boolean allCommandsBlocked = false;
    @Getter
    @Setter
    private boolean superadminIdVerified = false;
    private String lastCommand = "";
    private boolean cmdspyEnabled = false;
    private String tag = null;
    private int warningCount = 0;

    public FPlayer(TotalFreedomMod plugin, Player player)
    {
        this(plugin, player.getName(), player.getAddress().getAddress().getHostAddress());
    }

    private FPlayer(TotalFreedomMod plugin, String name, String ip)
    {
        this.plugin = plugin;
        this.name = name;
        this.ip = ip;
    }

    public Player getPlayer()
    {
        if (player != null && !player.isOnline())
        {
            player = null;
        }

        if (player == null)
        {
            player = Bukkit.getPlayerExact(name);
        }

        return player;
    }

    public boolean isOrbiting()
    {
        return isOrbiting;
    }

    public void startOrbiting(double strength)
    {
        this.isOrbiting = true;
        this.orbitStrength = strength;
    }

    public void stopOrbiting()
    {
        this.isOrbiting = false;
    }

    public double orbitStrength()
    {
        return orbitStrength;
    }

    public boolean isFuckOff()
    {
        return fuckoffRadius > 0;
    }

    public void setFuckoff(double radius)
    {
        this.fuckoffRadius = radius;
    }

    public void disableFuckoff()
    {
        this.fuckoffRadius = 0;
    }

    public void resetMsgCount()
    {
        this.messageCount = 0;
        this.messageCountWindowStart = System.currentTimeMillis();
    }

    public int incrementAndGetMsgCount()
    {
        final long now = System.currentTimeMillis();
        if (now - messageCountWindowStart > COUNTER_WINDOW_MS)
        {
            messageCount = 0;
            messageCountWindowStart = now;
        }
        return this.messageCount++;
    }

    public int incrementAndGetBlockDestroyCount()
    {
        final long now = System.currentTimeMillis();
        if (now - totalBlockDestroyWindowStart > COUNTER_WINDOW_MS)
        {
            totalBlockDestroy = 0;
            totalBlockDestroyWindowStart = now;
        }
        return this.totalBlockDestroy++;
    }

    public void resetBlockDestroyCount()
    {
        this.totalBlockDestroy = 0;
        this.totalBlockDestroyWindowStart = System.currentTimeMillis();
    }

    public int incrementAndGetBlockPlaceCount()
    {
        final long now = System.currentTimeMillis();
        if (now - totalBlockPlaceWindowStart > COUNTER_WINDOW_MS)
        {
            totalBlockPlace = 0;
            totalBlockPlaceWindowStart = now;
        }
        return this.totalBlockPlace++;
    }

    public void resetBlockPlaceCount()
    {
        this.totalBlockPlace = 0;
        this.totalBlockPlaceWindowStart = System.currentTimeMillis();
    }

    public int incrementAndGetFreecamDestroyCount()
    {
        return this.freecamDestroyCount++;
    }

    public void resetFreecamDestroyCount()
    {
        this.freecamDestroyCount = 0;
    }

    public int incrementAndGetFreecamPlaceCount()
    {
        return this.freecamPlaceCount++;
    }

    public void resetFreecamPlaceCount()
    {
        this.freecamPlaceCount = 0;
    }

    public void enableMobThrower(EntityType mobThrowerCreature, double mobThrowerSpeed)
    {
        this.mobThrowerEnabled = true;
        this.mobThrowerEntity = mobThrowerCreature;
        this.mobThrowerSpeed = mobThrowerSpeed;
    }

    public void disableMobThrower()
    {
        this.mobThrowerEnabled = false;
    }

    public EntityType mobThrowerCreature()
    {
        return this.mobThrowerEntity;
    }

    public double mobThrowerSpeed()
    {
        return this.mobThrowerSpeed;
    }

    public boolean mobThrowerEnabled()
    {
        return this.mobThrowerEnabled;
    }

    public void enqueueMob(LivingEntity mob)
    {
        mobThrowerQueue.add(mob);
        if (mobThrowerQueue.size() > 4)
        {
            LivingEntity oldmob = mobThrowerQueue.remove(0);
            if (oldmob != null)
            {
                oldmob.damage(500.0);
            }
        }
    }

    public void startArrowShooter(TotalFreedomMod plugin)
    {
        this.stopArrowShooter();
        this.mp44ScheduleTask = new ArrowShooter(this.player).runTaskTimer(plugin, 1L, 1L);
        this.mp44Firing = true;
    }

    public void stopArrowShooter()
    {
        if (this.mp44ScheduleTask != null)
        {
            this.mp44ScheduleTask.cancel();
            this.mp44ScheduleTask = null;
        }
        this.mp44Firing = false;
    }

    public void armMP44()
    {
        this.mp44Armed = true;
        this.stopArrowShooter();
    }

    public void disarmMP44()
    {
        this.mp44Armed = false;
        this.stopArrowShooter();
    }

    public boolean isMP44Armed()
    {
        return this.mp44Armed;
    }

    public boolean toggleMP44Firing()
    {
        this.mp44Firing = !this.mp44Firing;
        return mp44Firing;
    }

    public boolean isMuted()
    {
        return unmuteTask != null;
    }

    public void setMuted(boolean muted)
    {
        FUtil.cancel(unmuteTask);
        unmuteTask = null;

        if (!muted)
        {
            return;
        }

        if (getPlayer() == null)
        {
            return;
        }
        unmuteTask = plugin.getServer().getScheduler().runTaskLater(plugin, () ->
        {
            FUtil.adminAction("TotalFreedom", "Unmuting " + getPlayer().getName(), false);
            setMuted(false);
        }, AUTO_PURGE_TICKS);
    }

    public BukkitTask getLockupScheduleID()
    {
        return this.lockupScheduleTask;
    }

    public void setLockupScheduleId(BukkitTask id)
    {
        this.lockupScheduleTask = id;
    }

    public void setLastMessage(String message)
    {
        this.lastMessage = message;
    }

    public String getLastMessage()
    {
        return lastMessage;
    }

    public void setAdminChat(boolean inAdminchat)
    {
        this.inAdminchat = inAdminchat;
    }

    public boolean inAdminChat()
    {
        return this.inAdminchat;
    }

    public boolean allCommandsBlocked()
    {
        return this.allCommandsBlocked;
    }

    public void setCommandsBlocked(boolean commandsBlocked)
    {
        this.allCommandsBlocked = commandsBlocked;
    }

    public String getLastCommand()
    {
        return lastCommand;
    }

    public void setLastCommand(String lastCommand)
    {
        this.lastCommand = lastCommand;
    }

    public void setCommandSpy(boolean enabled)
    {
        this.cmdspyEnabled = enabled;
    }

    public boolean cmdspyEnabled()
    {
        return cmdspyEnabled;
    }

    public void setTag(String tag)
    {
        if (tag == null || tag.isEmpty())
        {
            this.tag = tag;
        }
        else
        {
            String colorizedTag = AdventureUtil.componentToLegacySection(FUtil.colorize(tag));
            this.tag = colorizedTag + "§f"; // §f is white
        }
    }

    public String getTag()
    {
        return this.tag;
    }

    public int getWarningCount()
    {
        return this.warningCount;
    }

    public void incrementWarnings()
    {
        this.warningCount++;

        if (this.warningCount % 2 == 0)
        {
            Player p = getPlayer();
            p.getWorld().strikeLightning(p.getLocation());
            FUtil.playerMsg(p, "You have been warned at least twice now, make sure to read the rules at " + ConfigEntry.SERVER_BAN_URL.getString(), NamedTextColor.RED);
        }
    }

    private class ArrowShooter extends BukkitRunnable
    {

        private final Player player;

        private ArrowShooter(Player player)
        {
            this.player = player;
        }

        @Override
        public void run()
        {
            Arrow shot = player.launchProjectile(Arrow.class);
            shot.setVelocity(shot.getVelocity().multiply(2.0));
        }
    }
}
