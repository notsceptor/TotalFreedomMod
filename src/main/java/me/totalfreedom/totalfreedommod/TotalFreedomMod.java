package me.totalfreedom.totalfreedommod;

import java.io.File;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.LoggerFactory;

import me.totalfreedom.api.BuildInfo;
import me.totalfreedom.api.FreedomAPI;
import me.totalfreedom.api.economy.IBank;
import me.totalfreedom.totalfreedommod.admin.AdminList;
import me.totalfreedom.totalfreedommod.banning.BanManager;
import me.totalfreedom.totalfreedommod.banning.PermbanList;
import me.totalfreedom.totalfreedommod.blocking.sweep.SweepScheduler;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.config.MainConfig;
import me.totalfreedom.totalfreedommod.eco.EcoManager;
import me.totalfreedom.totalfreedommod.framework.ServiceManager;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.player.PlayerList;
import me.totalfreedom.totalfreedommod.rank.ConsoleSenderRegistry;
import me.totalfreedom.totalfreedommod.rank.RankManager;
import me.totalfreedom.totalfreedommod.sql.FreedomDatabase;
import me.totalfreedom.totalfreedommod.title.TitleManager;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import me.totalfreedom.totalfreedommod.util.MethodTimer;
import me.totalfreedom.totalfreedommod.vanish.VanishService;
import me.totalfreedom.totalfreedommod.world.CleanroomChunkGenerator;
import me.totalfreedom.totalfreedommod.world.WorldManager;

/**
 * Connects {@link FreedomAggregate}, the composition root that owns every service and its
 * lifecycle, to Bukkit's plugin lifecycle. Implements {@link FreedomAPI} by delegating to the
 * aggregate; holds no service state of its own.
 */
public class TotalFreedomMod extends JavaPlugin implements FreedomAPI
{

    public static final String CONFIG_FILENAME = "config.yml";

    private FreedomAggregate aggregate;

    @Override
    public void onLoad()
    {
        PluginProvider.bind(this);

        FLog.setPluginLogger(getSLF4JLogger());
        FLog.setServerLogger(LoggerFactory.getLogger("Minecraft-Server"));

        aggregate = new FreedomAggregate(this);
        aggregate.load();
    }

    @Override
    public void onEnable()
    {
        FLog.info("Created by Madgeek1450 and Prozza");
        FLog.info("Version " + aggregate.buildInfo().formattedVersion());
        FLog.info("Compiled " + aggregate.buildInfo().date() + " by " + aggregate.buildInfo().author());

        final MethodTimer timer = new MethodTimer();
        timer.start();

        // Delete unused files
        FUtil.deleteCoreDumps();
        FUtil.deleteFolder(new File("./_deleteme"));

        BackupManager backups = new BackupManager(this);
        backups.createBackups(TotalFreedomMod.CONFIG_FILENAME, true);
        backups.createBackups(AdminList.CONFIG_FILENAME);
        backups.createBackups(PermbanList.CONFIG_FILENAME);

        aggregate.enable();

        timer.update();
        FLog.info("Version " + getPluginMeta().getVersion() + " enabled in " + timer.getTotal() + "ms");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
    {
        if ("flatlands".equals(worldName))
        {
            String params;
            if (aggregate != null && aggregate.config() != null)
            {
                params = ConfigEntry.FLATLANDS_GENERATE_PARAMS.getString();
            }
            else
            {
                saveDefaultConfig();
                params = getConfig().getString("flatlands.generate_params", "16|stone|32|dirt|1|grass_block");
            }
            return new CleanroomChunkGenerator(params);
        }
        return super.getDefaultWorldGenerator(worldName, id);
    }

    @Override
    public void onDisable()
    {
        if (aggregate != null)
        {
            aggregate.disable();
        }

        getServer().getScheduler().cancelTasks(this);

        FLog.info("Plugin disabled");
        PluginProvider.unbind();
    }

    @Override
    public ServiceManager services()
    {
        return aggregate.services();
    }

    @Override
    public ServiceManager bridges()
    {
        return aggregate.bridges();
    }

    @Override
    public MainConfig config()
    {
        return aggregate.config();
    }

    @Override
    public BuildInfo buildInfo()
    {
        return aggregate.buildInfo();
    }

    @Override
    public FreedomDatabase database()
    {
        return aggregate.database();
    }

    @Override
    public AdminList admins()
    {
        return aggregate.admins();
    }

    @Override
    public PlayerList players()
    {
        return aggregate.players();
    }

    @Override
    public IBank bank()
    {
        return aggregate.bank();
    }

    @Override
    public EcoManager economy()
    {
        return aggregate.economy();
    }

    @Override
    public RankManager ranks()
    {
        return aggregate.ranks();
    }

    @Override
    public TitleManager titles()
    {
        return aggregate.titles();
    }

    @Override
    public WorldManager worlds()
    {
        return aggregate.worlds();
    }

    @Override
    public VanishService vanish()
    {
        return aggregate.vanish();
    }

    @Override
    public ConsoleSenderRegistry consoleSenders()
    {
        return aggregate.consoleSenders();
    }

    @Override
    public BanManager bans()
    {
        return aggregate.bans();
    }

    @Override
    public AutoEject autoEject()
    {
        return aggregate.autoEject();
    }

    @Override
    public SweepScheduler sweepScheduler()
    {
        return aggregate.sweepScheduler();
    }

    @Override
    public GameRuleHandler gameRules()
    {
        return aggregate.gameRules();
    }

    @Override
    public PlayerData getPlayerData(Player target)
    {
        return players().getData(target);
    }

    @Override
    public void savePlayerData(PlayerData data)
    {
        players().saveData(data);
    }

    @Override
    public boolean isAdmin(CommandSender sender)
    {
        return admins().isAdmin(sender);
    }
}
