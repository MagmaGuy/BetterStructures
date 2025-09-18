package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.api.WorldGenerationFinishEvent;
import com.magmaguy.betterstructures.config.spawnpools.SpawnPoolsConfig;
import com.magmaguy.betterstructures.config.spawnpools.SpawnPoolsConfigFields;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfig;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfigFields;
import com.magmaguy.elitemobs.mobconstructor.custombosses.CustomBossEntity;
import com.magmaguy.elitemobs.mobconstructor.custombosses.InstancedBossEntity;
import com.magmaguy.magmacore.instance.MatchInstance;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector2i;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModularWorld {

    @Getter
    private final List<Location> spawnLocations = new ArrayList<>();
    @Getter
    private final List<ExitLocation> exitLocations = new ArrayList<>();
    @Getter
    private final List<Location> chestLocations = new ArrayList<>();
    @Getter
    private final List<Location> barrelLocations = new ArrayList<>();
    private final HashSet<ModulePasting.InterpretedSign> otherLocations = new HashSet<>();
    private final List<ScheduledInstancedEntity> scheduledInstancedEntities = new ArrayList<>();
    @Getter
    private final File worldFolder;
    @Getter
    private World world = null;

    public ModularWorld(World world, File worldFolder, List<ModulePasting.InterpretedSign> interpretedSigns) {
        this.world = world;
        this.worldFolder = worldFolder;
        for (ModulePasting.InterpretedSign interpretedSign : interpretedSigns) {
            for (String signText : interpretedSign.text()) {
                if (signText.contains("[spawn]"))
                    spawnLocations.add(new Location(world,
                            (int) interpretedSign.location().getX(),
                            (int) interpretedSign.location().getY(),
                            (int) interpretedSign.location().getZ()));
                else if (signText.contains("[exit]")) {
                    processExitLocations(interpretedSign);
                } else if (signText.contains("[chest]")) {
                    chestLocations.add(new Location(world,
                            (int) interpretedSign.location().getX(),
                            (int) interpretedSign.location().getY(),
                            (int) interpretedSign.location().getZ()));
                } else if (signText.contains("[barrel]")) {
                    barrelLocations.add(new Location(world,
                            (int) interpretedSign.location().getX(),
                            (int) interpretedSign.location().getY(),
                            (int) interpretedSign.location().getZ()));
                } else
                    otherLocations.add(interpretedSign);
            }
        }
    }

    public static String extractPoolText(String input) {
        Pattern pattern = Pattern.compile("\\[pool:\\s*([^\\]]+)\\]");
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void processExitLocations(ModulePasting.InterpretedSign interpretedSign) {
        String exitClipboardFilename = "";
        for (int i = 1; i < interpretedSign.text().size(); i++) {
            exitClipboardFilename += interpretedSign.text().get(i);
        }
        if (exitClipboardFilename.isEmpty()) {
            Logger.warn("Failed to get exit clipboard filename from sign " + interpretedSign.location());
            exitLocations.add(new ExitLocation(new Location(world,
                    (int) interpretedSign.location().getX(),
                    (int) interpretedSign.location().getY(),
                    (int) interpretedSign.location().getZ()),
                    "genericelevator_up",
                    "genericelevator_down"));
            return;
        }
//        Logger.debug("Exit location " + exitClipboardFilename + " detected");
        exitLocations.add(new ExitLocation(new Location(world,
                (int) interpretedSign.location().getX(),
                (int) interpretedSign.location().getY(),
                (int) interpretedSign.location().getZ()),
                interpretedSign.text().get(1),
                interpretedSign.text().get(2)));
    }

    public List<Block> spawnChests() {
        List<Block> chests = new ArrayList<>();
        for (Location chestLocation : chestLocations) {
            chestLocation.getBlock().setType(Material.CHEST);
            chests.add(chestLocation.getBlock());
        }
        return chests;
    }

    public List<Block> spawnBarrels() {
        List<Block> barrels = new ArrayList<>();
        for (Location barrelLocation : barrelLocations) {
            barrelLocation.getBlock().setType(Material.BARREL);
            barrels.add(barrelLocation.getBlock());
        }
        return barrels;
    }

    //todo: maybe this should go into extractioncraft later
    public List<Location> spawnInaccessibleExitLocations() {
        List<Location> randomizedLocations = new ArrayList<>();
        for (ExitLocation exitLocation : exitLocations) {
            File exitLocationsFile = new File(MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "components" + File.separatorChar + exitLocation.clipboardFilenameUp + ".schem");
            if (!exitLocationsFile.exists()) {
                Logger.warn("Failed to find elevator file");
                continue;
            }
            randomizedLocations.add(exitLocation.location);
            Schematic.paste(Schematic.load(exitLocationsFile), exitLocation.location);
        }
        return randomizedLocations;
    }

    public List<Location> spawnAccessibleExitLocations() {
        List<Location> randomizedLocations = new ArrayList<>();
        for (ExitLocation exitLocation : exitLocations) {
            File exitLocationsFile = new File(MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "components" + File.separatorChar + exitLocation.clipboardFilenameDown + ".schem");
            if (!exitLocationsFile.exists()) {
                Logger.warn("Failed to find elevator file");
                continue;
            }
            randomizedLocations.add(exitLocation.location);
            Schematic.paste(Schematic.load(exitLocationsFile), exitLocation.location);
        }
        return randomizedLocations;
    }

    public void spawnOtherEntities() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ModulePasting.InterpretedSign otherLocation : otherLocations)
                    for (String string : otherLocation.text())
                        if (string.contains("pool")) {
                            String parsedString = extractPoolText(string) + ".yml";
                            SpawnPoolsConfigFields spawnPoolsConfigFields = SpawnPoolsConfig.getConfigFields(parsedString);
                            if (spawnPoolsConfigFields == null) {
                                Logger.warn("Could not find spawn pool " + parsedString);
                                continue;
                            }
                            CustomBossesConfigFields customBossesConfigFields = CustomBossesConfig.getCustomBoss(spawnPoolsConfigFields.getPoolStrings().get(ThreadLocalRandom.current().nextInt(0, spawnPoolsConfigFields.getPoolStrings().size())));
                            if (!customBossesConfigFields.isInstanced()) {
                                CustomBossEntity customBossEntity = new CustomBossEntity(customBossesConfigFields);
                                customBossEntity.spawn(otherLocation.location(), true);
                            } else {
                                scheduledInstancedEntities.add(new ScheduledInstancedEntity(otherLocation.location(), customBossesConfigFields, parsedString, spawnPoolsConfigFields.getMinLevel(), spawnPoolsConfigFields.getMaxLevel()));
                            }
                        }
                //got to keep the memory clear for this one, unfortunately
                otherLocations.clear();
                generationFinished();
            }
        }.runTask(MetadataHandler.PLUGIN);
    }

    public List<InstancedBossEntity> spawnInstancedEntities(MatchInstance matchInstance) {
        List<InstancedBossEntity> instancedBossEntities = new ArrayList<>();
        for (ScheduledInstancedEntity scheduledInstancedEntity : scheduledInstancedEntities) {
            int totalRadius = 2 * 128 + 64;//todo this is just a placeholder for now that hardcodes the radius
            Vector2i center = new Vector2i(64, 64); //todo this is just a placeholder for now that hardcodes the center
            Vector2i entityLocation = new Vector2i(scheduledInstancedEntity.location.getBlockX(), scheduledInstancedEntity.location.getBlockZ());
            double distance = center.distance(entityLocation);
            double percentageDistance = distance / totalRadius;
            int level = (int) Math.round((1.0 - percentageDistance) * scheduledInstancedEntity.maxLevel + percentageDistance * scheduledInstancedEntity.minLevel);

            InstancedBossEntity instancedBossEntity = new InstancedBossEntity(scheduledInstancedEntity.configFields, scheduledInstancedEntity.location, matchInstance, level);
//            InstancedBossEntity instancedBossEntity = new InstancedBossEntity(scheduledInstancedEntity.configFields, scheduledInstancedEntity.location, matchInstance, 10);//todo: level is just a placeholder for now
            instancedBossEntity.spawn(true);
            instancedBossEntity.addCustomData(new NamespacedKey("betterstructures", "spawnpool"), scheduledInstancedEntity.originalSpawnPool);
            instancedBossEntities.add(instancedBossEntity);
        }
        return instancedBossEntities;
    }

    public void generationFinished() {
        Bukkit.getServer().getPluginManager().callEvent(new WorldGenerationFinishEvent(this));
    }

    private record ExitLocation(Location location, String clipboardFilenameUp, String clipboardFilenameDown) {
    }

    private record ScheduledInstancedEntity(
            Location location,
            CustomBossesConfigFields configFields,
            String originalSpawnPool,
            int minLevel,
            int maxLevel
    ){}
}
