package com.magmaguy.betterstructures.config.generators;

import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.thirdparty.CustomBiomeCompatibility;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.util.*;

public class GeneratorConfigFields extends CustomConfigFields {

    @Getter
    @Setter
    private List<StructureType> structureTypes = new ArrayList<>(List.of(StructureType.UNDEFINED));
    @Getter
    @Setter
    private int lowestYLevel = -59;
    @Getter
    @Setter
    private int highestYLevel = 320;
    @Getter
    @Setter
    private List<String> validWorlds = null;
    @Getter
    @Setter
    private List<World.Environment> validWorldEnvironments = null;
    @Getter
    @Setter
    private List<String> validBiomesStrings = new ArrayList<>();
    @Getter
    @Setter
    private List<String> validBiomesNamespaces = new ArrayList<>();
    @Getter
    @Setter
    private String treasureFilename = null;
    @Getter
    private ChestContents chestContents = null;

    /**
     * Used by plugin-generated files (defaults)
     *
     * @param filename The config filename
     * @param isEnabled Whether the generator is enabled by default
     */
    public GeneratorConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    /**
     * Used by plugin-generated files (defaults) with structure types
     *
     * @param filename The config filename
     * @param isEnabled Whether the generator is enabled by default
     * @param structureTypes List of structure types
     */
    public GeneratorConfigFields(String filename, boolean isEnabled, List<StructureType> structureTypes) {
        super(filename, isEnabled);
        this.structureTypes = structureTypes;
    }

    @Override
    public void processConfigFields() {
        this.isEnabled = processBoolean("isEnabled", isEnabled, true, true);
        this.structureTypes = processEnumList("structureType", structureTypes, List.of(StructureType.UNDEFINED), StructureType.class, true);
        this.lowestYLevel = processInt("lowestYLevel", lowestYLevel, -59, false);
        this.highestYLevel = processInt("highestYLevel", highestYLevel, 320, false);
        this.validWorlds = processStringList("validWorlds", validWorlds, new ArrayList<>(), false);
        this.validWorldEnvironments = processEnumList("validWorldEnvironments", validWorldEnvironments, null, World.Environment.class, false);

        // Process biomes
        processBiomes();

        // Load treasure config
        this.treasureFilename = processString("treasureFilename", treasureFilename, null, false);
        TreasureConfigFields treasureConfig = TreasureConfig.getConfigFields(treasureFilename);
        if (treasureConfig != null) {
            this.chestContents = new ChestContents(treasureConfig);
        } else {
            Logger.warn("No valid treasure config file found for generator " + filename + " ! This will not spawn loot in chests until fixed.");
        }
    }

    /**
     * Processes biome configuration and populates validBiomesNamespaces list.
     * This method ensures all custom biome mappings are properly applied.
     */
    private void processBiomes() {
        // Initialize or clear the namespaces list
        if (validBiomesNamespaces == null) {
            validBiomesNamespaces = new ArrayList<>();
        } else {
            validBiomesNamespaces.clear();
        }

        // Read biomes from config or use defaults
        if (fileConfiguration.contains("validBiomesV2") &&
                !fileConfiguration.getList("validBiomesV2", new ArrayList<>()).isEmpty()) {

            // Read biomes from config
            this.validBiomesStrings = processStringList("validBiomesV2", validBiomesStrings, validBiomesStrings, false);
        }

        // Process biomes and their custom variants
        Set<String> processedBiomes = new HashSet<>();

        // First pass: standardize all biome formats and collect default biomes
        List<String> standardizedBiomes = new ArrayList<>();
        for (String biomeString : validBiomesStrings) {
            String standardizedBiome = standardizeBiomeFormat(biomeString);
            if (standardizedBiome != null) {
                standardizedBiomes.add(standardizedBiome);
                processedBiomes.add(standardizedBiome);
            }
        }

        // Add all standard biomes to the namespaces list
        validBiomesNamespaces.addAll(standardizedBiomes);

        // Second pass: collect all custom biomes that map to our default biomes
        List<String> customBiomes = new ArrayList<>();
        for (String standardizedBiome : standardizedBiomes) {
            // Skip non-minecraft biomes (they're already custom)
            if (!standardizedBiome.startsWith("minecraft:")) {
                continue;
            }

            // Add custom biomes that map to this default biome
            List<String> mappedCustomBiomes = CustomBiomeCompatibility.getCustomBiomes(standardizedBiome);
            for (String customBiome : mappedCustomBiomes) {
                if (!processedBiomes.contains(customBiome)) {
                    customBiomes.add(customBiome);
                    processedBiomes.add(customBiome);
                }
            }
        }

        // Add all custom biomes to both lists
        validBiomesNamespaces.addAll(customBiomes);

        // If we're creating a new config or updating an existing one, save the full list
        if (customBiomes.size() > 0) {
            List<String> fullBiomeList = new ArrayList<>(validBiomesStrings);
            fullBiomeList.addAll(customBiomes);
            validBiomesStrings = fullBiomeList;
            fileConfiguration.set("validBiomesV2", fullBiomeList);
        }

        // Debug output
//        if (!validBiomesNamespaces.isEmpty()) {
//            Logger.debug("Valid biomes for " + filename + ":");
//            for (String biome : validBiomesNamespaces) {
//                Logger.debug(" - " + biome);
//            }
//        }
    }

    /**
     * Standardizes biome format to namespace:key format.
     *
     * @param biomeString The biome string to standardize
     * @return The standardized biome string in namespace:key format, or null if invalid
     */
    private String standardizeBiomeFormat(String biomeString) {
        if (biomeString == null || biomeString.isEmpty()) {
            return null;
        }

        // If already in namespace:key format, return as is (ensuring lowercase)
        if (biomeString.contains(":")) {
            return biomeString.toLowerCase(Locale.ROOT);
        }

        // Handle vanilla biomes (convert from enum name to namespace:key format)
        try {
            Biome biome = Biome.valueOf(biomeString.toUpperCase(Locale.ROOT));
            return "minecraft:" + biome.getKey().getKey();
        } catch (IllegalArgumentException e) {
            Logger.warn("Invalid biome name: " + biomeString);
            return null;
        }
    }

    public enum StructureType {
        UNDEFINED,
        UNDERGROUND_DEEP,
        UNDERGROUND_SHALLOW,
        SURFACE,
        SKY,
        LIQUID_SURFACE,
        DUNGEON
    }
}