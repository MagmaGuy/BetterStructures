package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.api.WorldGenerationFinishEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3i;

import java.util.*;

public class ModularWorld {
    @Getter
    private final List<Vector3i> spawnLocations = new ArrayList<>();
    @Getter
    private World world = null;
    private HashSet<ModulePasting.InterpretedSign> otherLocations = new HashSet<>();
    @Getter
    private ModularGenerationStatus modularGenerationStatus;

    public ModularWorld(World world, List<ModulePasting.InterpretedSign> interpretedSigns, ModularGenerationStatus modularGenerationStatus) {
        this.world = world;
        this.modularGenerationStatus = modularGenerationStatus;
        for (ModulePasting.InterpretedSign interpretedSign : interpretedSigns) {
            for (String signText : interpretedSign.text()) {
                if (signText.contains("[spawn]"))
                    spawnLocations.add(new Vector3i(
                            (int) interpretedSign.location().getX(),
                            (int) interpretedSign.location().getY(),
                            (int) interpretedSign.location().getZ()));
                else
                    otherLocations.add(interpretedSign);
            }
        }
    }

    public void spawnOtherEntities(){
        new BukkitRunnable() {
            @Override
            public void run() {
                modularGenerationStatus.startPlacingMobs();
                for (ModulePasting.InterpretedSign otherLocation : otherLocations)
                    for (String string : otherLocation.text())
                        if (string.contains("pool"))
                            otherLocation.location().getWorld().spawn(otherLocation.location(), Zombie.class, zombie -> {
                                zombie.setRemoveWhenFarAway(false);
                                zombie.setCustomName("TEST ZOMBIE");
                                zombie.setCustomNameVisible(true);
                                zombie.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 5));
                            });
                //got to keep the memory clear for this one, unfortunately
                otherLocations.clear();
                modularGenerationStatus.finishedPlacingMobs();
                modularGenerationStatus.done();
                generationFinished();
            }
        }.runTask(MetadataHandler.PLUGIN);
    }

    public void generationFinished(){
        Bukkit.getServer().getPluginManager().callEvent(new WorldGenerationFinishEvent(this));
    }
}
