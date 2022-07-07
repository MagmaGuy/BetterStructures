package com.magmaguy.betterstructures.config.generators;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.CustomConfigFields;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeneratorConfigFields extends CustomConfigFields {

    public enum StructureType {
        UNDEFINED,
        UNDERGROUND_DEEP,
        UNDERGROUND_SHALLOW,
        SURFACE,
        SKY,
        LIQUID_SURFACE
    }

    @Getter
    @Setter
    private List<StructureType> structureTypes = new ArrayList<>(Arrays.asList(StructureType.UNDEFINED));
    @Getter
    @Setter
    private int lowestYLevel = -59;
    @Getter
    @Setter
    private int highestYLevel = 320;
    @Getter
    @Setter
    private List<World> validWorlds = new ArrayList<>();
    @Getter
    @Setter
    private List<World.Environment> validWorldEnvironments = new ArrayList<>();
    @Getter
    @Setter
    private List<Biome> validBiomes = new ArrayList<>();
    @Getter
    @Setter
    List<String> chestEntries = new ArrayList<>();
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
        this.structureTypes = processEnumList("structureType", structureTypes, Arrays.asList(StructureType.UNDEFINED), StructureType.class, true);
        this.lowestYLevel = processInt("lowestYLevel", lowestYLevel, -59, false);
        this.highestYLevel = processInt("highestYLevel", highestYLevel, 320, false);
        this.validWorlds = processWorldList("validWorlds", validWorlds, new ArrayList<>(), false);
        this.validWorldEnvironments = processEnumList("validWorldEnvironments", validWorldEnvironments, null, World.Environment.class, false);
        this.validBiomes = processEnumList("validBiomes", validBiomes, new ArrayList<>(), Biome.class, false);
        this.chestEntries = processStringList("chestEntries", chestEntries, new ArrayList<>(), false);
        chestContents = new ChestContents(chestEntries, this);
    }

    public void addChestEntry(String entry, Player player) {
        chestEntries.add(entry);
        fileConfiguration.set("chestEntries", chestEntries);
        try {
            fileConfiguration.save(file);
        } catch (Exception ex) {
            player.sendMessage("[BetterStructures] Failed to save entry to file! Report this to the developer.");
            return;
        }
        MetadataHandler.PLUGIN.onDisable();
        MetadataHandler.PLUGIN.onLoad();
        MetadataHandler.PLUGIN.onEnable();
        player.sendMessage("[BetterStructures] Reloaded plugin to add chest entry! It should now be live.");
    }
}
