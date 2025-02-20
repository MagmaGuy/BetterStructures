package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.api.WorldGenerationFinishEvent;
import com.magmaguy.betterstructures.config.spawnpools.SpawnPoolsConfig;
import com.magmaguy.betterstructures.config.spawnpools.SpawnPoolsConfigFields;
import com.magmaguy.elitemobs.mobconstructor.custombosses.CustomBossEntity;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModularWorld {
    @Getter
    private final List<Vector3i> spawnLocations = new ArrayList<>();
    private final HashSet<ModulePasting.InterpretedSign> otherLocations = new HashSet<>();
    @Getter
    private final ModularGenerationStatus modularGenerationStatus;
    @Getter
    private World world = null;

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

    public static String extractPoolText(String input) {
        Pattern pattern = Pattern.compile("\\[pool:\\s*([^\\]]+)\\]");
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    public void spawnOtherEntities() {
        new BukkitRunnable() {
            @Override
            public void run() {
                modularGenerationStatus.startPlacingMobs();
                for (ModulePasting.InterpretedSign otherLocation : otherLocations)
                    for (String string : otherLocation.text())
                        if (string.contains("pool")) {
                            String parsedString = extractPoolText(string) + ".yml";
                            SpawnPoolsConfigFields spawnPoolsConfigFields = SpawnPoolsConfig.getConfigFields(parsedString);
                            if (spawnPoolsConfigFields == null) {
                                Logger.warn("Could not find spawn pool " + parsedString);
                                continue;
                            }
                            otherLocation.location().getChunk().load();
                            CustomBossEntity customBossEntity = CustomBossEntity.createCustomBossEntity(spawnPoolsConfigFields.getPoolStrings().get(ThreadLocalRandom.current().nextInt(0, spawnPoolsConfigFields.getPoolStrings().size())));
                            customBossEntity.spawn(otherLocation.location(), true);
                        }
                //got to keep the memory clear for this one, unfortunately
                otherLocations.clear();
                modularGenerationStatus.finishedPlacingMobs();
                modularGenerationStatus.done();
                generationFinished();
            }
        }.runTask(MetadataHandler.PLUGIN);
    }

    public void generationFinished() {
        Bukkit.getServer().getPluginManager().callEvent(new WorldGenerationFinishEvent(this));
    }
}
