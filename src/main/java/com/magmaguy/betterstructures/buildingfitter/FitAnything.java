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
import com.magmaguy.betterstructures.util.SurfaceMaterials;
import com.magmaguy.betterstructures.util.VersionChecker;
import com.magmaguy.betterstructures.util.WeighedProbability;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.SpigotMessage;
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
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FitAnything {
    public static boolean worldGuardWarn = false;
    protected final int searchRadius = 1;
    protected final int scanStep = 3;
    @Getter
    protected SchematicContainer schematicContainer;
    protected double startingScore = 100;
    @Getter
    protected Clipboard schematicClipboard = null;
    @Getter
    protected Vector schematicOffset;
    //At 10% it is assumed a fit is so bad it's better just to skip
    protected double highestScore = 10;
    @Getter
    protected Location location = null;
    protected GeneratorConfigFields.StructureType structureType;
    Material pedestalMaterial = null;
    private final HashMap<Material, Integer> undergroundPedestalMaterials = new HashMap<>();
    private final HashMap<Material, Integer> surfacePedestalMaterials = new HashMap<>();

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
        Bukkit.getServer().getPluginManager().callEvent(buildPlaceEvent);
        if (buildPlaceEvent.isCancelled()) return;

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
        for (int x = 0; x < schematicClipboard.getDimensions().x(); x++)
            for (int y = 0; y < schematicClipboard.getDimensions().y(); y++)
                for (int z = 0; z < schematicClipboard.getDimensions().z(); z++) {
                    BlockVector3 adjustedClipboardLocation = BlockVector3.at(
                            x + schematicClipboard.getMinimumPoint().x(),
                            y + schematicClipboard.getMinimumPoint().y(),
                            z + schematicClipboard.getMinimumPoint().z());
                    BlockState blockState = schematicClipboard.getBlock(adjustedClipboardLocation);
                    Material material = BukkitAdapter.adapt(blockState.getBlockType());
                    boolean isGround =  !BukkitAdapter.adapt(schematicClipboard.getBlock(BlockVector3.at(adjustedClipboardLocation.x(), adjustedLocation.getY(), adjustedLocation.getZ()).add(0,1,0)).getBlockType()).isSolid();
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
                                worldBlock.setType(getPedestalMaterial(isGround));
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
            String structureTypeString = fitAnything.structureType.toString().toLowerCase(Locale.ROOT).replace("_", " ");
            for (Player player : Bukkit.getOnlinePlayers())
                if (player.hasPermission("betterstructures.warn"))
                    player.spigot().sendMessage(
                            SpigotMessage.commandHoverMessage("[BetterStructures] New " + structureTypeString + " building generated! Click to teleport. Do \"/betterstructures silent\" to stop getting warnings!",
                                    "Click to teleport to " + location.getWorld().getName() + ", " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "\n Schem name: " + schematicContainer.getConfigFilename(),
                                    "/betterstructures teleport " + location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ())
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
                Logger.warn("Failed to correctly assign pedestal material!");
                exception.printStackTrace();
            }
            try {
                clearTrees(location);
            } catch (Exception exception) {
                Logger.warn("Failed to correctly clear trees!");
                exception.printStackTrace();
            }
        }
        try {
            fillChests();
        } catch (Exception exception) {
            Logger.warn("Failed to correctly fill chests!");
            exception.printStackTrace();
        }
        try {
            spawnEntities();
        } catch (Exception exception) {
            Logger.warn("Failed to correctly spawn entities!");
            //exception.printStackTrace();
        }

    }

    private void assignPedestalMaterial(Location location) {
        if (this instanceof FitAirBuilding) return;
        pedestalMaterial = schematicContainer.getSchematicConfigField().getPedestalMaterial();
        Location lowestCorner = location.clone().add(schematicOffset);

        int maxSurfaceHeightScan = 20;

        //get underground pedestal blocks
        for (int x = 0; x < schematicClipboard.getDimensions().x(); x++)
            for (int z = 0; z < schematicClipboard.getDimensions().z(); z++)
                for (int y = 0; y < schematicClipboard.getDimensions().y(); y++) {
                    Block groundBlock = lowestCorner.clone().add(new Vector(x, y, z)).getBlock();
                    Block aboveBlock = groundBlock.getRelative(BlockFace.UP);

                    if (aboveBlock.getType().isSolid() && groundBlock.getType().isSolid() && !SurfaceMaterials.ignorable(groundBlock.getType()))
                        undergroundPedestalMaterials.merge(groundBlock.getType(), 1, Integer::sum);
                }

        //get above ground pedestal blocks, if any
        for (int x = 0; x < schematicClipboard.getDimensions().x(); x++)
            for (int z = 0; z < schematicClipboard.getDimensions().z(); z++) {
                boolean scanUp = lowestCorner.clone().add(new Vector(x, schematicClipboard.getDimensions().y(), z)).getBlock().getType().isSolid();
                for (int y = 0; y < maxSurfaceHeightScan; y++) {
                    Block groundBlock = lowestCorner.clone().add(new Vector(x, scanUp ? y : -y, z)).getBlock();
                    Block aboveBlock = groundBlock.getRelative(BlockFace.UP);

                    if (!aboveBlock.getType().isSolid() && groundBlock.getType().isSolid()) {
                        surfacePedestalMaterials.merge(groundBlock.getType(), 1, Integer::sum);
                        break;
                    }
                }
            }
    }

    private Material getPedestalMaterial(boolean isPedestalSurface) {
        if (isPedestalSurface){
            if (surfacePedestalMaterials.isEmpty()) return pedestalMaterial;
            return getRandomMaterialBasedOnWeight(surfacePedestalMaterials);
        } else {
            if (undergroundPedestalMaterials.isEmpty()) return pedestalMaterial;
            return getRandomMaterialBasedOnWeight(undergroundPedestalMaterials);
        }
    }

    public Material getRandomMaterialBasedOnWeight(HashMap<Material, Integer> weightedMaterials) {
        // Calculate the total weight
        int totalWeight = weightedMaterials.values().stream().mapToInt(Integer::intValue).sum();

        // Generate a random number in the range of 0 (inclusive) to totalWeight (exclusive)
        int randomNumber = ThreadLocalRandom.current().nextInt(totalWeight);

        // Iterate through the materials and pick one based on the random number
        int cumulativeWeight = 0;
        for (Map.Entry<Material, Integer> entry : weightedMaterials.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomNumber < cumulativeWeight) {
                return entry.getKey();
            }
        }

        // Fallback return, should not occur if the map is not empty and weights are positive
        throw new IllegalStateException("Weighted random selection failed.");
    }

    private void addPedestal(Location location) {
        if (this instanceof FitAirBuilding || this instanceof FitLiquidBuilding) return;
        Location lowestCorner = location.clone().add(schematicOffset);
        for (int x = 0; x < schematicClipboard.getDimensions().x(); x++)
            for (int z = 0; z < schematicClipboard.getDimensions().z(); z++) {
                //Only add pedestals for areas with a solid floor, some schematics can have rounded air edges to better fit terrain
                Block groundBlock = lowestCorner.clone().add(new Vector(x, 0, z)).getBlock();
                if (groundBlock.getType().isAir()) continue;
                for (int y = -1; y > -11; y--) {
                    Block block = lowestCorner.clone().add(new Vector(x, y, z)).getBlock();
                    if (SurfaceMaterials.ignorable(block.getType()))
                        block.setType(getPedestalMaterial(!block.getRelative(BlockFace.UP).getType().isSolid()));
                    else {
                        //Pedestal only fills until it hits the first solid block
                        break;
                    }
                }
            }
    }

    private void clearTrees(Location location) {
        Location highestCorner = location.clone().add(schematicOffset).add(new Vector(0, schematicClipboard.getDimensions().y(), 0));
        boolean detectedTreeElement = true;
        for (int y = 0; y < 31; y++) {
            if (!detectedTreeElement) return;
            detectedTreeElement = false;
            for (int x = 0; x < schematicClipboard.getDimensions().x(); x++)
                for (int z = 0; z < schematicClipboard.getDimensions().z(); z++) {
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
                if (!(chestLocation.getBlock().getState() instanceof Container container)) {
                    Logger.warn("Expected a container for " + chestLocation.getBlock().getType() + " but didn't get it. Skipping this loot!");
                    continue;
                }

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
            //I think FAWE is messing with this
            signLocation.getChunk().load();
            Entity entity = signLocation.getWorld().spawnEntity(signLocation, schematicContainer.getVanillaSpawns().get(entityPosition));
            entity.setPersistent(true);
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).setRemoveWhenFarAway(false);
            }

            if (!VersionChecker.serverVersionOlderThan(21, 0) &&
                    entity.getType().equals(EntityType.END_CRYSTAL)) {
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
            if (DefaultConfig.isProtectEliteMobsRegions() &&
                    Bukkit.getPluginManager().getPlugin("WorldGuard") != null &&
                    Bukkit.getPluginManager().getPlugin("EliteMobs") != null) {
                WorldGuard.Protect(lowestCorner, highestCorner, bossFilename, eliteLocation);
            } else {
                if (!worldGuardWarn) {
                    worldGuardWarn = true;
                    Logger.warn("You are not using WorldGuard, so BetterStructures could not protect a boss arena! Using WorldGuard is recommended to guarantee a fair combat experience.");
                }
            }
        }

        // carm start - Support for MythicMobs
        for (Map.Entry<Vector, String> entry : schematicContainer.getMythicMobsSpawns().entrySet()) {
            Location mobLocation = LocationProjector.project(location, schematicOffset, entry.getKey()).clone();
            mobLocation.getBlock().setType(Material.AIR);

            //If the spawn fails then don't continue
            if (!MythicMobs.Spawn(mobLocation, entry.getValue())) return;
        }
        // carm end - Support for MythicMobs

    }
}
