package com.magmaguy.betterstructures.config.modules;

import lombok.Getter;
import org.bukkit.World;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModularWorld {
    @Getter
    private final List<Vector3i> spawnLocations = new ArrayList<>();
    @Getter
    private final HashMap<Vector3i, ModularChunk> modularChunks;
    @Getter
    private final World world;

    public ModularWorld(World world, HashMap<Vector3i, ModularChunk> modularChunks) {
        this.world = world;
        this.modularChunks = modularChunks;
        for (ModularChunk value : modularChunks.values())
            for (Map.Entry<Vector3i, List<String>> vector3iListEntry : value.rawSigns().entrySet())
                for (String s : vector3iListEntry.getValue())
                    if (s.equalsIgnoreCase("[spawn]"))
                        spawnLocations.add(vector3iListEntry.getKey());
    }

    public record ModularChunk(Vector3i chunkLocation, HashMap<Vector3i, List<String>> rawSigns) {

    }
}
