package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ModularWorld {
    @Getter
    private final List<Vector3i> spawnLocations = new ArrayList<>();
//    @Getter
//    private final HashMap<Vector3i, ModularChunk> modularChunks;
    @Getter
    private final World world;

    public ModularWorld(World world, List<ModulePasting.InterpretedSign> interpretedSigns) {
        this.world = world;
        for (ModulePasting.InterpretedSign interpretedSign : interpretedSigns) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    interpretedSign.location().getWorld().spawn(interpretedSign.location(), Zombie.class, new Consumer<Zombie>() {
                        @Override
                        public void accept(Zombie zombie) {
                            zombie.setRemoveWhenFarAway(false);
                            zombie.setCustomName("TEST ZOMBIE");
                            zombie.setCustomNameVisible(true);
                            zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 5));
                        }
                    });
                }
            }.runTaskLater(MetadataHandler.PLUGIN, 20*10);
        }
//        this.modularChunks = modularChunks;
//        for (ModularChunk value : modularChunks.values())
//            for (Map.Entry<Vector3i, List<String>> vector3iListEntry : value.rawSigns().entrySet())
//                for (String s : vector3iListEntry.getValue())
//                    if (s.equalsIgnoreCase("[spawn]"))
//                        spawnLocations.add(vector3iListEntry.getKey());
    }

    public record ModularChunk(Vector3i chunkLocation, HashMap<Vector3i, List<String>> rawSigns) {

    }
}
