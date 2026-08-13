package com.magmaguy.betterstructures.buildingfitter.util;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.WeighedProbability;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SchematicPicker {
    private static boolean warnedNullPick = false;

    public static SchematicContainer pick(Location naiveAnchorLocation, GeneratorConfigFields.StructureType structureType) {
        List<SchematicContainer> schematicContainers = new ArrayList<>(SchematicContainer.getSchematics().get(structureType));
        if (schematicContainers.isEmpty()) return null;
        String worldName = naiveAnchorLocation.getWorld().getName();
        World.Environment environment = naiveAnchorLocation.getWorld().getEnvironment();
        Object biome = naiveAnchorLocation.getBlock().getBiome();
        int blockY = naiveAnchorLocation.getBlockY();
        schematicContainers.removeIf(schematicContainer ->
                !schematicContainer.isValidWorld(worldName) ||
                        !schematicContainer.isValidEnvironment(environment) ||
                        !schematicContainer.isValidBiome(biome) ||
                        !schematicContainer.isValidYLevel(blockY));
        if (schematicContainers.isEmpty()) return null;
        HashMap<Integer, Double> probabilities = new HashMap<>();
        for (int i = 0; i < schematicContainers.size(); i++)
            probabilities.put(i, schematicContainers.get(i).getSchematicConfigField().getWeight());
        Integer pickedIndex = WeighedProbability.pickWeightedProbability(probabilities);
        if (pickedIndex == null) {
            //Only possible with broken weights (all zero/negative); guard instead of NPE-ing
            if (!warnedNullPick) {
                warnedNullPick = true;
                Logger.warn("Could not pick a schematic for structure type " + structureType + " ! Check that the schematic configuration weights are positive numbers. This will only be warned about once.");
            }
            return null;
        }
        return schematicContainers.get(pickedIndex);
    }
}
