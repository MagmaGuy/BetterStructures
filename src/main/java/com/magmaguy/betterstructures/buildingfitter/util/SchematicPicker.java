package com.magmaguy.betterstructures.buildingfitter.util;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.WeighedProbability;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SchematicPicker {
    public static SchematicContainer pick(Location naiveAnchorLocation, GeneratorConfigFields.StructureType structureType) {
        List<SchematicContainer> schematicContainers = new ArrayList<>(SchematicContainer.getSchematics().get(structureType));
        if (schematicContainers.isEmpty()) return null;
        schematicContainers.removeIf(schematicContainer ->
                !schematicContainer.isValidEnvironment(naiveAnchorLocation.getWorld().getEnvironment()) ||
                        !schematicContainer.isValidBiome(naiveAnchorLocation.getBlock().getBiome()) ||
                        !schematicContainer.isValidYLevel(naiveAnchorLocation.getBlockY()));
        if (schematicContainers.isEmpty()) return null;
        HashMap<Integer, Double> probabilities = new HashMap<>();
        for (int i = 0; i < schematicContainers.size(); i++)
            probabilities.put(i, schematicContainers.get(i).getSchematicConfigField().getWeight());
        SchematicContainer schematicContainer = schematicContainers.get(WeighedProbability.pickWeighedProbability(probabilities));
        return schematicContainer;
    }
}
