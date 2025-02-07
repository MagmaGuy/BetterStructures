package com.magmaguy.betterstructures.config.generators;

import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.magmacore.config.CustomConfigFields;
import com.magmaguy.magmacore.thirdparty.CustomBiomeCompatibility;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private List<Biome> validBiomes = new ArrayList<>();
    @Getter
    @Setter
    private String treasureFilename = null;
    @Getter
    private ChestContents chestContents = null;

    /**
     * Used by plugin-generated files (defaults)
     *
     * @param filename
     * @param isEnabled
     */
    public GeneratorConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }


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

        List<String> extendedDefaults = new ArrayList<>();
        if (fileConfiguration.getList("validBiomesV2") == null || fileConfiguration.getList("validBiomesV2").isEmpty()) {
            for (String validBiomesString : validBiomesStrings) {
                Biome biome;
                if (!validBiomesString.contains(":")) {
                    biome = Biome.valueOf(validBiomesString.toLowerCase(Locale.ROOT));
                } else {
                    biome = Registry.BIOME.get(new NamespacedKey(validBiomesString.split(":")[0], validBiomesString.split(":")[1].toLowerCase(Locale.ROOT)));
                }
                if (biome == null && !validBiomesString.contains("minecraft:custom")){
                    Logger.warn("Null biome for " + validBiomesString);
                    continue;
                }
                List<Biome> customBiomes = CustomBiomeCompatibility.getCustomBiomes(biome);
                if (customBiomes != null && !customBiomes.isEmpty())
                    for (Biome customBiome : customBiomes) {
                        String customBiomeString = biome.getKey().getNamespace() + ":" + customBiome.getKey().getKey();
                        extendedDefaults.add(customBiomeString);
                    }
            }
        }

        validBiomesStrings.addAll(extendedDefaults);
        fileConfiguration.addDefault("validBiomesV2", validBiomesStrings);
//        this.validBiomesStrings = processStringList("validBiomesV2", validBiomesStrings, validBiomesStrings, false);

        for (String validBiomesString : validBiomesStrings) {
            Biome biome;
            if (!validBiomesString.contains(":")) {
                biome = Biome.valueOf(validBiomesString.toLowerCase(Locale.ROOT));
            } else {
                biome = Registry.BIOME.get(new NamespacedKey(validBiomesString.split(":")[0], validBiomesString.split(":")[1].toLowerCase(Locale.ROOT)));
            }
            validBiomes.add(biome);
//            Logger.debug("Added biome " + biome.getKey().getKey() + " to valid biomes list for generator " + filename + " with namespace " + biome.getKey().getNamespace() + ".");
        }

        this.treasureFilename = processString("treasureFilename", treasureFilename, null, false);
        TreasureConfigFields treasureConfig = TreasureConfig.getConfigFields(treasureFilename);
        if (treasureConfig != null)
            this.chestContents = new ChestContents(treasureConfig);
        else
            Logger.warn("No valid treasure config file found for generator " + filename + " ! This will not spawn loot in chests until fixed.");

    }

    public enum StructureType {
        UNDEFINED,
        UNDERGROUND_DEEP,
        UNDERGROUND_SHALLOW,
        SURFACE,
        SKY,
        LIQUID_SURFACE
    }

}
