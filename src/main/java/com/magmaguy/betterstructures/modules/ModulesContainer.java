package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.config.modules.ModulesConfigFields;
import com.magmaguy.betterstructures.util.WeighedProbability;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;
import org.joml.Vector3i;

import java.util.*;

public class ModulesContainer {

    @Getter
    private static final HashMap<String, ModulesContainer> modulesContainers = new HashMap<>();
    private static final List<Integer> validRotations = Arrays.asList(0, 90, 180, 270);
    @Getter
    private final Clipboard clipboard;
    @Getter
    private final String clipboardFilename;
    private final String configFilename;
    @Getter
    private final int rotation;
    private final Map<Direction, HashSet<ModulesContainer>> validBorders = new HashMap<>();
    @Getter
    private final ModulesConfigFields modulesConfigField;
    @Getter
    private BorderTags borderTags = new BorderTags(new EnumMap<>(Direction.class));
    @Getter
    private boolean nothing = false;
    @Getter
    private boolean horizontalEdge = false;
    private static final String WORLD_BORDER = "world_border";
    public static ModulesContainer nothingContainer;

    public ModulesContainer(Clipboard clipboard, String clipboardFilename, ModulesConfigFields modulesConfigField, String configFilename, int rotation) {
        this.clipboard = clipboard;
        this.clipboardFilename = clipboardFilename;
        this.modulesConfigField = modulesConfigField;
        this.configFilename = configFilename;
        this.rotation = rotation;
        if (!clipboardFilename.equalsIgnoreCase("nothing")) {
            processBorders(modulesConfigField.getBorderMap());
            modulesContainers.put(clipboardFilename + "_rotation_" + rotation, this);
        } else {
            nothing = true;
            modulesContainers.put(clipboardFilename, this);
        }
    }

    public static void initializeModulesContainer(Clipboard clipboard, String clipboardFilename, ModulesConfigFields modulesConfigField, String configFilename) {
        validRotations.forEach(rotation -> new ModulesContainer(clipboard, clipboardFilename, modulesConfigField, configFilename, rotation));
    }

    public static void postInitializeModulesContainer() {
        for (ModulesContainer modulesContainer : modulesContainers.values()) {
            for (Map.Entry<Direction, List<NeighborTag>> buildBorderListEntry : modulesContainer.borderTags.entrySet()) {
                Direction direction = buildBorderListEntry.getKey();
                List<NeighborTag> borderTags = buildBorderListEntry.getValue();

                for (ModulesContainer neighborContainer : modulesContainers.values()) {
                    List<NeighborTag> neighborTags = neighborContainer.borderTags.neighborMap.get(direction.getOpposite());
                    for (NeighborTag borderTag : borderTags) {
                        for (NeighborTag neighborTag : neighborTags) {
                            //"nothing" is a special module, borders that share "nothing" should not be joined and instead they should only join with empty space
                            if (borderTag.getTag().equalsIgnoreCase("nothing")) {
                                modulesContainer.validBorders.computeIfAbsent(direction, k -> new HashSet<>()).add(modulesContainers.get("nothing"));
                                nothingContainer.validBorders.computeIfAbsent(direction.getOpposite(), k -> new HashSet<>()).add(modulesContainer);
                                continue;
                            }
                            //"world_border" is a special module, borders that share "world_border" should not be joined and the only thing they should join with is spaces beyond the radius of the grid
                            if (borderTag.getTag().equalsIgnoreCase(WORLD_BORDER)) {
                                modulesContainer.validBorders.computeIfAbsent(direction, k -> new HashSet<>()).add(neighborContainer);
                                modulesContainer.horizontalEdge = true;
                                continue;
                            }
                            if (borderTag.getTag().equals(neighborTag.getTag()) && (borderTag.isCanMirror() || neighborTag.isCanMirror())) {
                                modulesContainer.validBorders.computeIfAbsent(direction, k -> new HashSet<>()).add(neighborContainer);
                                break;
                            }
                        }
                    }
                }
            }
            for (Direction direction : Direction.values()) {
                if (!modulesContainer.horizontalEdge && (modulesContainer.validBorders.get(direction) == null || modulesContainer.validBorders.get(direction).isEmpty())) {
                    Logger.warn("No valid neighbors for " + modulesContainer.getClipboardFilename() + " in direction " + direction);
                    break;
                }
            }
        }
    }

