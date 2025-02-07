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
    @Getter
//    private static final HashMap<String, Integer> tagOccurrences = new HashMap<>();
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

    public ModulesContainer(Clipboard clipboard, String clipboardFilename, ModulesConfigFields modulesConfigField, String configFilename, int rotation) {
        this.clipboard = clipboard;
        this.clipboardFilename = clipboardFilename;
        this.modulesConfigField = modulesConfigField;
        this.configFilename = configFilename;
        this.rotation = rotation;
        if (!clipboardFilename.equalsIgnoreCase("nothing") && !clipboardFilename.equalsIgnoreCase("world_border")) {
            processBorders(modulesConfigField.getBorderMap());
            modulesContainers.put(clipboardFilename + "_rotation_" + rotation, this);
        } else if (clipboardFilename.equalsIgnoreCase("nothing")) {
            nothing = true;
            modulesContainers.put(clipboardFilename, this);
        } else if (clipboardFilename.equalsIgnoreCase("world_border")) {
            modulesContainers.put(clipboardFilename, this);
            processBorders(modulesConfigField.getBorderMap());
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
//                        boolean valid = false; todo: investigate why I wrote this and if it's really necessary
                        for (NeighborTag neighborTag : neighborTags) {
                            //"nothing" is a special module, borders that share "nothing" should not be joined and instead they should only join with empty space
                            if (borderTag.getTag().equalsIgnoreCase("nothing")) {
                                modulesContainer.validBorders.computeIfAbsent(direction, k -> new HashSet<>()).add(modulesContainers.get("nothing"));
                                continue;
                            }
                            //"world_border" is a special module, borders that share "world_border" should not be joined and the only thing they should join with is spaces beyond the radius of the grid
                            if (borderTag.getTag().equalsIgnoreCase("world_border")) {
//                                modulesContainer.validBorders.computeIfAbsent(direction, k -> new HashSet<>()).add(modulesContainers.get("world_border"));
                                modulesContainer.validBorders.computeIfAbsent(direction, k -> new HashSet<>()).add(neighborContainer);
//                                Logger.debug("hit world border for build " + value.clipboardFilename);
                                modulesContainer.horizontalEdge = true;
                                continue;
                            }
                            if (borderTag.getTag().equals(neighborTag.getTag()) && (borderTag.isCanMirror() || neighborTag.isCanMirror())) {
                                modulesContainer.validBorders.computeIfAbsent(direction, k -> new HashSet<>()).add(neighborContainer);
//                                valid = true;
                                break;
                            }
                        }
//                        if (valid) break;
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
        ModulesContainer nothing = new ModulesContainer(null, "nothing", new ModulesConfigFields("nothing", true), null, 0);
        nothing.borderTags = new BorderTags(Map.of(
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
//        Logger.debug("Getting valid modules from surroundings for " + gridCell.getCellLocation());

        for (Map.Entry<Direction, GridCell> buildBorderChunkDataEntry : gridCell.getOrientedNeighbors().entrySet()) {
            Direction direction = buildBorderChunkDataEntry.getKey();
            //Handle the neighbor not being generated yet
            if (buildBorderChunkDataEntry.getValue() == null || buildBorderChunkDataEntry.getValue().getModulesContainer() == null)
                continue;

            HashSet<ModulesContainer> validBorderSpecificModules = new HashSet<>();

            //This is necessary for the edge case where there's borders and they can have a scenario where only 'nothing' can be up against them
            boolean canOnlyBeNothingOrBorder = true;

            if (buildBorderChunkDataEntry.getValue().getModulesContainer().validBorders.get(direction.getOpposite()) == null)
                Logger.debug("no valid direction " + direction.getOpposite() + " for " + buildBorderChunkDataEntry.getValue().getModulesContainer().getClipboardFilename() + " as it was " + buildBorderChunkDataEntry.getValue().getModulesContainer().validBorders);


            for (ModulesContainer modulesContainer : buildBorderChunkDataEntry.getValue().getModulesContainer().validBorders.get(direction.getOpposite())) {
                if (modulesContainer == null) {
                    Logger.debug("null module, somehow, for " + buildBorderChunkDataEntry.getValue().getModulesContainer().clipboardFilename);
                    continue;
                }

                if (isGridBorder && modulesContainer.clipboardFilename.contains("border")) Logger.debug("0");

                if (modulesContainer.isHorizontalEdge() != isGridBorder)
                    //'nothing' should be compatible anywhere
                    if (!(isGridBorder && modulesContainer.nothing))
                        continue;

                if (!isGridBorder) canOnlyBeNothingOrBorder = false;
                else if (!modulesContainer.nothing && !modulesContainer.isHorizontalEdge())
                    canOnlyBeNothingOrBorder = false;

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
                        if (directionListEntry.getKey() == direction.getOpposite()) {
                            for (NeighborTag tag : directionListEntry.getValue()) {
                                boolean isBeyondGrid = !gridCell.getWaveFunctionCollapseGenerator().getSpatialGrid().isWithinBounds(
                                        gridCell.getCellLocation().add(directionListEntry.getKey().getOffsetVector()));
                                boolean isWorldBorderTag = tag.getTag().equalsIgnoreCase("world_border");
                                if (isBeyondGrid != isWorldBorderTag) {
//                                    Logger.debug("triggered world border skip!");
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

            if (validModules.isEmpty() && canOnlyBeNothingOrBorder) {
                validModules.add(modulesContainers.get("nothing"));
            }
        }

        if (validModules == null || validModules.isEmpty()) {
            Logger.debug(gridCell.getCellLocation() + " list was empty");
            return new HashSet<>();
        }
        return validModules;
    }

    private static String debugGridCellList(HashSet<ModulesContainer> validBorderSpecificModules) {
        String string = "";
        for (ModulesContainer validBorderSpecificModule : validBorderSpecificModules) {
            string += validBorderSpecificModule.clipboardFilename + ", ";
        }
        return string;
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

    public static ModulesContainer pickRandomModule(HashSet<ModulesContainer> modulesContainerList, GridCell gridCell) {
//        Logger.debug("About to pick module");
        if (modulesContainerList == null || modulesContainerList.isEmpty()) return null;
//        Logger.debug("Picking module from " + modulesContainerList.size() + " modules");
        return pickWeightedRandomModule(modulesContainerList, gridCell);
    }

//    public static ModulesContainer pickWeightedRandomModule(List<ModulesContainer> modules,
//                                                            GridCell gridCell) {
//        Map<Integer, Double> weightMap = new HashMap<>();
//        Map<Integer, ModulesContainer> moduleMap = new HashMap<>();
//        for (int i = 0; i < modules.size(); i++) {
//            ModulesContainer modulesContainer = modules.get(i);
//            double weight = modulesContainer.getWeight();
//            if (!modulesContainer.nothing && modulesContainer.getModulesConfigField().getRepetitionPenalty() != 0) {
//                for (GridCell value : gridCell.getOrientedNeighbors().values()) {
//                    if (value != null && value.getModulesContainer() != null && modules.get(i).getClipboardFilename().equals(value.getModulesContainer().getClipboardFilename()))
//                        weight += modules.get(i).getModulesConfigField().getRepetitionPenalty();
//                }
//            }
//            weightMap.put(i, weight);
//            moduleMap.put(i, modules.get(i));
//        }
//        return moduleMap.get(WeighedProbability.pickWeightedProbability(weightMap));
//    }

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
            if (tag.equalsIgnoreCase("world_border")) {
                isWorldBorder = true;
            }
        }
    }

}