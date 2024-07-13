package com.magmaguy.betterstructures.schematics;

import com.google.common.collect.ArrayListMultimap;
import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.config.schematics.SchematicConfigField;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.WarningMessage;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SchematicContainer {
    @Getter
    private static final ArrayListMultimap<GeneratorConfigFields.StructureType, SchematicContainer> schematics = ArrayListMultimap.create();
    @Getter
    private final Clipboard clipboard;
    @Getter
    private final SchematicConfigField schematicConfigField;
    @Getter
    private final GeneratorConfigFields generatorConfigFields;
    @Getter
    private final String clipboardFilename;
    @Getter
    private final String configFilename;
    @Getter
    private final List<Vector> chestLocations = new ArrayList<>();
    @Getter
    private final HashMap<Vector, EntityType> vanillaSpawns = new HashMap<>();
    @Getter
    private final HashMap<Vector, String> eliteMobsSpawns = new HashMap<>();
    @Getter
    private final HashMap<Vector, String> mythicMobsSpawns = new HashMap<>(); // carm - Support for MythicMobs
    @Getter
    List<AbstractBlock> abstractBlocks = new ArrayList<>();
    @Getter
    private ChestContents chestContents = null;
    @Getter
    private boolean valid = true;

    private List<Material> signs = Arrays.stream(Material.values()).parallel()
            .filter(it -> it.name().endsWith("SIGN"))
            .toList();

    public SchematicContainer(Clipboard clipboard, String clipboardFilename, SchematicConfigField schematicConfigField, String configFilename) {
        this.clipboard = clipboard;
        this.clipboardFilename = clipboardFilename;
        this.schematicConfigField = schematicConfigField;
        this.configFilename = configFilename;
        generatorConfigFields = schematicConfigField.getGeneratorConfigFields();
        if (generatorConfigFields == null) {
            new WarningMessage("Failed to assign generator for configuration of schematic " + schematicConfigField.getFilename() + " ! This means this structure will not appear in the world.");
            return;
        }
        for (int x = 0; x <= clipboard.getDimensions().getX(); x++)
            for (int y = 0; y <= clipboard.getDimensions().getY(); y++)
                for (int z = 0; z <= clipboard.getDimensions().getZ(); z++) {
                    BlockVector3 translatedLocation = BlockVector3.at(x, y, z).add(clipboard.getMinimumPoint());
                    BlockState weBlockState = clipboard.getBlock(translatedLocation);
                    Material minecraftMaterial = BukkitAdapter.adapt(weBlockState.getBlockType());
                    if (minecraftMaterial == null) continue;
                    //register chest location
                    if (minecraftMaterial.equals(Material.CHEST) ||
                            minecraftMaterial.equals(Material.TRAPPED_CHEST) ||
                            minecraftMaterial.equals(Material.SHULKER_BOX)) {
                        chestLocations.add(new Vector(x, y, z));
                    }

                    if (signs.contains(minecraftMaterial)) {
                        BaseBlock baseBlock = clipboard.getFullBlock(translatedLocation);
                        //For future reference, I don't know how to get the data in any other way than parsing the string. Sorry!
                        String line1 = WorldEditUtils.getLine(baseBlock, 1);

                        //Case for spawning a vanilla mob
                        if (line1.toLowerCase().contains("[spawn]")) {
                            String line2 = WorldEditUtils.getLine(baseBlock, 2).toUpperCase().replaceAll("\"", "");
                            EntityType entityType;
                            try {
                                entityType = EntityType.valueOf(line2);
                            } catch (Exception ex) {
                                new WarningMessage("Failed to determine entity type for sign! Entry was " + line2 + " in schematic " + clipboardFilename + " ! Fix this by inputting a valid entity type!");
                                continue;
                            }
                            vanillaSpawns.put(new Vector(x, y, z), entityType);
                        } else if (line1.toLowerCase().contains("[elitemobs]")) {
                            if (Bukkit.getPluginManager().getPlugin("EliteMobs") == null) {
                                Bukkit.getLogger().warning("[BetterStructures] " + configFilename + " uses EliteMobs bosses but you do not have EliteMobs installed! BetterStructures does not require EliteMobs to work, but if you want cool EliteMobs boss fights you will have to install EliteMobs here: https://www.spigotmc.org/resources/%E2%9A%94elitemobs%E2%9A%94.40090/");
                                Bukkit.getLogger().warning("[BetterStructures] Since EliteMobs is not installed, " + configFilename + " will not be used.");
                                valid = false;
                                return;
                            }
                            String filename = "";
                            for (int i = 2; i < 5; i++) filename += WorldEditUtils.getLine(baseBlock, i);
                            eliteMobsSpawns.put(new Vector(x, y, z), filename);
                        } else if (line1.toLowerCase().contains("[mythicmobs]")) { // carm start - Support MythicMobs
                            if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
                                Bukkit.getLogger().warning("[BetterStructures] " + configFilename + " uses MythicMobs bosses but you do not have MythicMobs installed! BetterStructures does not require MythicMobs to work, but if you want MythicMobs boss fights you will have to install MythicMobs.");
                                Bukkit.getLogger().warning("[BetterStructures] Since MythicMobs is not installed, " + configFilename + " will not be used.");
                                valid = false;
                                return;
                            }
                            String mob = WorldEditUtils.getLine(baseBlock, 2);
                            String level = WorldEditUtils.getLine(baseBlock, 3);
                            mythicMobsSpawns.put(new Vector(x, y, z), mob + (level.isEmpty() ? "" : ":" + level));
                        } // carm end - Support MythicMobs
                    }
                }
        chestContents = generatorConfigFields.getChestContents();
        if (schematicConfigField.getTreasureFile() != null && !schematicConfigField.getTreasureFile().isEmpty()) {
            TreasureConfigFields treasureConfigFields = TreasureConfig.getConfigFields(schematicConfigField.getFilename());
            if (treasureConfigFields == null) {
                new WarningMessage("Failed to get treasure configuration " + schematicConfigField.getTreasureFile());
                return;
            }
            chestContents = schematicConfigField.getChestContents();
        }
        if (valid)
            generatorConfigFields.getStructureTypes().forEach(structureType -> schematics.put(structureType, this));
    }

    public static void shutdown() {
        schematics.clear();
    }

    public boolean isValidEnvironment(World.Environment environment) {
        return generatorConfigFields.getValidWorldEnvironments() == null ||
                generatorConfigFields.getValidWorldEnvironments().isEmpty() ||
                generatorConfigFields.getValidWorldEnvironments().contains(environment);
    }

    public boolean isValidBiome(Biome biome) {
        return generatorConfigFields.getValidBiomes() == null ||
                generatorConfigFields.getValidBiomes().isEmpty() ||
                generatorConfigFields.getValidBiomes().contains(biome);
    }

    public boolean isValidYLevel(int yLevel) {
        return generatorConfigFields.getLowestYLevel() <= yLevel && generatorConfigFields.getHighestYLevel() >= yLevel;
    }

    public boolean isValidWorld(String worldName) {
        return generatorConfigFields.getValidWorlds() == null ||
                generatorConfigFields.getValidWorlds().isEmpty() ||
                generatorConfigFields.getValidWorlds().contains(worldName);
    }
}
