package com.magmaguy.betterstructures.thirdparty;

import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.util.InfoMessage;
import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import com.magmaguy.elitemobs.mobconstructor.custombosses.CustomBossEntity;
import com.magmaguy.elitemobs.mobconstructor.custombosses.RegionalBossEntity;
import com.magmaguy.elitemobs.utils.WarningMessage;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class WorldGuard implements Listener {
    private static StateFlag BETTERSTRUCTURES_PROTECTED = null;
    private static final StateFlag.State allow = StateFlag.State.ALLOW;
    private static final StateFlag.State deny = StateFlag.State.DENY;


    public static void initializeFlag() {
        //Enable WorldGuard
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null)
            return;

        FlagRegistry registry = null;
        try {
            registry = com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry();
        } catch (Exception ex) {
            new WarningMessage("Something went wrong while loading WorldGuard. Are you using the right WorldGuard version?");
            return;
        }

        if (BETTERSTRUCTURES_PROTECTED != null) {
            new InfoMessage("Flag betterstructures-protect is already registered, this is normal if the plugin or server have just been reloaded.");
            return;
        }

        Bukkit.getLogger().info("[BetterStructures] Enabling flags:");
        try {
            BETTERSTRUCTURES_PROTECTED = new StateFlag("betterstructures-protect", false);
            registry.register(BETTERSTRUCTURES_PROTECTED);
            Bukkit.getLogger().info("[BetteStructures] - betterstructures-protect");
        } catch (FlagConflictException | IllegalStateException e) {
            //e.printStackTrace();
            Bukkit.getLogger().warning("[EliteMobs] Warning: flag betterstructures-protect already exists! This is normal if you've just now reloaded BetterStructures.");
            BETTERSTRUCTURES_PROTECTED = (StateFlag) registry.get("betterstructures-protect");
        }
    }

    public static void Protect(BlockVector3 corner1, BlockVector3 corner2, String bossFilename, Location spawnLocation) {
        RegionContainer regionContainer = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(spawnLocation.getWorld()));
        BlockVector3 min = corner1;
        BlockVector3 max = corner2;
        ProtectedRegion region = new ProtectedCuboidRegion(regionIDGenerator(bossFilename, spawnLocation), min, max);
        region.setFlag(BETTERSTRUCTURES_PROTECTED, allow);
        region.setFlag(Flags.PASSTHROUGH, allow);
        regionManager.addRegion(region);
    }

    public static void Unprotect(CustomBossEntity customBossEntity) {
        if (!customBossEntity.getCustomBossesConfigFields().isRemoveAfterDeath()) return;
        ProtectedRegion protectedRegion = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(customBossEntity.getLocation().getWorld())).getRegion(
                        regionIDGenerator(
                                customBossEntity.getCustomBossesConfigFields().getFilename(),
                                customBossEntity.getSpawnLocation()));
        if (protectedRegion == null) return;
        com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(customBossEntity.getLocation().getWorld())).removeRegion(regionIDGenerator(
                        customBossEntity.getCustomBossesConfigFields().getFilename(),
                        customBossEntity.getSpawnLocation()));
    }

    public static boolean checkArea(Location location, Player player) {
        com.sk89q.worldedit.util.Location wgLocation = BukkitAdapter.adapt(location);
        RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(wgLocation);
        if (set.testState(null, BETTERSTRUCTURES_PROTECTED)) {
            player.sendMessage(DefaultConfig.getRegionProtectedMessage());
            return true;
        }
        return false;
    }

    private static String regionIDGenerator(String bossFilename, Location spawnLocation) {
        return "betterstructures_autoprotected_" + bossFilename.replace(".yml", "") + "_" + spawnLocation.getBlockX() + "_" + spawnLocation.getBlockY() + "_" + spawnLocation.getBlockZ();
    }

    @EventHandler
    public void onEliteDeath(EliteMobDeathEvent event) {
        if (event.getEliteEntity() instanceof RegionalBossEntity) Unprotect((CustomBossEntity) event.getEliteEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (checkArea(event.getBlock().getLocation(), event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (checkArea(event.getBlock().getLocation(), event.getPlayer())) event.setCancelled(true);
    }
}
