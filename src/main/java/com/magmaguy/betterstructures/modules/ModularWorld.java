package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.api.WorldGenerationFinishEvent;
import com.magmaguy.betterstructures.config.spawnpools.SpawnPoolsConfig;
import com.magmaguy.betterstructures.config.spawnpools.SpawnPoolsConfigFields;
import com.magmaguy.betterstructures.util.SchematicFileUtils;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.magmaguy.betterstructures.worldedit.SchematicClipboardCache;
import com.magmaguy.betterstructures.worldedit.SchematicConversionLog;
import com.magmaguy.betterstructures.worldedit.SchematicDiskCache;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfig;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfigFields;
import com.magmaguy.elitemobs.mobconstructor.custombosses.CustomBossEntity;
import com.magmaguy.elitemobs.mobconstructor.custombosses.InstancedBossEntity;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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
    private final World world;
    private final Location center;
    @Getter
    private final double horizontalSize;

    public ModularWorld(World world, File worldFolder, List<ModulePasting.InterpretedSign> interpretedSigns,
                        Location center, double horizontalSize) {
        this.world = Objects.requireNonNull(world, "world");
        Objects.requireNonNull(center, "center");
        if (!world.equals(center.getWorld()) || !Double.isFinite(center.getX()) || !Double.isFinite(center.getY())
                || !Double.isFinite(center.getZ())) throw new IllegalArgumentException("The center must be a finite location in the modular world");
        if (!Double.isFinite(horizontalSize) || horizontalSize <= 0)
            throw new IllegalArgumentException("The modular world's horizontal size must be positive and finite");
        this.center = center.clone();
        this.horizontalSize = horizontalSize;
        this.worldFolder = worldFolder;
        interpretedSigns.forEach(this::addSign);
    }

    public Location getCenter() { return center.clone(); }

    /** Square bands share the generated footprint; exact boundaries belong to the less difficult band. */
    public String getDifficultyId(Location location) {
        Objects.requireNonNull(location, "location");
        if (!world.equals(location.getWorld()) || !Double.isFinite(location.getX()) || !Double.isFinite(location.getZ()))
            throw new IllegalArgumentException("Difficulty requires a finite location in the modular world");
        double distance = Math.max(Math.abs(location.getX() - center.getX()), Math.abs(location.getZ() - center.getZ()));
        if (distance < horizontalSize / 6) return "2";
        if (distance < horizontalSize / 3) return "1";
        return "0";
    }

    void addSign(ModulePasting.InterpretedSign interpretedSign) {
        for (String signText : interpretedSign.text()) {
            if (signText.contains("[spawn]"))
                spawnLocations.add(ModulePasting.entitySpawnLocation(interpretedSign.location()));
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

    public static String extractPoolText(String input) {
        return SpawnPoolsConfig.extractPoolName(input);
    }

    private void processExitLocations(ModulePasting.InterpretedSign interpretedSign) {
        if (interpretedSign.text().size() < 3
                || interpretedSign.text().get(1).isBlank()
                || interpretedSign.text().get(2).isBlank()) {
            Logger.warn("Failed to get both exit clipboard filenames from sign " + interpretedSign.location());
            exitLocations.add(new ExitLocation(new Location(world,
                    (int) interpretedSign.location().getX(),
                    (int) interpretedSign.location().getY(),
                    (int) interpretedSign.location().getZ()),
                    "genericelevator_up",
                    "genericelevator_down"));
            return;
        }

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

    //Component schematics load one at a time at world-generation time, outside the startup scan,
    //so every generated world used to re-run Mojang's data converter over the same few files.
    private static final SchematicClipboardCache componentClipboardCache = new SchematicClipboardCache();

    //todo: maybe this should go into extractioncraft later
    public List<Location> spawnInaccessibleExitLocations() {
        return spawnExitLocations(exitLocation -> exitLocation.clipboardFilenameUp);
    }

    public List<Location> spawnAccessibleExitLocations() {
        return spawnExitLocations(exitLocation -> exitLocation.clipboardFilenameDown);
    }

    private List<Location> spawnExitLocations(Function<ExitLocation, String> componentFilename) {
        List<Location> randomizedLocations = new ArrayList<>();
        if (exitLocations.isEmpty()) return randomizedLocations;

        File componentsFolder = new File(MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath()
                + File.separatorChar + "components");
        //Pruning needs the full current component set, not just the files this world references —
        //anything the prune cannot account for is deleted.
        List<File> componentFiles = new ArrayList<>();
        SchematicFileUtils.scanDirectoryForSchematics(componentsFolder, componentFiles);
        int forgotten = componentClipboardCache.retainOnly(componentFiles);
        SchematicDiskCache diskCache =
                new SchematicDiskCache(SchematicDiskCache.componentCacheFolder());

        // Conversion-log capture: these component schematics load fresh at
        // world-generation time, outside the startup scan, so without a session
        // here they were the one remaining path spamming raw DataFixerUpper
        // errors for legacy block entries.
        try (SchematicConversionLog.Session conversionLog = SchematicConversionLog.capture()) {
            for (ExitLocation exitLocation : exitLocations) {
                File exitLocationsFile = new File(componentsFolder,
                        componentFilename.apply(exitLocation) + ".schem");
                if (!exitLocationsFile.exists()) {
                    Logger.warn("Failed to find elevator file");
                    continue;
                }
                randomizedLocations.add(exitLocation.location);
                Schematic.paste(loadComponent(exitLocationsFile, diskCache), exitLocation.location);
            }
        }
        if (diskCache.filesRead() > 0 || forgotten > 0)
            diskCache.pruneStaleEntries(componentFiles);
        return randomizedLocations;
    }

    private static Clipboard loadComponent(File componentFile, SchematicDiskCache diskCache) {
        Clipboard clipboard = componentClipboardCache.get(componentFile, diskCache);
        if (clipboard != null) return clipboard;
        SchematicDiskCache.LoadedClipboard loaded = diskCache.loadWithIdentity(componentFile);
        clipboard = loaded == null ? null : loaded.clipboard();
        if (loaded != null) componentClipboardCache.put(componentFile, loaded);
        return clipboard;
    }

    public void spawnOtherEntities() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ModulePasting.InterpretedSign otherLocation : List.copyOf(otherLocations))
                    spawnOtherEntitiesAt(otherLocation);
                //got to keep the memory clear for this one, unfortunately
                otherLocations.clear();
                generationFinished();
            }
        }.runTask(MetadataHandler.PLUGIN);
    }

    void spawnOtherEntitiesAt(ModulePasting.InterpretedSign otherLocation) {
        for (String string : otherLocation.text())
            if (string.contains("pool")) {
                String parsedString = extractPoolText(string) + ".yml";
                SpawnPoolsConfigFields spawnPoolsConfigFields = SpawnPoolsConfig.getConfigFields(parsedString);
                if (spawnPoolsConfigFields == null) {
                    Logger.warn("Could not find spawn pool " + parsedString);
                    continue;
                }
                String bossFilename = SpawnPoolsConfig.pickBossFilename(spawnPoolsConfigFields);
                if (bossFilename == null) {
                    Logger.warn("Spawn pool " + parsedString + " has no entries");
                    continue;
                }
                CustomBossesConfigFields customBossesConfigFields = CustomBossesConfig.getCustomBoss(bossFilename);
                if (customBossesConfigFields == null) {
                    Logger.warn("Spawn pool " + parsedString + " references missing boss " + bossFilename);
                    continue;
                }
                Location spawnLocation = ModulePasting.entitySpawnLocation(otherLocation.location());
                if (!customBossesConfigFields.isInstanced()) {
                    CustomBossEntity customBossEntity = new CustomBossEntity(customBossesConfigFields);
                    customBossEntity.spawn(spawnLocation, true);
                } else {
                    scheduledInstancedEntities.add(new ScheduledInstancedEntity(spawnLocation, customBossesConfigFields,
                            parsedString, spawnPoolsConfigFields.getMaxLevel()));
                }
            }
        otherLocations.remove(otherLocation);
    }

    public List<InstancedBossEntity> spawnInstancedEntities() {
        List<InstancedBossEntity> instancedBossEntities = new ArrayList<>();
        for (ScheduledInstancedEntity scheduledInstancedEntity : scheduledInstancedEntities) {
            InstancedBossEntity instancedBossEntity = new InstancedBossEntity(scheduledInstancedEntity.configFields,
                    scheduledInstancedEntity.location, scheduledInstancedEntity.level, getDifficultyId(scheduledInstancedEntity.location));
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
            int level
    ){}
}
