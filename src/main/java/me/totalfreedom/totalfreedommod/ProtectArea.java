package me.totalfreedom.totalfreedommod;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.util.FLog;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.util.Vector;

public class ProtectArea extends FreedomService
{

    public static final String DATA_FILENAME = "protectedareas.yml";
    public static final String LEGACY_DATA_FILENAME = "protectedareas.dat";
    public static final double MAX_RADIUS = 50.0;
    //
    private final Map<String, ProtectedRegion> areas = Maps.newHashMap();

    public ProtectArea(TotalFreedomMod plugin)
    {
        super(plugin);
    }

    @Override
    protected void onStart()
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        File ymlFile = new File(plugin.getDataFolder(), DATA_FILENAME);
        File legacyFile = new File(plugin.getDataFolder(), LEGACY_DATA_FILENAME);

        if (legacyFile.exists() && !ymlFile.exists())
        {
            migrateLegacyData(legacyFile, ymlFile);
        }

        loadFromYaml(ymlFile);
        Bukkit.getScheduler().runTask(plugin, this::cleanProtectedAreas);
    }

    @SuppressWarnings("unchecked")
    private void migrateLegacyData(File legacyFile, File ymlFile)
    {
        FLog.info("Migrating protected areas from legacy .dat format to .yml format...");
        try (FileInputStream fis = new FileInputStream(legacyFile);
             ObjectInputStream ois = new ObjectInputStream(fis))
        {
            HashMap<String, SerializableProtectedRegion> legacyAreas = 
                (HashMap<String, SerializableProtectedRegion>) ois.readObject();
            
            areas.clear();
            for (Map.Entry<String, SerializableProtectedRegion> entry : legacyAreas.entrySet())
            {
                SerializableProtectedRegion legacy = entry.getValue();
                areas.put(entry.getKey(), new ProtectedRegion(
                    legacy.x, legacy.y, legacy.z, 
                    legacy.radius, 
                    legacy.worldName, 
                    legacy.worldUUID
                ));
            }
            
            save();
            
            File oldFile = new File(legacyFile.getParent(), LEGACY_DATA_FILENAME + ".old");
            if (legacyFile.renameTo(oldFile))
            {
                FLog.info("Migration complete. Legacy file renamed to " + LEGACY_DATA_FILENAME + ".old");
            }
            else
            {
                FLog.warning("Migration complete but could not rename legacy file.");
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Failed to migrate legacy protected areas data: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    private void loadFromYaml(File file)
    {
        areas.clear();
        
        if (!file.exists())
        {
            return;
        }

        try
        {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection areasSection = config.getConfigurationSection("areas");
            
            if (areasSection == null)
            {
                return;
            }

            for (String label : areasSection.getKeys(false))
            {
                ConfigurationSection areaSection = areasSection.getConfigurationSection(label);
                if (areaSection == null)
                {
                    continue;
                }

                double x = areaSection.getDouble("x");
                double y = areaSection.getDouble("y");
                double z = areaSection.getDouble("z");
                double radius = areaSection.getDouble("radius");
                String worldName = areaSection.getString("world_name");
                String worldUuidStr = areaSection.getString("world_uuid");
                UUID worldUUID = worldUuidStr != null ? UUID.fromString(worldUuidStr) : null;

                areas.put(label, new ProtectedRegion(x, y, z, radius, worldName, worldUUID));
            }
        }
        catch (Exception ex)
        {
            FLog.severe("Failed to load protected areas: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    @Override
    protected void onStop()
    {
        save();
    }

    public void save()
    {
        try
        {
            YamlConfiguration config = new YamlConfiguration();
            ConfigurationSection areasSection = config.createSection("areas");

            for (Map.Entry<String, ProtectedRegion> entry : areas.entrySet())
            {
                ConfigurationSection areaSection = areasSection.createSection(entry.getKey());
                ProtectedRegion region = entry.getValue();
                
                areaSection.set("x", region.x);
                areaSection.set("y", region.y);
                areaSection.set("z", region.z);
                areaSection.set("radius", region.radius);
                areaSection.set("world_name", region.worldName);
                areaSection.set("world_uuid", region.worldUUID != null ? region.worldUUID.toString() : null);
            }

            config.save(new File(plugin.getDataFolder(), DATA_FILENAME));
        }
        catch (IOException ex)
        {
            FLog.severe("Failed to save protected areas: " + ex.getMessage());
            FLog.severe(ex);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return;
        }

        final Location location = event.getBlock().getLocation();

        if (isInProtectedArea(location))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return;
        }

        final Location location = event.getBlock().getLocation();

        if (isInProtectedArea(location))
        {
            event.setCancelled(true);
        }
    }

    // Entity explosions (TNT, Creepers, Withers, etc.)
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityExplode(EntityExplodeEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        event.blockList().removeIf(block -> isInProtectedArea(block.getLocation()));
    }

    // Block explosions (beds in nether, respawn anchors)
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockExplode(BlockExplodeEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        event.blockList().removeIf(block -> isInProtectedArea(block.getLocation()));
    }

    // Enderman picking up blocks, falling blocks, etc.
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityChangeBlock(EntityChangeBlockEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        if (isInProtectedArea(event.getBlock().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    // Water/lava bucket placement
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketEmpty(PlayerBucketEmptyEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return;
        }

        if (isInProtectedArea(event.getBlock().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    // Water/lava bucket removal
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBucketFill(PlayerBucketFillEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return;
        }

        if (isInProtectedArea(event.getBlock().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    // Fire starting
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockIgnite(BlockIgniteEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        final Player player = event.getPlayer();
        if (player != null && plugin.al.isAdmin(player))
        {
            return;
        }

        if (isInProtectedArea(event.getBlock().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    // Fire spread
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockSpread(BlockSpreadEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        // Only block fire spread
        if (event.getSource().getType() == org.bukkit.Material.FIRE)
        {
            if (isInProtectedArea(event.getBlock().getLocation()))
            {
                event.setCancelled(true);
            }
        }
    }

    // Blocks burning
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBurn(BlockBurnEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        if (isInProtectedArea(event.getBlock().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    // Water/lava flow
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockFromTo(BlockFromToEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        // Check if liquid is flowing INTO a protected area from outside
        if (!isInProtectedArea(event.getBlock().getLocation()) && isInProtectedArea(event.getToBlock().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    // Piston extend
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPistonExtend(BlockPistonExtendEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        for (Block block : event.getBlocks())
        {
            if (isInProtectedArea(block.getLocation()))
            {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Piston retract
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPistonRetract(BlockPistonRetractEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        for (Block block : event.getBlocks())
        {
            if (isInProtectedArea(block.getLocation()))
            {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Placing paintings, item frames, etc.
    @EventHandler(priority = EventPriority.NORMAL)
    public void onHangingPlace(HangingPlaceEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        final Player player = event.getPlayer();
        if (player != null && plugin.al.isAdmin(player))
        {
            return;
        }

        if (isInProtectedArea(event.getEntity().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    // Breaking paintings, item frames by entity
    @EventHandler(priority = EventPriority.NORMAL)
    public void onHangingBreak(HangingBreakByEntityEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        Entity remover = event.getRemover();
        if (remover instanceof Player)
        {
            Player player = (Player) remover;
            if (plugin.al.isAdmin(player))
            {
                return;
            }
        }

        if (isInProtectedArea(event.getEntity().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    // Vehicle destruction (minecarts, boats)
    @EventHandler(priority = EventPriority.NORMAL)
    public void onVehicleDestroy(VehicleDestroyEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        Entity attacker = event.getAttacker();
        if (attacker instanceof Player)
        {
            Player player = (Player) attacker;
            if (plugin.al.isAdmin(player))
            {
                return;
            }
        }

        if (isInProtectedArea(event.getVehicle().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    // Block fade (ice melting, snow melting, etc.) - protect structure integrity
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockFade(BlockFadeEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        if (isInProtectedArea(event.getBlock().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    // Sign text editing (Minecraft 1.20+ allows editing signs after placement)
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignChange(SignChangeEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        final Player player = event.getPlayer();
        if (plugin.al.isAdmin(player))
        {
            return;
        }

        if (isInProtectedArea(event.getBlock().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    // Player interact (crop trampling, etc.)
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        final Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null)
        {
            return;
        }

        final Location location = block.getLocation();
        if (!shouldBlockInteraction(player, location))
        {
            return;
        }

        if (event.getAction() == org.bukkit.event.block.Action.PHYSICAL)
        {
            event.setCancelled(true);
            return;
        }

        // block right-click interactions
		if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
        {
            if (event.getItem() != null)
            {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        final Player player = event.getPlayer();
        final Location location = event.getRightClicked().getLocation();
        
        if (shouldBlockInteraction(player, location))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        if (!ConfigEntry.PROTECTAREA_PROTECT_PLAYERS.getBoolean())
        {
            return;
        }

        if (!(event.getEntity() instanceof Player))
        {
            return;
        }

        if (isInProtectedArea(event.getEntity().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        if (!ConfigEntry.PROTECTAREA_BLOCK_POTIONS.getBoolean())
        {
            return;
        }

        for (LivingEntity affected : event.getAffectedEntities())
        {
            if (affected instanceof Player && isInProtectedArea(affected.getLocation()))
            {
                event.setIntensity(affected, 0.0D);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLingeringPotionSplash(LingeringPotionSplashEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        if (!ConfigEntry.PROTECTAREA_BLOCK_POTIONS.getBoolean())
        {
            return;
        }

        if (isInProtectedArea(event.getEntity().getLocation()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        if (!ConfigEntry.PROTECTAREA_BLOCK_POTIONS.getBoolean())
        {
            return;
        }

        event.getAffectedEntities().removeIf(
                entity -> entity instanceof Player && isInProtectedArea(entity.getLocation()));
    }

    private boolean shouldBlockInteraction(Player player, Location location)
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return false;
        }

        if (player != null && plugin.al.isAdmin(player))
        {
            return false;
        }

        return isInProtectedArea(location);
    }

    public boolean isInProtectedArea(final Location modifyLocation)
    {
        boolean doSave = false;
        boolean inProtectedArea = false;

        final Iterator<Map.Entry<String, ProtectedRegion>> it = areas.entrySet().iterator();

        while (it.hasNext())
        {
            final ProtectedRegion region = it.next().getValue();

            Location regionCenter = null;
            try
            {
                regionCenter = region.getLocation();
            }
            catch (ProtectedRegion.CantFindWorldException ex)
            {
                it.remove();
                doSave = true;
                continue;
            }

            if (regionCenter != null)
            {
                if (modifyLocation.getWorld() == regionCenter.getWorld())
                {
                    final double regionRadius = region.getRadius();
                    if (modifyLocation.distanceSquared(regionCenter) <= (regionRadius * regionRadius))
                    {
                        inProtectedArea = true;
                        break;
                    }
                }
            }
        }

        if (doSave)
        {
            save();
        }

        return inProtectedArea;
    }

    public boolean isInProtectedArea(final Vector min, final Vector max, final String worldName)
    {
        boolean doSave = false;
        boolean inProtectedArea = false;

        final Iterator<Map.Entry<String, ProtectedRegion>> it = areas.entrySet().iterator();

        while (it.hasNext())
        {
            final ProtectedRegion region = it.next().getValue();

            Location regionCenter = null;
            try
            {
                regionCenter = region.getLocation();
            }
            catch (ProtectedRegion.CantFindWorldException ex)
            {
                it.remove();
                doSave = true;
                continue;
            }

            if (regionCenter != null)
            {
                if (worldName.equals(regionCenter.getWorld().getName()))
                {
                    if (cubeIntersectsSphere(min, max, regionCenter.toVector(), region.getRadius()))
                    {
                        inProtectedArea = true;
                        break;
                    }
                }
            }
        }

        if (doSave)
        {
            save();
        }

        return inProtectedArea;
    }

    private boolean cubeIntersectsSphere(Vector min, Vector max, Vector sphere, double radius)
    {
        double d = square(radius);

        if (sphere.getX() < min.getX())
        {
            d -= square(sphere.getX() - min.getX());
        }
        else if (sphere.getX() > max.getX())
        {
            d -= square(sphere.getX() - max.getX());
        }
        if (sphere.getY() < min.getY())
        {
            d -= square(sphere.getY() - min.getY());
        }
        else if (sphere.getY() > max.getY())
        {
            d -= square(sphere.getY() - max.getY());
        }
        if (sphere.getZ() < min.getZ())
        {
            d -= square(sphere.getZ() - min.getZ());
        }
        else if (sphere.getZ() > max.getZ())
        {
            d -= square(sphere.getZ() - max.getZ());
        }

        return d > 0;
    }

    private double square(double v)
    {
        return v * v;
    }

    public void addProtectedArea(String label, Location location, double radius)
    {
        areas.put(label.toLowerCase(), new ProtectedRegion(location, radius));
        save();
    }

    public void removeProtectedArea(String label)
    {
        areas.remove(label.toLowerCase());
        save();
    }

    public void clearProtectedAreas()
    {
        clearProtectedAreas(true);
    }

    public void clearProtectedAreas(boolean createSpawnpointProtectedAreas)
    {
        areas.clear();

        if (createSpawnpointProtectedAreas)
        {
            autoAddSpawnpoints();
        }

        save();
    }

    public void cleanProtectedAreas()
    {
        boolean doSave = false;

        final Iterator<Map.Entry<String, ProtectedRegion>> it = areas.entrySet().iterator();

        while (it.hasNext())
        {
            try
            {
                it.next().getValue().getLocation();
            }
            catch (ProtectedRegion.CantFindWorldException ex)
            {
                it.remove();
                doSave = true;
            }
        }

        if (doSave)
        {
            save();
        }
    }

    public Set<String> getProtectedAreaLabels()
    {
        return areas.keySet();
    }

    public void autoAddSpawnpoints()
    {
        if (!ConfigEntry.PROTECTAREA_ENABLED.getBoolean())
        {
            return;
        }

        if (ConfigEntry.PROTECTAREA_SPAWNPOINTS.getBoolean())
        {
            for (World world : Bukkit.getWorlds())
            {
                String spawnLabel = "spawn_" + world.getName();
                removeProtectedArea(spawnLabel);
                addProtectedArea(spawnLabel, world.getSpawnLocation(), ConfigEntry.PROTECTAREA_RADIUS.getDouble());
            }
        }
    }

    public static class ProtectedRegion
    {
        private final double x, y, z;
        private final double radius;
        private final String worldName;
        private final UUID worldUUID;
        private transient Location location = null;

        public ProtectedRegion(final Location location, final double radius)
        {
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.radius = radius;
            this.worldName = location.getWorld().getName();
            this.worldUUID = location.getWorld().getUID();
            this.location = location;
        }

        public ProtectedRegion(double x, double y, double z, double radius, String worldName, UUID worldUUID)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.worldName = worldName;
            this.worldUUID = worldUUID;
        }

        public Location getLocation() throws CantFindWorldException
        {
            if (this.location == null)
            {
                World world = null;
                
                if (this.worldUUID != null)
                {
                    world = Bukkit.getWorld(this.worldUUID);
                }

                if (world == null && this.worldName != null)
                {
                    world = Bukkit.getWorld(this.worldName);
                }

                if (world == null)
                {
                    throw new CantFindWorldException("Can't find world " + this.worldName + ", UUID: " + this.worldUUID);
                }

                location = new Location(world, x, y, z);
            }
            return this.location;
        }

        public double getRadius()
        {
            return radius;
        }

        public static class CantFindWorldException extends Exception
        {
            private static final long serialVersionUID = 1L;

            public CantFindWorldException(String string)
            {
                super(string);
            }
        }
    }

    // Legacy class for reading old .dat files during migration
    private static class SerializableProtectedRegion implements Serializable
    {
        private static final long serialVersionUID = 213123517828282L;
        final double x, y, z;
        final double radius;
        final String worldName;
        final UUID worldUUID;

        private SerializableProtectedRegion()
        {
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.radius = 0;
            this.worldName = null;
            this.worldUUID = null;
        }
    }

}
