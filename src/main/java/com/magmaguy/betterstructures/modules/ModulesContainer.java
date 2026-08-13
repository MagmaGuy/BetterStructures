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

                for (NeighborTag borderTag : borderTags) {
                    // "nothing" and "world_border" both face the pre-collapsed
                    // sentinel outside the placeable lattice. Register both sides
                    // explicitly so compatibility remains reciprocal.
                    if (borderTag.getTag().equalsIgnoreCase("nothing") || borderTag.isWorldBorder()) {
                        modulesContainer.validBorders.computeIfAbsent(direction, k -> new HashSet<>()).add(nothingContainer);
                        nothingContainer.validBorders.computeIfAbsent(direction.getOpposite(), k -> new HashSet<>()).add(modulesContainer);
                        if (borderTag.isWorldBorder()) modulesContainer.horizontalEdge = true;
                        continue;
                    }

                    for (ModulesContainer neighborContainer : modulesContainers.values()) {
                        List<NeighborTag> neighborTags = neighborContainer.borderTags.neighborMap.get(direction.getOpposite());
                        if (neighborTags == null) continue;
                        for (NeighborTag neighborTag : neighborTags) {
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

    public static HashSet<ModulesContainer> getValidModulesFromSurroundings(WFCNode WFCNode) {
        HashSet<ModulesContainer> validModules = null;
        //getCellLocation() copies defensively, so grab it once per call instead of per candidate
        Vector3i cellLocation = WFCNode.getCellLocation();
        int placeableEdgeRadius = WFCNode.getWfcGenerator().getSpatialGrid().getLatticeRadius() - 1;
        boolean isPlaceableEdge = Math.abs(cellLocation.x) == placeableEdgeRadius ||
                Math.abs(cellLocation.z) == placeableEdgeRadius;

        for (Map.Entry<Direction, WFCNode> buildBorderChunkDataEntry : WFCNode.getOrientedNeighbors().entrySet()) {
            Direction direction = buildBorderChunkDataEntry.getKey();
            //Handle the neighbor not being generated yet
            if (buildBorderChunkDataEntry.getValue() == null || buildBorderChunkDataEntry.getValue().getModulesContainer() == null)
                continue;

            HashSet<ModulesContainer> validBorderSpecificModules = new HashSet<>();

            Set<ModulesContainer> neighborCompatibleModules = buildBorderChunkDataEntry.getValue()
                    .getModulesContainer().validBorders.get(direction.getOpposite());
            if (neighborCompatibleModules == null) neighborCompatibleModules = Collections.emptySet();
            for (ModulesContainer modulesContainer : neighborCompatibleModules) {
                if (modulesContainer == null) {
                    continue;
                }

                if (!modulesContainer.getModulesConfigField().isAutomaticallyPlaced()) continue;

                if (modulesContainer.isHorizontalEdge() != isPlaceableEdge)
                    //'nothing' should be compatible anywhere
                    if (!(isPlaceableEdge && modulesContainer.nothing)) {
                        continue;
                    }

                boolean repeatStop = false;
                for (WFCNode neighbourData : WFCNode.getOrientedNeighbors().values()) {
                    if (neighbourData == null || neighbourData.getModulesContainer() == null) continue;
                    if (!modulesContainer.nothing &&
                            modulesContainer.getModulesConfigField().isNoRepeat() &&
                            neighbourData.getModulesContainer().getModulesConfigField().getUuid().equals(modulesContainer.getModulesConfigField().getUuid())) {
                        repeatStop = true;
                        break;
                    }
                }

                if (repeatStop) continue;

                if (isPlaceableEdge && !modulesContainer.nothing &&
                        !hasRequiredWorldBorderFaces(modulesContainer, cellLocation, placeableEdgeRadius)) continue;

                if (!checkVerticalRotationValidity(direction, buildBorderChunkDataEntry.getValue().getModulesContainer(), modulesContainer) ||
                        !checkHorizontalRotationValidity(direction, buildBorderChunkDataEntry.getValue().getModulesContainer(), modulesContainer)) {
                    continue;
                }

                if (cellLocation.y < modulesContainer.modulesConfigField.getMinY() ||
                        cellLocation.y > modulesContainer.modulesConfigField.getMaxY()) {
                    continue;
                }

                validBorderSpecificModules.add(modulesContainer);
            }

            if (validModules == null) validModules = new HashSet<>(validBorderSpecificModules);
            else validModules.retainAll(validBorderSpecificModules);
        }

        if (validModules == null || validModules.isEmpty()) {
            return new HashSet<>();
        }
        return validModules;
    }

    private static boolean hasRequiredWorldBorderFaces(ModulesContainer modulesContainer, Vector3i location,
                                                        int placeableEdgeRadius) {
        if (location.x == -placeableEdgeRadius && !hasOnlyWorldBorderTags(modulesContainer, Direction.WEST))
            return false;
        if (location.x == placeableEdgeRadius && !hasOnlyWorldBorderTags(modulesContainer, Direction.EAST))
            return false;
        if (location.z == -placeableEdgeRadius && !hasOnlyWorldBorderTags(modulesContainer, Direction.NORTH))
            return false;
        return location.z != placeableEdgeRadius || hasOnlyWorldBorderTags(modulesContainer, Direction.SOUTH);
    }

    private static boolean hasOnlyWorldBorderTags(ModulesContainer modulesContainer, Direction direction) {
        List<NeighborTag> tags = modulesContainer.borderTags.neighborMap.get(direction);
        return tags != null && !tags.isEmpty() && tags.stream().allMatch(NeighborTag::isWorldBorder);
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

    public static ModulesContainer pickWeightedRandomModule(HashSet<ModulesContainer> modules, WFCNode WFCNode) {
        Map<Integer, Double> weightMap = new HashMap<>();
        Map<Integer, ModulesContainer> moduleMap = new HashMap<>();
        int index = 0;

        for (ModulesContainer modulesContainer : modules) {
            double weight = modulesContainer.getWeight();
            if (!modulesContainer.nothing && modulesContainer.getModulesConfigField().getRepetitionPenalty() != 0) {
                for (WFCNode value : WFCNode.getOrientedNeighbors().values()) {
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
            isWorldBorder = this.tag.equalsIgnoreCase(WORLD_BORDER);
        }
    }

}