    public static void initializeSpecialModules() {
        //Initialize "nothing", a reserved name with special behavior
        nothingContainer = new ModulesContainer(null, "nothing", new ModulesConfigFields("nothing", true), null, 0);
        nothingContainer.borderTags = new BorderTags(Map.of(
                Direction.NORTH, Collections.singletonList(new NeighborTag("nothing")),
                Direction.SOUTH, Collections.singletonList(new NeighborTag("nothing")),
                Direction.EAST, Collections.singletonList(new NeighborTag("nothing")),
                Direction.WEST, Collections.singletonList(new NeighborTag("nothing")),
                Direction.UP, Collections.singletonList(new NeighborTag("nothing")),
                Direction.DOWN, Collections.singletonList(new NeighborTag("nothing"))));
    }

    public static void shutdown() {
        modulesContainers.clear();
    }

    public static HashSet<ModulesContainer> getValidModulesFromSurroundings(GridCell gridCell) {
        HashSet<ModulesContainer> validModules = null;
        boolean isGridBorder = gridCell.getWaveFunctionCollapseGenerator().getSpatialGrid().isBorder(gridCell.getCellLocation());

        for (Map.Entry<Direction, GridCell> buildBorderChunkDataEntry : gridCell.getOrientedNeighbors().entrySet()) {
            Direction direction = buildBorderChunkDataEntry.getKey();
            //Handle the neighbor not being generated yet
            if (buildBorderChunkDataEntry.getValue() == null || buildBorderChunkDataEntry.getValue().getModulesContainer() == null)
                continue;

            HashSet<ModulesContainer> validBorderSpecificModules = new HashSet<>();

            for (ModulesContainer modulesContainer : buildBorderChunkDataEntry.getValue().getModulesContainer().validBorders.get(direction.getOpposite())) {
                if (modulesContainer == null) {
//                    Logger.debug("null module, somehow, for " + buildBorderChunkDataEntry.getValue().getModulesContainer().clipboardFilename);
                    continue;
                }

                if (!modulesContainer.getModulesConfigField().isAutomaticallyPlaced()) continue;

                if (modulesContainer.isHorizontalEdge() != isGridBorder)
                    //'nothing' should be compatible anywhere
                    if (!(isGridBorder && modulesContainer.nothing)) {
//                        Logger.debug("Prevented placement of border in non-border zone");
                        continue;
                    }

                boolean repeatStop = false;
                for (GridCell neighbourData : gridCell.getOrientedNeighbors().values()) {
                    if (neighbourData == null || neighbourData.getModulesContainer() == null) continue;
                    if (!modulesContainer.nothing &&
                            modulesContainer.getModulesConfigField().isNoRepeat() &&
                            neighbourData.getModulesContainer().getModulesConfigField().getUuid().equals(modulesContainer.getModulesConfigField().getUuid())) {
                        repeatStop = true;
                        break;
                    }
                }

                if (repeatStop) continue;

                boolean worldBorderFacesTheOutside = true;

                //If it's on the border, check if world_border is facing towards the outside
                if (isGridBorder) {
                    for (Map.Entry<Direction, List<NeighborTag>> directionListEntry : modulesContainer.getBorderTags().entrySet()) {
                        Direction checkDirection = directionListEntry.getKey();
                        // We only need to check directions that point outward from the grid
                        boolean isOutwardDirection = false;

                        // Determine if this direction points outward based on cell position
                        Vector3i pos = gridCell.getCellLocation();
                        if ((pos.x == -gridCell.getWaveFunctionCollapseGenerator().getSpatialGrid().getGridRadius() && checkDirection == Direction.WEST) ||
                                (pos.x == gridCell.getWaveFunctionCollapseGenerator().getSpatialGrid().getGridRadius() && checkDirection == Direction.EAST) ||
                                (pos.z == -gridCell.getWaveFunctionCollapseGenerator().getSpatialGrid().getGridRadius() && checkDirection == Direction.NORTH) ||
                                (pos.z == gridCell.getWaveFunctionCollapseGenerator().getSpatialGrid().getGridRadius() && checkDirection == Direction.SOUTH)) {
                            isOutwardDirection = true;
                        }

                        // Only validate world border tags for directions that point outward
                        if (isOutwardDirection) {
                            for (NeighborTag tag : directionListEntry.getValue()) {
                                boolean isWorldBorderTag = tag.getTag().equalsIgnoreCase(WORLD_BORDER);
                                // For outward directions, we expect world_border tags
                                if (!isWorldBorderTag) {
                                    worldBorderFacesTheOutside = false;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (!worldBorderFacesTheOutside) continue;

                if (!checkVerticalRotationValidity(direction, buildBorderChunkDataEntry.getValue().getModulesContainer(), modulesContainer) ||
                        !checkHorizontalRotationValidity(direction, buildBorderChunkDataEntry.getValue().getModulesContainer(), modulesContainer)) {
                    continue;
                }

                Vector3i loc = gridCell.getCellLocation();
                if (loc.y < modulesContainer.modulesConfigField.getMinY() ||
                        loc.y > modulesContainer.modulesConfigField.getMaxY()) {
                    continue;
                }

                validBorderSpecificModules.add(modulesContainer);
            }

            if (validModules == null) {
                validModules = new HashSet<>(validBorderSpecificModules);
            } else {
                if (!validBorderSpecificModules.isEmpty())
                    validModules.retainAll(validBorderSpecificModules);
            }
        }

        if (validModules == null || validModules.isEmpty()) {
//            Logger.debug(gridCell.getCellLocation() + " list was empty");
            return new HashSet<>();
        }
        return validModules;
    }

    private static boolean checkVerticalRotationValidity(Direction direction, ModulesContainer module, ModulesContainer neighbour) {
        if (direction != Direction.UP && direction != Direction.DOWN) return true;
        if (module.nothing || neighbour.nothing) return true;
        if (!neighbour.modulesConfigField.isEnforceVerticalRotation() && !module.modulesConfigField.isEnforceVerticalRotation())
            return true;
        return module.rotation == neighbour.rotation;
    }

    private static boolean checkHorizontalRotationValidity(Direction direction, ModulesContainer module, ModulesContainer neighbour) {
        if (direction == Direction.UP || direction == Direction.DOWN) return true;
        if (module.nothing || neighbour.nothing) return true;
        if (!module.modulesConfigField.isEnforceHorizontalRotation() &&
                !neighbour.modulesConfigField.isEnforceHorizontalRotation())
            return true;
        else
            return module.rotation == neighbour.rotation;
    }

    public static ModulesContainer pickWeightedRandomModule(HashSet<ModulesContainer> modules, GridCell gridCell) {
        Map<Integer, Double> weightMap = new HashMap<>();
        Map<Integer, ModulesContainer> moduleMap = new HashMap<>();
        int index = 0;

        for (ModulesContainer modulesContainer : modules) {
            double weight = modulesContainer.getWeight();
            if (!modulesContainer.nothing && modulesContainer.getModulesConfigField().getRepetitionPenalty() != 0) {
                for (GridCell value : gridCell.getOrientedNeighbors().values()) {
                    if (value != null && value.getModulesContainer() != null && modulesContainer.getClipboardFilename().equals(value.getModulesContainer().getClipboardFilename())) {
                        weight += modulesContainer.getModulesConfigField().getRepetitionPenalty();
                    }
                }
            }
            weightMap.put(index, weight);
            moduleMap.put(index, modulesContainer);
            index++;
        }

        return moduleMap.get(WeighedProbability.pickWeightedProbability(weightMap));
    }


    private double getWeight() {
        if (nothing) return 50;
        else return modulesConfigField.getWeight();
    }


    private void processBorders(Map<String, Object> borderMap) {
        for (Map.Entry<String, Object> entry : borderMap.entrySet()) {
            List<NeighborTag> processedBorderList = new ArrayList<>();
            for (String tag : (List<String>) (entry.getValue())) {
                processedBorderList.add(new NeighborTag(tag));
            }
            Direction border = Direction.fromString(entry.getKey());
            if (border == null) {
                Logger.warn("Invalid border " + entry.getKey() + " for module " + configFilename);
                continue;
            }

            borderTags.put(Direction.transformDirection(border, rotation), processedBorderList);
        }

        // Check for missing borders
        for (Direction border : Direction.values()) {
            if (!borderTags.containsKey(border)) {
                Logger.warn("Failed to get module border " + border.toString() + " for module " + configFilename);
            }
        }

    }

    public record BorderTags(Map<Direction, List<NeighborTag>> neighborMap) {
        public void put(Direction direction, List<NeighborTag> tags) {
            neighborMap.put(direction, tags);
        }

        public boolean containsKey(Direction direction) {
            return neighborMap.containsKey(direction);
        }

        public Set<Map.Entry<Direction, List<NeighborTag>>> entrySet() {
            return neighborMap.entrySet();
        }

        public Collection<List<NeighborTag>> values() {
            return neighborMap.values();
        }
    }

    @Getter
    public static class NeighborTag {
        private String tag;
        private boolean canMirror = true;
        private boolean isWorldBorder = false;

        public NeighborTag(String tag) {
            this.tag = tag;
            if (tag.contains("no-mirror_")) {
                canMirror = false;
                this.tag = this.tag.replace("no-mirror_", "");
            }
            if (tag.equalsIgnoreCase(WORLD_BORDER)) {
                isWorldBorder = true;
            }
        }
    }

}