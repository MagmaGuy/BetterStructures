package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.api.BuildPlaceEvent;
import com.magmaguy.betterstructures.api.ChestFillEvent;
import com.magmaguy.betterstructures.buildingfitter.util.FitUndergroundDeepBuilding;
import com.magmaguy.betterstructures.buildingfitter.util.LocationProjector;
import com.magmaguy.betterstructures.buildingfitter.util.SchematicPicker;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.thirdparty.EliteMobs;
import com.magmaguy.betterstructures.thirdparty.MythicMobs;
import com.magmaguy.betterstructures.thirdparty.WorldGuard;
import com.magmaguy.betterstructures.util.SpigotMessage;
import com.magmaguy.betterstructures.util.SurfaceMaterials;
import com.magmaguy.betterstructures.util.WarningMessage;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FitAnything {
    @Getter
    protected SchematicContainer schematicContainer;
    protected double startingScore = 100;
    protected final int searchRadius = 1;
    protected final int scanStep = 3;
    protected Clipboard schematicClipboard = null;
    protected Vector schematicOffset;
    //At 10% it is assumed a fit is so bad it's better just to skip
    protected double highestScore = 10;
    @Getter
    protected Location location = null;
    protected GeneratorConfigFields.StructureType structureType;

    public static void commandBasedCreation(Chunk chunk, GeneratorConfigFields.StructureType structureType, SchematicContainer container) {
        switch (structureType) {
            case SKY:
                new FitAirBuilding(chunk, container);
                break;
            case SURFACE:
                new FitSurfaceBuilding(chunk, container);
                break;
            case LIQUID_SURFACE:
                new FitLiquidBuilding(chunk, container);
                break;
            case UNDERGROUND_DEEP:
                FitUndergroundDeepBuilding.fit(chunk, container);
                break;
            case UNDERGROUND_SHALLOW:
                FitUndergroundShallowBuilding.fit(chunk, container);
                break;
            default:
        }
    }

    protected void setSchematicFilename(Location location, GeneratorConfigFields.StructureType structureType) {
        if (schematicClipboard != null) return;
        schematicContainer = SchematicPicker.pick(location, structureType);
        if (schematicContainer != null)
            schematicClipboard = schematicContainer.getClipboard();
    }


    protected void paste(Location location) {
        BuildPlaceEvent buildPlaceEvent = new BuildPlaceEvent(this);
        if (buildPlaceEvent.isCancelled()) return;
        Bukkit.getServer().getPluginManager().callEvent(buildPlaceEvent);

        FitAnything fitAnything = this;
        //Set pedestal material before the paste so bedrock blocks get replaced correctly
        assignPedestalMaterial(location);
        if (pedestalMaterial == null)
            switch (location.getWorld().getEnvironment()) {
                case NORMAL:
                case CUSTOM:
                    pedestalMaterial = Material.STONE;
                    break;
                case NETHER:
                    pedestalMaterial = Material.NETHERRACK;
                    break;
                case THE_END:
                    pedestalMaterial = Material.END_STONE;
                    break;
                default:
                    pedestalMaterial = Material.STONE;
            }

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
        if (DefaultConfig.isNewBuildingWarn()) {
            String structureTypeString = fitAnything.structureType.toString().toLowerCase().replace("_", " ");
            for (Player player : Bukkit.getOnlinePlayers())
                if (player.hasPermission("betterstructures.warn"))
                    player.spigot().sendMessage(
                            SpigotMessage.commandHoverMessage("[BetterStructures] New " + structureTypeString + " building generated! Click to teleport. Do \"/betterstructures silent\" to stop getting warnings!",
                                    "Click to teleport to " + location.getWorld().getName() + ", " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "\n Schem name: " + schematicContainer.getConfigFilename(),
                                    "/betterstructures teleporttocoords " + location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ())
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

        if (!(fitAnything instanceof FitAirBuilding)) {
            try {
                addPedestal(location);
            } catch (Exception exception) {
                new WarningMessage("Failed to correctly assign pedestal material!");
                exception.printStackTrace();
            }
            try {
                clearTrees(location);
            } catch (Exception exception) {
                new WarningMessage("Failed to correctly clear trees!");
                exception.printStackTrace();
            }
        }
        try {
            fillChests();
        } catch (Exception exception) {
            new WarningMessage("Failed to correctly fill chests!");
            exception.printStackTrace();
        }
        try {
            spawnEntities();
        } catch (Exception exception) {
            new WarningMessage("Failed to correctly spawn entities!");
            //exception.printStackTrace();
        }

    }

    Material pedestalMaterial = null;

    private void assignPedestalMaterial(Location location) {
        if (this instanceof FitAirBuilding) return;
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
        if (this instanceof FitAirBuilding || this instanceof FitLiquidBuilding) return;
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
        if (schematicContainer.getGeneratorConfigFields().getChestContents() != null)
            for (Vector chestPosition : schematicContainer.getChestLocations()) {
                Location chestLocation = LocationProjector.project(location, schematicOffset, chestPosition);
                Container container = (Container) chestLocation.getBlock().getState();

                if (schematicContainer.getChestContents() != null)
                    schematicContainer.getChestContents().rollChestContents(container);
                else
                    schematicContainer.getGeneratorConfigFields().getChestContents().rollChestContents(container);

                ChestFillEvent chestFillEvent = new ChestFillEvent(container);
                Bukkit.getServer().getPluginManager().callEvent(chestFillEvent);
                if (!chestFillEvent.isCancelled())
                    container.update(true);
            }
    }

    private void spawnEntities() {
        for (Vector entityPosition : schematicContainer.getVanillaSpawns().keySet()) {
            Location signLocation = LocationProjector.project(location, schematicOffset, entityPosition).clone();
            signLocation.getBlock().setType(Material.AIR);
            //If mobs spawn in corners they might choke on adjacent walls
            signLocation.add(new Vector(0.5, 0, 0.5));
            Entity entity = signLocation.getWorld().spawnEntity(signLocation, schematicContainer.getVanillaSpawns().get(entityPosition));
            entity.setPersistent(true);
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).setRemoveWhenFarAway(false);
            }
            if (entity.getType().equals(EntityType.ENDER_CRYSTAL)) {
                EnderCrystal enderCrystal = (EnderCrystal) entity;
                enderCrystal.setShowingBottom(false);
            }
        }
        for (Vector elitePosition : schematicContainer.getEliteMobsSpawns().keySet()) {
            Location eliteLocation = LocationProjector.project(location, schematicOffset, elitePosition).clone();
            eliteLocation.getBlock().setType(Material.AIR);
            eliteLocation.add(new Vector(0.5, 0, 0.5));
            String bossFilename = schematicContainer.getEliteMobsSpawns().get(elitePosition);
            //If the spawn fails then don't continue
            if (!EliteMobs.Spawn(eliteLocation, bossFilename)) return;
            Location lowestCorner = location.clone().add(schematicOffset);
            Location highestCorner = lowestCorner.clone().add(new Vector(schematicClipboard.getRegion().getWidth() - 1, schematicClipboard.getRegion().getHeight(), schematicClipboard.getRegion().getLength() - 1));
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null &&
                    Bukkit.getPluginManager().getPlugin("EliteMobs") != null) {
                WorldGuard.Protect(
                        BlockVector3.at(lowestCorner.getX(), lowestCorner.getY(), lowestCorner.getZ()),
                        BlockVector3.at(highestCorner.getX(), highestCorner.getY(), highestCorner.getZ()),
                        bossFilename, eliteLocation);
            } else {
                if (!worldGuardWarn) {
                    worldGuardWarn = true;
                    new WarningMessage("You are not using WorldGuard, so BetterStructures could not protect a boss arena! Using WorldGuard is recommended to guarantee a fair combat experience.");
                }
            }
        }

        // carm start - Support for MythicMobs
        for (Map.Entry<Vector, String> entry : schematicContainer.getMythicMobsSpawns().entrySet()) {
            Location mobLocation = LocationProjector.project(location, schematicOffset, entry.getKey()).clone();
            String conf = entry.getValue();

            //If the spawn fails then don't continue
            if (!MythicMobs.Spawn(mobLocation, conf)) return;

            Location lowestCorner = location.clone().add(schematicOffset);
            Location highestCorner = lowestCorner.clone().add(new Vector(schematicClipboard.getRegion().getWidth() - 1, schematicClipboard.getRegion().getHeight(), schematicClipboard.getRegion().getLength() - 1));

            if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null &&
                    Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
                WorldGuard.Protect(
                        BlockVector3.at(lowestCorner.getX(), lowestCorner.getY(), lowestCorner.getZ()),
                        BlockVector3.at(highestCorner.getX(), highestCorner.getY(), highestCorner.getZ()),
                        conf, mobLocation);
            } else {
                if (!worldGuardWarn) {
                    worldGuardWarn = true;
                    new WarningMessage("You are not using WorldGuard, so BetterStructures could not protect a boss arena! Using WorldGuard is recommended to guarantee a fair combat experience.");
                }
            }
        }
        // carm end - Support for MythicMobs

    }

    public static boolean worldGuardWarn = false;
}
