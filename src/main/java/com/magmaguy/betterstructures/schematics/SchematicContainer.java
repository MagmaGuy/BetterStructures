package com.magmaguy.betterstructures.schematics;

import com.google.common.collect.ArrayListMultimap;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.config.schematics.SchematicConfigField;
import com.magmaguy.betterstructures.util.WarningMessage;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SchematicContainer {
    @Getter
    private static final ArrayListMultimap<GeneratorConfigFields.StructureType, SchematicContainer> schematics = ArrayListMultimap.create();

    public static void shutdown() {
        schematics.clear();
    }

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

    public SchematicContainer(Clipboard clipboard, String clipboardFilename, SchematicConfigField schematicConfigField, String configFilename) {
        this.clipboard = clipboard;
        this.clipboardFilename = clipboardFilename;
        this.schematicConfigField = schematicConfigField;
        this.configFilename = configFilename;
        generatorConfigFields = schematicConfigField.getGeneratorConfigFields();
        if (generatorConfigFields != null)
            generatorConfigFields.getStructureTypes().forEach(structureType -> schematics.put(structureType, this));
        else
            schematics.put(GeneratorConfigFields.StructureType.UNDEFINED, this);
        for (int x = 0; x <= clipboard.getDimensions().getX(); x++)
            for (int y = 0; y <= clipboard.getDimensions().getY(); y++)
                for (int z = 0; z <= clipboard.getDimensions().getZ(); z++) {
                    BlockVector3 translatedLocation = BlockVector3.at(x, y, z).add(clipboard.getMinimumPoint());
                    BlockState weBlockState = clipboard.getBlock(translatedLocation);
                    Material minecraftMaterial = BukkitAdapter.adapt(weBlockState.getBlockType());
                    //register chest location
                    if (minecraftMaterial.equals(Material.CHEST) ||
                            minecraftMaterial.equals(Material.TRAPPED_CHEST) ||
                            minecraftMaterial.equals(Material.SHULKER_BOX)) {
                        chestLocations.add(new Vector(x, y, z));
                    }
                    if (minecraftMaterial.equals(Material.ACACIA_SIGN) ||
                            minecraftMaterial.equals(Material.ACACIA_WALL_SIGN) ||
                            minecraftMaterial.equals(Material.SPRUCE_SIGN) ||
                            minecraftMaterial.equals(Material.SPRUCE_WALL_SIGN) ||
                            minecraftMaterial.equals(Material.BIRCH_SIGN) ||
                            minecraftMaterial.equals(Material.BIRCH_WALL_SIGN) ||
                            minecraftMaterial.equals(Material.CRIMSON_SIGN) ||
                            minecraftMaterial.equals(Material.CRIMSON_WALL_SIGN) ||
                            minecraftMaterial.equals(Material.DARK_OAK_SIGN) ||
                            minecraftMaterial.equals(Material.DARK_OAK_WALL_SIGN) ||
                            minecraftMaterial.equals(Material.JUNGLE_SIGN) ||
                            minecraftMaterial.equals(Material.JUNGLE_WALL_SIGN) ||
                            minecraftMaterial.equals(Material.OAK_SIGN) ||
                            minecraftMaterial.equals(Material.OAK_WALL_SIGN) ||
                            minecraftMaterial.equals(Material.WARPED_SIGN) ||
                            minecraftMaterial.equals(Material.WARPED_WALL_SIGN)) {
                        BaseBlock baseBlock = clipboard.getFullBlock(translatedLocation);
                        //For future reference, I don't know how to get the data in any other way than parsing the string. Sorry!
                        String line1 = baseBlock.getNbtData().getString("Text1");
                        //Case for spawning a vanilla mob
                        if (line1.toLowerCase().contains("[spawn]")) {
                            String line2 = baseBlock.getNbtData().getString("Text2");
                            line2 = line2.toUpperCase();
                            String value = line2.split(":")[1].replace("\"", "").replace("}", "");
                            EntityType entityType;
                            try {
                                entityType = EntityType.valueOf(value);
                            } catch (Exception ex) {
                                new WarningMessage("Failed to determine entity type for sign! Entry was " + value + " in schematic " + clipboardFilename + " ! Fix this by inputting a valid entity type!");
                                continue;
                            }
                            vanillaSpawns.put(new Vector(x, y, z), entityType);
                        } else if (line1.toLowerCase().contains("[elitemobs]")) {
                            //todo: elitemobs integration goes here
                        }
                    }
                }
    }

    public boolean isValidEnvironment(World.Environment environment) {
        return generatorConfigFields.getValidWorldEnvironments().isEmpty() || generatorConfigFields.getValidWorldEnvironments().contains(environment);
    }

    public boolean isValidBiome(Biome biome) {
        return generatorConfigFields.getValidBiomes().isEmpty() || generatorConfigFields.getValidBiomes().contains(biome);
    }

    public boolean isValidYLevel(int yLevel) {
        return generatorConfigFields.getLowestYLevel() <= yLevel && generatorConfigFields.getHighestYLevel() >= yLevel;
    }
}
