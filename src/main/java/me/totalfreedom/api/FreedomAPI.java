package me.totalfreedom.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.totalfreedom.api.economy.IBank;

// all of these imports should prob be in me.totalfreedom.api instead
import me.totalfreedom.totalfreedommod.AutoEject;
import me.totalfreedom.totalfreedommod.GameRuleHandler;
import me.totalfreedom.totalfreedommod.admin.AdminList;
import me.totalfreedom.totalfreedommod.banning.BanManager;
import me.totalfreedom.totalfreedommod.blocking.sweep.SweepScheduler;
import me.totalfreedom.totalfreedommod.config.MainConfig;
import me.totalfreedom.totalfreedommod.eco.EcoManager;
import me.totalfreedom.totalfreedommod.framework.ServiceManager;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.player.PlayerList;
import me.totalfreedom.totalfreedommod.rank.ConsoleSenderRegistry;
import me.totalfreedom.totalfreedommod.rank.RankManager;
import me.totalfreedom.totalfreedommod.sql.FreedomDatabase;
import me.totalfreedom.totalfreedommod.title.TitleManager;
import me.totalfreedom.totalfreedommod.vanish.VanishService;
import me.totalfreedom.totalfreedommod.world.WorldManager;

/**
 * Curated capability surface exposed to services and commands. Backed by
 * {@code TotalFreedomMod} and its {@link ServiceManager}, this interface stands in for the
 * plugin class itself so consumers depend on capabilities rather than the god object that
 * provides them.
 */
public interface FreedomAPI extends Plugin
{

    /**
     * @return The service registry backing this API. Prefer the named accessors below for
     * frequently used services; fall back to {@code services().require(Class)} for the rest.
     */
    ServiceManager services();

    /**
     * @return The registry for optional third-party plugin integrations (CoreProtect,
     * Essentials, WorldEdit, LibsDisguises).
     */
    ServiceManager bridges();

    IBank bank();

    /**
     * @return The service owning every online player's {@link me.totalfreedom.api.economy.IEcoPlayer}.
     */
    EcoManager economy();

    MainConfig config();

    BuildInfo buildInfo();

    FreedomDatabase database();

    AdminList admins();

    PlayerList players();

    RankManager ranks();

    TitleManager titles();

    WorldManager worlds();

    VanishService vanish();

    ConsoleSenderRegistry consoleSenders();

    BanManager bans();

    AutoEject autoEject();

    SweepScheduler sweepScheduler();

    GameRuleHandler gameRules();

    /**
     * @return {@code target}'s stored player data, loading it if necessary.
     */
    PlayerData getPlayerData(Player target);

    /**
     * Persists {@code data} back to storage.
     */
    void savePlayerData(PlayerData data);

    /**
     * @return Whether {@code sender} is a currently active admin.
     */
    boolean isAdmin(CommandSender sender);
}
