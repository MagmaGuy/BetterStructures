package com.magmaguy.betterstructures.schematics;

import com.google.common.collect.ArrayListMultimap;
import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.config.schematics.SchematicConfigField;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.WorldEditUtils;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SchematicContainer {
    @Getter
    private static final ArrayListMultimap<GeneratorConfigFields.StructureType, SchematicContainer> schematics = ArrayListMultimap.create();
    private static final Map<Object, String> BIOME_ID_CACHE = new ConcurrentHashMap<>();
    private static volatile boolean eliteMobsAvailable;
    private static volatile boolean mythicMobsAvailable;
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
    private ChestContents chestContents = null;
    @Getter
    private ChestContents barrelContents = null;
    private boolean valid = true;

    public SchematicContainer(Clipboard clipboard, String clipboardFilename, SchematicConfigField schematicConfigField, String configFilename) {
        this.clipboard = clipboard;
        this.clipboardFilename = clipboardFilename;
        this.schematicConfigField = schematicConfigField;
        this.configFilename = configFilename;
        generatorConfigFields = schematicConfigField.getGeneratorConfigFields();
        if (generatorConfigFields == null) {
            Logger.warn("Failed to assign generator for configuration of schematic " + schematicConfigField.getFilename() + " ! This means this structure will not appear in the world.");
            return;
        }
        if (!generatorConfigFields.isEnabled()) {
            valid = false;
            return;
        }
        if (clipboard == null) {
            Logger.warn("Failed to find schematic " + clipboardFilename + " for configuration " + configFilename + "; this structure will not be used.");
            valid = false;
            return;
        }
        for (int x = 0; x < clipboard.getDimensions().x(); x++)
            for (int y = 0; y < clipboard.getDimensions().y(); y++)
                for (int z = 0; z < clipboard.getDimensions().z(); z++) {
                    BlockVector3 translatedLocation = BlockVector3.at(x, y, z).add(clipboard.getMinimumPoint());
                    BlockState weBlockState = clipboard.getBlock(translatedLocation);
                    Material minecraftMaterial = WorldEditUtils.adaptMaterial(weBlockState);
                    if (minecraftMaterial == null) continue;
                    //register chest location
                    if (minecraftMaterial.equals(Material.CHEST) ||
                            minecraftMaterial.equals(Material.TRAPPED_CHEST) ||
                            minecraftMaterial.equals(Material.SHULKER_BOX) ||
                            minecraftMaterial.equals(Material.BARREL)) {
                        chestLocations.add(new Vector(x, y, z));
                    }
                    if (isSign(minecraftMaterial)) {
                        BaseBlock baseBlock = clipboard.getFullBlock(translatedLocation);
                        //For future reference, I don't know how to get the data in any other way than parsing the string. Sorry!
                        String line1 = WorldEditUtils.getLine(baseBlock, 1);
                        if (line1 == null || line1.isBlank()) continue;

                        //Case for spawning a vanilla mob
                        if (line1.toLowerCase(Locale.ROOT).contains("[spawn]")) {
                            String rawLine2 = WorldEditUtils.getLine(baseBlock, 2);
                            if (rawLine2 == null || rawLine2.isBlank()) {
                                Logger.warn("Missing entity type for spawn sign in schematic " + clipboardFilename);
                                continue;
                            }
                            String line2 = rawLine2.toUpperCase(Locale.ROOT).replace("\"", "");
                            EntityType entityType;
                            try {
                                entityType = EntityType.valueOf(line2);
                            } catch (Exception ex) {
                                if (line2.equalsIgnoreCase("WITHER_CRYSTAL"))
                                    entityType = EntityType.END_CRYSTAL;
                                else {
                                    Logger.warn("Failed to determine entity type for sign! Entry was " + line2 + " in schematic " + clipboardFilename + " ! Fix this by inputting a valid entity type!");
                                    continue;
                                }
                            }
                            vanillaSpawns.put(new Vector(x, y, z), entityType);
                        } else if (line1.toLowerCase(Locale.ROOT).contains("[elitemobs]")) {
                            if (!eliteMobsAvailable) {
                                Logger.warn(configFilename + " uses EliteMobs bosses but EliteMobs is not installed; this schematic will not be used.");
                                valid = false;
                                return;
                            }
                            String filename = "";
                            for (int i = 2; i < 5; i++) filename += WorldEditUtils.getLine(baseBlock, i);
                            eliteMobsSpawns.put(new Vector(x, y, z), filename);
                        } else if (line1.toLowerCase(Locale.ROOT).contains("[mythicmobs]")) { // carm start - Support MythicMobs
                            if (!mythicMobsAvailable) {
                                Logger.warn(configFilename + " uses MythicMobs bosses but MythicMobs is not installed; this schematic will not be used.");
                                valid = false;
                                return;
                            }
                            String mob = WorldEditUtils.getLine(baseBlock, 2);
                            String level = WorldEditUtils.getLine(baseBlock, 3);
                            if (mob == null || mob.isBlank()) {
                                Logger.warn("Missing MythicMobs entity ID for spawn sign in schematic "
                                        + clipboardFilename);
                                continue;
                            }
                            mythicMobsSpawns.put(
                                    new Vector(x, y, z),
                                    mob + (level == null || level.isBlank()
                                            ? ""
                                            : ":" + level));
                        } // carm end - Support MythicMobs
                    }
                }
        chestContents = generatorConfigFields.getChestContents();
        barrelContents = generatorConfigFields.getBarrelContents();
        if (schematicConfigField.getTreasureFile() != null && !schematicConfigField.getTreasureFile().isEmpty()) {
            TreasureConfigFields treasureConfigFields = TreasureConfig.getConfigFields(schematicConfigField.getTreasureFile());
            if (treasureConfigFields == null) {
                Logger.warn("Failed to get treasure configuration " + schematicConfigField.getTreasureFile() + " for schematic " + schematicConfigField.getFilename() + " ! Defaulting to the generator chest treasure.");
            } else {
                chestContents = schematicConfigField.getChestContents();
            }
        }
        if (schematicConfigField.getBarrelTreasureFilename() != null && !schematicConfigField.getBarrelTreasureFilename().isEmpty()) {
            TreasureConfigFields barrelTreasureConfigFields = TreasureConfig.getConfigFields(schematicConfigField.getBarrelTreasureFilename());
            if (barrelTreasureConfigFields == null) {
                Logger.warn("Failed to get barrel treasure configuration " + schematicConfigField.getBarrelTreasureFilename() + " for schematic " + schematicConfigField.getFilename() + " ! Defaulting to the generator barrel treasure.");
            } else {
                barrelContents = schematicConfigField.getBarrelContents();
            }
        }
        if (valid)
            generatorConfigFields.getStructureTypes().forEach(structureType -> schematics.put(structureType, this));
    }

    private static boolean isSign(Material material) {
        return material.name().endsWith("_SIGN") || material.name().endsWith("_WALL_SIGN");
    }

    public static void shutdown() {
        schematics.clear();
        BIOME_ID_CACHE.clear();
    }

    public static void updateOptionalPluginAvailability(boolean eliteMobs, boolean mythicMobs) {
        eliteMobsAvailable = eliteMobs;
        mythicMobsAvailable = mythicMobs;
    }

    public boolean isValidEnvironment(World.Environment environment) {
        return generatorConfigFields.getValidWorldEnvironments() == null ||
                generatorConfigFields.getValidWorldEnvironments().isEmpty() ||
                generatorConfigFields.getValidWorldEnvironments().contains(environment);
    }

    /**
     * Validates if a biome is in the list of valid biomes, handling both newer interface-based
     * biomes and older class-based biomes.
     *
     * @param biomeObj The biome to validate
     * @return True if the biome is valid, false otherwise
     */
    public boolean isValidBiome(Object biomeObj) {
        if (generatorConfigFields.getValidBiomesNamespaces() == null) return true;
        if (generatorConfigFields.getValidBiomesNamespaces().isEmpty()) return true;

        // Extract biome identifier based on version
        String biomeString = getBiomeIdentifier(biomeObj);

        for (String validBiome : generatorConfigFields.getValidBiomesNamespaces()) {
            if (biomeString.equals(validBiome)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets a string identifier for a biome that works across different Minecraft versions.
     * Handles both interface (newer) and class (older) implementation of Biome.
     *
     * @param biomeObj The biome to get an identifier for (passed as Object to avoid class casting issues)
     * @return A string identifier for the biome
     */
    private static String getBiomeIdentifier(Object biomeObj) {
        return BIOME_ID_CACHE.computeIfAbsent(biomeObj, SchematicContainer::computeBiomeIdentifier);
    }

    private static String computeBiomeIdentifier(Object biomeObj) {
        // First, try to use reflection to safely handle both class and interface versions
        try {
            // Try to get the getKey method (newer versions)
            java.lang.reflect.Method getKeyMethod = biomeObj.getClass().getMethod("getKey");
            Object key = getKeyMethod.invoke(biomeObj);

            // Get namespace and key from the NamespacedKey
            java.lang.reflect.Method getNamespaceMethod = key.getClass().getMethod("getNamespace");
            java.lang.reflect.Method getKeyNameMethod = key.getClass().getMethod("getKey");

            String namespace = (String) getNamespaceMethod.invoke(key);
            String keyName = (String) getKeyNameMethod.invoke(key);

            return namespace + ":" + keyName;
        } catch (Exception e) {
            // Older versions may use different methods or be enums
            try {
                // If it's an enum, try to get the name
                if (biomeObj.getClass().isEnum()) {
                    String enumName = ((Enum<?>) biomeObj).name().toLowerCase();
                    return "minecraft:" + enumName;
                }

                // Try name() method which might exist in some implementations
                java.lang.reflect.Method nameMethod = biomeObj.getClass().getMethod("name");
                String name = (String) nameMethod.invoke(biomeObj);
                return "minecraft:" + name.toLowerCase();
            } catch (Exception e2) {
                // Last resort - use toString and clean it up
                String fallback = biomeObj.toString();

                // Try to extract the name from common toString() formats
                if (fallback.contains("{") && fallback.contains("}")) {
                    // Handle patterns like "Biome{name=DESERT}"
                    int startIndex = fallback.indexOf("=") + 1;
                    int endIndex = fallback.indexOf("}", startIndex);
                    if (startIndex > 0 && endIndex > startIndex) {
                        fallback = fallback.substring(startIndex, endIndex);
                    }
                } else if (fallback.contains(".")) {
                    // Handle patterns like "ENUM.DESERT"
                    fallback = fallback.substring(fallback.lastIndexOf(".") + 1);
                }

                // Clean up and return with default namespace
                return "minecraft:" + fallback.toLowerCase().trim();
            }
        }
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
