package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.buildingfitter.util.LocationProjector;
import com.magmaguy.betterstructures.buildingfitter.util.SchematicPicker;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.SpigotMessage;
import com.magmaguy.betterstructures.util.SurfaceMaterials;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class FitAnything {
    protected SchematicContainer schematicContainer;
    protected final double startingScore = 100;
    protected final int searchRadius = 1;
    protected final int scanStep = 3;
    protected Clipboard schematicClipboard = null;
    protected Vector schematicOffset;
    //At 10% it is assumed a fit is so bad it's better just to skip
    protected double highestScore = 10;
    protected Location location = null;

    protected void setSchematicFilename(Location location, GeneratorConfigFields.StructureType structureType) {
        if (schematicClipboard != null) return;
        schematicContainer = SchematicPicker.pick(location, structureType);
        if (schematicContainer != null)
            schematicClipboard = schematicContainer.getClipboard();
    }


    protected void paste(Location location) {

        //Set pedestal material before the paste so bedrock blocks get replaced correcty
        assignPedestalMaterial(location);

        //These blocks are dynamic and get replaced with world contents, need to be replaced back after the paste to preserve the mechanic
        Set<BlockVector3> barrierBlocks = new HashSet<>();
        Set<BlockVector3> bedrockBlocks = new HashSet<>();
        BlockData barrierBlock = null;
        BlockData bedrockBlock = null;


        //adjusts the offset just for the prescan, not needed for worldedit as that figures it out on its own
        Location adjustedLocation = location.clone().add(schematicOffset);
        for (int x = 0; x < schematicClipboard.getDimensions().getX(); x++)
            for (int y = 0; y < schematicClipboard.getDimensions().getY(); y++)
                for (int z = 0; z < schematicClipboard.getDimensions().getZ(); z++) {
                    BlockVector3 adjustedClipboardLocation = BlockVector3.at(
                            x + schematicClipboard.getMinimumPoint().getX(),
                            y + schematicClipboard.getMinimumPoint().getY(),
                            z + schematicClipboard.getMinimumPoint().getZ());
                    BlockState blockState = schematicClipboard.getBlock(adjustedClipboardLocation);
                    Material material = BukkitAdapter.adapt(blockState.getBlockType());
                    Block worldBlock = adjustedLocation.clone().add(new Vector(x, y, z)).getBlock();
                    if (material == Material.BARRIER) {
                        //special behavior: do not replace
                        try {
                            if (barrierBlock == null)
                                barrierBlock = BukkitAdapter.adapt(schematicClipboard.getBlock(adjustedClipboardLocation));
                            schematicClipboard.setBlock(adjustedClipboardLocation, BukkitAdapter.adapt(worldBlock.getBlockData()));
                            barrierBlocks.add(adjustedClipboardLocation);
                        } catch (WorldEditException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (material == Material.BEDROCK) {
                        //special behavior: replace if air
                        try {
                            if (bedrockBlock == null)
                                bedrockBlock = BukkitAdapter.adapt(schematicClipboard.getBlock(adjustedClipboardLocation));

                            worldBlock = adjustedLocation.clone().add(new Vector(x, y, z)).getBlock();
                            if (worldBlock.getType().isAir() || worldBlock.isLiquid()) {
                                //Case for air - replace with filler block
                                worldBlock.setType(pedestalMaterial);
                            }
                            //Case for any solid block - do not replace world block
                            schematicClipboard.setBlock(adjustedClipboardLocation, BukkitAdapter.adapt(worldBlock.getBlockData()));
                            bedrockBlocks.add(adjustedClipboardLocation);
                        } catch (WorldEditException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }


        Schematic.paste(schematicClipboard, location);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("betterstructures.warn"))
                player.spigot().sendMessage(
                        SpigotMessage.commandHoverMessage("[BetterStructures] New building generated! Click to teleport.",
                                "Click to teleport to " + location.toString(),
                                "/tp " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ())
                );
        }
        for (BlockVector3 blockVector3 : barrierBlocks) {
            try {
                schematicClipboard.setBlock(blockVector3, BukkitAdapter.adapt(barrierBlock));
            } catch (WorldEditException e) {
                throw new RuntimeException(e);
            }
        }

        for (BlockVector3 blockVector3 : bedrockBlocks) {
            try {
                schematicClipboard.setBlock(blockVector3, BukkitAdapter.adapt(bedrockBlock));
            } catch (WorldEditException e) {
                throw new RuntimeException(e);
            }
        }

        if (!(this instanceof FitAirBuilding)) {
            addPedestal(location);
            clearTrees(location);
        }

        fillChests();
        spawnEntities();
    }

    Material pedestalMaterial = null;

    private void assignPedestalMaterial(Location location) {
        if (this instanceof FitAirBuilding || this instanceof FitLiquidBuilding) return;
        pedestalMaterial = schematicContainer.getSchematicConfigField().getPedestalMaterial();
        //If the pedestal material is null, fill in with the most common sampled ground source
        if (pedestalMaterial != null) return;
        Location lowestCorner = location.clone().add(schematicOffset);
        HashMap<Material, Integer> materials = new HashMap<>();
        for (int x = 0; x < schematicClipboard.getDimensions().getX(); x += 3)
            for (int z = 0; z < schematicClipboard.getDimensions().getZ(); z += 3)
                for (int y = -1; y > -3; y--) {
                    Block groundBlock = lowestCorner.clone().add(new Vector(x, 0, z)).getBlock();
                    if (SurfaceMaterials.isPedestalMaterial(groundBlock.getType()))
                        if (materials.get(groundBlock.getType()) != null)
                            materials.
                                    put(groundBlock.getType(), materials.get(groundBlock.getType()) + 1);
                        else
                            materials.put(groundBlock.getType(), 1);
                }
        //Case for if all blocks were air, this should be impossible
        if (materials.isEmpty()) pedestalMaterial = Material.DIRT;
        Material mostCommonMaterial = null;
        int highestScore = 0;
        for (Material material : materials.keySet()) {
            if (highestScore < materials.get(material)) {
                highestScore = materials.get(material);
                mostCommonMaterial = material;
            }
        }
        pedestalMaterial = mostCommonMaterial;
    }

    private void addPedestal(Location location) {
        Location lowestCorner = location.clone().add(schematicOffset);
        for (int x = 0; x < schematicClipboard.getDimensions().getX(); x++)
            for (int z = 0; z < schematicClipboard.getDimensions().getZ(); z++) {
                //Only add pedestals for areas with a solid floor, some schematics can have rounded air edges to better fit terrain
                Block groundBlock = lowestCorner.clone().add(new Vector(x, 0, z)).getBlock();
                if (groundBlock.getType().isAir()) continue;
                for (int y = -1; y > -11; y--) {
                    Block block = lowestCorner.clone().add(new Vector(x, y, z)).getBlock();
                    if (SurfaceMaterials.ignorable(block.getType()))
                        block.setType(pedestalMaterial);
                    else
                        //Pedestal only fills until it hits the first solid block
                        break;
                }
            }
    }


    private void clearTrees(Location location) {
        Location highestCorner = location.clone().add(schematicOffset).add(new Vector(0, schematicClipboard.getDimensions().getY(), 0));
        boolean detectedTreeElement = true;
        for (int y = 0; y < 31; y++) {
            if (!detectedTreeElement) return;
            detectedTreeElement = false;
            for (int x = 0; x < schematicClipboard.getDimensions().getX(); x++)
                for (int z = 0; z < schematicClipboard.getDimensions().getZ(); z++) {
                    Block block = highestCorner.clone().add(new Vector(x, y, z)).getBlock();
                    if (SurfaceMaterials.ignorable(block.getType()) && !block.getType().isAir()) {
                        detectedTreeElement = true;
                        block.setType(Material.AIR);
                    }
                }
        }
    }

    private void fillChests() {
        for (Vector chestPosition : schematicContainer.getChestLocations()) {
            Location chestLocation = LocationProjector.project(location, schematicOffset, chestPosition);
            schematicContainer.getGeneratorConfigFields().getChestContents().rollChestContents((Chest) chestLocation.getBlock().getState());
        }
    }

    private void spawnEntities() {
        for (Vector entityPosition : schematicContainer.getVanillaSpawns().keySet()) {
            Location signLocation = LocationProjector.project(location, schematicOffset, entityPosition);
            signLocation.getBlock().setType(Material.AIR);
            Entity entity = signLocation.getWorld().spawnEntity(signLocation, schematicContainer.getVanillaSpawns().get(entityPosition));
            entity.setPersistent(true);
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).setRemoveWhenFarAway(false);
            }
        }
    }

}
