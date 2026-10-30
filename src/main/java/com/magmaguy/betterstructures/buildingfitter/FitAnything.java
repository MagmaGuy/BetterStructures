package com.magmaguy.betterstructures.buildingfitter;

import com.magmaguy.betterstructures.api.BuildCustomMarkerEvent;
import com.magmaguy.betterstructures.api.BuildPasteCompleteEvent;
import com.magmaguy.betterstructures.api.BuildPlaceEvent;
import com.magmaguy.betterstructures.api.ChestFillEvent;
import com.magmaguy.betterstructures.buildingfitter.util.FitUndergroundDeepBuilding;
import com.magmaguy.betterstructures.buildingfitter.util.LocationProjector;
import com.magmaguy.betterstructures.buildingfitter.util.SchematicPicker;
import com.magmaguy.betterstructures.buildingfitter.util.TerrainReads;
import com.magmaguy.betterstructures.chests.ChestContents;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.thirdparty.EliteMobs;
import com.magmaguy.betterstructures.thirdparty.MythicMobs;
import com.magmaguy.betterstructures.thirdparty.WorldGuard;
import com.magmaguy.betterstructures.util.SurfaceMaterials;
import com.magmaguy.betterstructures.worldedit.Schematic;
import com.magmaguy.betterstructures.worldedit.PasteChunkReadiness;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.entity.ExtentEntityCopy;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.SpigotMessage;
import com.magmaguy.magmacore.util.VersionChecker;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class FitAnything {
    public static boolean worldGuardWarn = false;
    protected final int searchRadius = 1;
    protected final int scanStep = 3;
    private final HashMap<Material, Integer> undergroundPedestalMaterials = new HashMap<>();
    private final HashMap<Material, Integer> surfacePedestalMaterials = new HashMap<>();
    @Getter
    protected SchematicContainer schematicContainer;
    protected double startingScore = 100;
    @Getter
    protected Clipboard schematicClipboard = null;
    @Getter
    protected Vector schematicOffset;
    protected int verticalOffset = 0;
    //At 10% it is assumed a fit is so bad it's better just to skip
    private static final double NO_FIT_SCORE = 10;
    protected double highestScore = NO_FIT_SCORE;
    @Getter
    protected Location location = null;
    // Set once a scan has picked its schematic and anchor; a natural scan that met an unloaded
    // chunk scores it again with the same plan once that chunk loads.
    protected Location anchor;
    protected GeneratorConfigFields.StructureType structureType;
    private Material pedestalMaterial = null;

    public FitAnything(SchematicContainer schematicContainer) {
        this.schematicContainer = schematicContainer;
        this.verticalOffset = schematicContainer.getClipboard().getMinimumPoint().y() - schematicContainer.getClipboard().getOrigin().y();
    }

    public FitAnything() {
    }

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

    protected void randomizeSchematicContainer(Location location, GeneratorConfigFields.StructureType structureType) {
        if (schematicClipboard != null) return;
        schematicContainer = SchematicPicker.pick(location, structureType);
        if (schematicContainer != null) {
            schematicClipboard = schematicContainer.getClipboard();
            verticalOffset = schematicContainer.getClipboard().getMinimumPoint().y() - schematicContainer.getClipboard().getOrigin().y();
        }
    }

    /** Scores {@link #anchor} and its neighbouring positions, then pastes the best fit. */
    protected void evaluate() {
    }

    /**
     * Runs a natural scan again after the chunks it could not read have loaded. The schematic and
     * anchor stay the same, so a structure that needs more chunks is not swapped for one that needs fewer.
     */
    public void rescan() {
        if (anchor == null) return;
        highestScore = NO_FIT_SCORE;
        location = null;
        evaluate();
    }

    protected void paste(Location location) {
        // A natural scan that met an unloaded chunk scored guesses; it runs again once the chunk loads.
        if (TerrainReads.incomplete()) return;
        BuildPlaceEvent buildPlaceEvent = new BuildPlaceEvent(this);
        Bukkit.getServer().getPluginManager().callEvent(buildPlaceEvent);
        if (buildPlaceEvent.isCancelled()) return;

        Schematic.enqueue(new Preparation(location.clone()));
    }

    private void setDefaultPedestalMaterial(Location base) {
        if (pedestalMaterial != null) return;
        pedestalMaterial = switch (base.getWorld().getEnvironment()) {
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.STONE;
        };
    }

    /** Sampling shares the paste queue's time budget and chunk lifetime. */
    private final class Preparation implements Schematic.PasteOperation {
        private final Location base;
        private final Location corner;
        private final PasteChunkReadiness chunks;
        private int phase, x, y, z;

        private Preparation(Location base) {
            this.base = base;
            corner = base.clone().add(schematicOffset);
            chunks = new PasteChunkReadiness(base.getWorld());
            pedestalMaterial = schematicContainer.getSchematicConfigField().getPedestalMaterial();
            if (FitAnything.this instanceof FitAirBuilding) phase = 2;
        }
        @Override public boolean hasNext() { return phase < 2; }
        @Override public boolean ready() { return chunks.ready(corner.clone().add(x, 0, z)); }
        @Override public void pasteNext() {
            if (phase == 0) {
                Block ground = corner.clone().add(x, y, z).getBlock();
                if (ground.getRelative(BlockFace.UP).getType().isSolid() && ground.getType().isSolid()
                        && !SurfaceMaterials.ignorable(ground.getType()))
                    undergroundPedestalMaterials.merge(ground.getType(), 1, Integer::sum);
                y += scanStep;
                if (y >= schematicClipboard.getDimensions().y()) { y = 0; advanceColumn(); }
            } else {
                boolean up = corner.clone().add(x, schematicClipboard.getDimensions().y(), z).getBlock().getType().isSolid();
                // At most 20 reads per column; no dimension-sized work inside a queue step.
                for (int offset = 0; offset < 20; offset++) {
                    Block ground = corner.clone().add(x, up ? offset : -offset, z).getBlock();
                    if (!ground.getRelative(BlockFace.UP).getType().isSolid() && ground.getType().isSolid()) {
                        surfacePedestalMaterials.merge(ground.getType(), 1, Integer::sum);
                        break;
                    }
                }
                advanceColumn();
            }
        }
        private void advanceColumn() {
            z += scanStep;
            if (z >= schematicClipboard.getDimensions().z()) { z = 0; x += scanStep; }
            if (x >= schematicClipboard.getDimensions().x()) { x = 0; phase++; }
        }
        @Override public void close() { chunks.close(); }
        @Override public void onComplete() {
            setDefaultPedestalMaterial(base);
            Schematic.pasteSchematic(schematicClipboard, base, schematicOffset,
                    FitAnything.this::getPedestalMaterial, () -> Schematic.enqueue(new Finishing(base)));
        }
    }

    /** One column, container, or entity per step; failures abort without announcing success. */
    private final class Finishing implements Schematic.PasteOperation {
        private final Location base;
        private final Location corner;
        private final PasteChunkReadiness chunks;
        private int phase, x, z;
        private Iterator<Vector> markers;
        private Iterator<? extends com.sk89q.worldedit.entity.Entity> props;
        private Vector nextMarker;
        private com.sk89q.worldedit.entity.Entity nextProp;
        private EditSession entitySession;
        private ExtentEntityCopy entityCopy;

        private Finishing(Location base) {
            this.base = base;
            corner = base.clone().add(schematicOffset);
            chunks = new PasteChunkReadiness(base.getWorld());
        }
        @Override public boolean hasNext() {
            while (phase < 8) {
                if (phase < 2) {
                    boolean enabled = phase == 0
                            ? !(FitAnything.this instanceof FitAirBuilding || FitAnything.this instanceof FitLiquidBuilding)
                            : FitAnything.this instanceof FitSurfaceBuilding;
                    if (enabled && x < schematicClipboard.getDimensions().x()) return true;
                } else if (phase != 6) {
                    if (nextMarker != null) return true;
                    if (markers == null) markers = switch (phase) {
                        case 2 -> schematicContainer.getChestLocations().iterator();
                        case 3 -> schematicContainer.getVanillaSpawns().keySet().iterator();
                        case 4 -> schematicContainer.getEliteMobsSpawns().keySet().iterator();
                        case 5 -> schematicContainer.getMythicMobsSpawns().keySet().iterator();
                        default -> schematicContainer.getCustomMarkers().keySet().iterator();
                    };
                    if (markers.hasNext()) { nextMarker = markers.next(); return true; }
                } else {
                    if (nextProp != null) return true;
                    if (props == null) props = schematicClipboard.getEntities().iterator();
                    if (props.hasNext()) { nextProp = props.next(); return true; }
                }
                phase++; x = 0; z = 0; markers = null;
            }
            return false;
        }
        @Override public boolean ready() {
            Location destination;
            if (phase < 2) destination = corner.clone().add(x, 0, z);
            else if (phase != 6) destination = LocationProjector.project(base, schematicOffset, nextMarker);
            else {
                var at = nextProp.getLocation();
                var minimum = schematicClipboard.getMinimumPoint();
                destination = corner.clone().add(at.getX() - minimum.x(), at.getY() - minimum.y(), at.getZ() - minimum.z());
            }
            return chunks.ready(destination);
        }
        @Override public void pasteNext() {
            switch (phase) {
                case 0 -> addPedestalColumn(corner, x, z);
                case 1 -> clearTreeColumn(corner, x, z);
                case 2 -> fillChest(nextMarker);
                case 3 -> spawnVanilla(nextMarker);
                case 4 -> spawnElite(nextMarker);
                case 5 -> spawnMythic(nextMarker);
                case 6 -> pasteProp();
                case 7 -> announceCustomMarker(nextMarker);
                default -> throw new IllegalStateException("Completed structure finishing operation");
            }
            if (phase < 2) {
                if (++z >= schematicClipboard.getDimensions().z()) { z = 0; x++; }
            }
            nextMarker = null;
            nextProp = null;
        }
        private void pasteProp() {
            if (entitySession == null) {
                entitySession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(base.getWorld()));
                entitySession.setTrackingHistory(false);
                entitySession.setSideEffectApplier(com.sk89q.worldedit.util.SideEffectSet.none());
                var minimum = schematicClipboard.getMinimumPoint();
                var origin = schematicClipboard.getOrigin();
                var destination = BlockVector3.at(corner.getBlockX(), corner.getBlockY(), corner.getBlockZ()).add(origin.subtract(minimum));
                entityCopy = new ExtentEntityCopy(origin.toVector3(), entitySession, destination.toVector3(), new AffineTransform());
            }
            try { entityCopy.apply(nextProp); }
            catch (com.sk89q.worldedit.WorldEditException failure) { throw new IllegalStateException("Could not paste structure entity", failure); }
        }
        @Override public void close() {
            try { if (entitySession != null) entitySession.close(); }
            finally { chunks.close(); }
        }
        @Override public void onComplete() {
            var region = schematicClipboard.getRegion();
            Location highestCorner = corner.clone().add(region.getWidth() - 1, region.getHeight() - 1, region.getLength() - 1);
            List<Location> containers = schematicContainer.getChestLocations().stream()
                    .map(position -> LocationProjector.project(base, schematicOffset, position)).toList();
            Bukkit.getServer().getPluginManager().callEvent(
                    new BuildPasteCompleteEvent(FitAnything.this, corner, highestCorner, containers));
            announceComplete(base);
        }
    }

    private void announceComplete(Location base) {
        if (!DefaultConfig.isNewBuildingWarn()) return;
        String type = structureType.toString().toLowerCase(Locale.ROOT).replace("_", " ");
        for (Player player : Bukkit.getOnlinePlayers())
            if (player.hasPermission("betterstructures.warn"))
                player.spigot().sendMessage(SpigotMessage.commandHoverMessage(
                        "[BetterStructures] New " + type + " building generated! Click to teleport. Do \"/betterstructures silent\" to stop getting warnings!",
                        "Click to teleport to " + base.getWorld().getName() + ", " + base.getBlockX() + ", " + base.getBlockY() + ", " + base.getBlockZ() + "\n Schem name: " + schematicContainer.getConfigFilename(),
                        "/betterstructures teleport " + base.getWorld().getName() + " " + base.getBlockX() + " " + base.getBlockY() + " " + base.getBlockZ()));
    }

    private Material getPedestalMaterial(boolean isPedestalSurface) {
        if (isPedestalSurface) {
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

    private void addPedestalColumn(Location corner, int x, int z) {
        if (corner.clone().add(x, 0, z).getBlock().getType().isAir()) return;
        for (int y = -1; y > -11; y--) {
            Block block = corner.clone().add(x, y, z).getBlock();
            if (!SurfaceMaterials.ignorable(block.getType())) break;
            block.setType(getPedestalMaterial(!block.getRelative(BlockFace.UP).getType().isSolid()));
        }
    }

    private void clearTreeColumn(Location corner, int x, int z) {
        for (int y = 0; y < 31; y++) {
            Block block = corner.clone().add(x, schematicClipboard.getDimensions().y() + 1 + y, z).getBlock();
            if (!SurfaceMaterials.ignorable(block.getType()) || block.getType().isAir()) break;
            block.setType(Material.AIR);
        }
    }

    private void fillChest(Vector chestPosition) {
        GeneratorConfigFields gen = schematicContainer.getGeneratorConfigFields();
        boolean barrelsEnabled = gen.isGenerateLootInBarrels() && schematicContainer.getBarrelContents() != null;
        boolean chestsEnabled = schematicContainer.getChestContents() != null;
        if (!barrelsEnabled && !chestsEnabled) return;

            Location chestLocation = LocationProjector.project(location, schematicOffset, chestPosition);
            if (!(chestLocation.getBlock().getState() instanceof Container container)) {
                Logger.warn("Expected a container for " + chestLocation.getBlock().getType() + " but didn't get it. Skipping this loot!");
                return;
            }

            boolean isBarrel = container.getBlock().getType() == Material.BARREL;
            if (isBarrel && !barrelsEnabled) return;
            if (!isBarrel && !chestsEnabled) return;

            ChestContents contents;
            if (isBarrel) {
                contents = schematicContainer.getBarrelContents();
            } else {
                contents = schematicContainer.getChestContents();
            }

            if (contents == null) return;
            contents.rollChestContents(container);

            ChestFillEvent chestFillEvent = new ChestFillEvent(container, contents.getTreasureConfigFields().getFilename());
            Bukkit.getServer().getPluginManager().callEvent(chestFillEvent);
            if (!chestFillEvent.isCancelled()) {
                container.update(true);
            }
    }

    private void spawnVanilla(Vector entityPosition) {
            Location signLocation = LocationProjector.project(location, schematicOffset, entityPosition).clone();
            signLocation.getBlock().setType(Material.AIR);
            //If mobs spawn in corners they might choke on adjacent walls
            signLocation.add(new Vector(0.5, 0, 0.5));
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

    private void spawnElite(Vector elitePosition) {
            Location eliteLocation = LocationProjector.project(location, schematicOffset, elitePosition).clone();
            eliteLocation.getBlock().setType(Material.AIR);
            eliteLocation.add(new Vector(0.5, 0, 0.5));
            String bossFilename = schematicContainer.getEliteMobsSpawns().get(elitePosition);
            //If the spawn fails then don't continue
            if (!EliteMobs.Spawn(eliteLocation, bossFilename)) throw new IllegalStateException("Could not spawn " + bossFilename);
            Location lowestCorner = location.clone().add(schematicOffset);
            Location highestCorner = lowestCorner.clone().add(new Vector(schematicClipboard.getRegion().getWidth() - 1, schematicClipboard.getRegion().getHeight() - 1, schematicClipboard.getRegion().getLength() - 1));
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

    /** The sign is removed first so a listener can place blocks at the marker. */
    private void announceCustomMarker(Vector position) {
        Location markerLocation = LocationProjector.project(location, schematicOffset, position).clone();
        markerLocation.getBlock().setType(Material.AIR);
        Bukkit.getServer().getPluginManager().callEvent(
                new BuildCustomMarkerEvent(this, markerLocation, schematicContainer.getCustomMarkers().get(position)));
    }

    private void spawnMythic(Vector position) {
            Location mobLocation = LocationProjector.project(location, schematicOffset, position).clone();
            mobLocation.getBlock().setType(Material.AIR);

            //If the spawn fails then don't continue
            if (!MythicMobs.Spawn(mobLocation, schematicContainer.getMythicMobsSpawns().get(position)))
                throw new IllegalStateException("Could not spawn " + schematicContainer.getMythicMobsSpawns().get(position));
    }
}
