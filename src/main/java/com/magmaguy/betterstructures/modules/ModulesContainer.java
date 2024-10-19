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
    private static final HashMap<String, Integer> tagOccurrences = new HashMap<>();
    private static final List<Integer> validRotations = Arrays.asList(0, 90, 180, 270);
    @Getter
    private final Clipboard clipboard;
    @Getter
    private final String clipboardFilename;
    private final String configFilename;
    @Getter
    private final int rotation;
    private final Map<Direction, List<ModulesContainer>> validBorders = new HashMap<>();
    @Getter
    private ModulesConfigFields modulesConfigField;
    @Getter
    private BorderTags borderTags = new BorderTags(new EnumMap<>(Direction.class));
    @Getter
    private boolean nothing = false;

    public ModulesContainer(Clipboard clipboard, String clipboardFilename, ModulesConfigFields modulesConfigField, String configFilename, int rotation) {
        this.clipboard = clipboard;
        this.clipboardFilename = clipboardFilename;
        this.modulesConfigField = modulesConfigField;
        this.configFilename = configFilename;
        this.rotation = rotation;
        if (!clipboardFilename.equalsIgnoreCase("nothing"))
            processBorders(modulesConfigField.getBorderMap());
        else nothing = true;
        modulesContainers.put(clipboardFilename + "_rotation_" + rotation, this);
    }

    public static void initializeModulesContainer(Clipboard clipboard, String clipboardFilename, ModulesConfigFields modulesConfigField, String configFilename) {
        validRotations.forEach(rotation -> new ModulesContainer(clipboard, clipboardFilename, modulesConfigField, configFilename, rotation));
    }

    public static void postInitializeModulesContainer() {
        for (ModulesContainer value : modulesContainers.values()) {
            for (Map.Entry<Direction, List<NeighborTag>> buildBorderListEntry : value.borderTags.entrySet()) {
                Direction direction = buildBorderListEntry.getKey();
                List<NeighborTag> borderTags = buildBorderListEntry.getValue();
                for (ModulesContainer neighborContainer : modulesContainers.values()) {
                    List<NeighborTag> neighborTags = neighborContainer.borderTags.neighborMap.get(direction.getOpposite());
                    for (NeighborTag borderTag : borderTags) {
                        boolean valid = false;
                        for (NeighborTag neighborTag : neighborTags) {
                            if (borderTag.getTag().equals(neighborTag.getTag()) && (borderTag.isCanMirror() || neighborTag.isCanMirror())) {
                                value.validBorders.computeIfAbsent(direction, k -> new ArrayList<>()).add(neighborContainer);
                                valid = true;
                                break;
                            }
                        }
                        if (valid) break;
                    }
                }
            }
            for (Direction direction : Direction.values()) {
                if (value.validBorders.get(direction) == null || value.validBorders.get(direction).isEmpty()) {
                    Logger.warn("No valid neighbors for " + value.getClipboardFilename() + " in direction " + direction);
                    break;
                }
            }
        }
    }

    public static void initialize() {
        //Initialize "nothing", a reserved name with special behavior
        ModulesContainer nothing = new ModulesContainer(null, "nothing", null, null, 0);
        nothing.borderTags = new BorderTags(Map.of(
                Direction.NORTH, Collections.singletonList(new NeighborTag("nothing")),
                Direction.SOUTH, Collections.singletonList(new NeighborTag("nothing")),
                Direction.EAST, Collections.singletonList(new NeighborTag("nothing")),
                Direction.WEST, Collections.singletonList(new NeighborTag("nothing")),
                Direction.UP, Collections.singletonList(new NeighborTag("nothing")),
                Direction.DOWN, Collections.singletonList(new NeighborTag("nothing"))));
        nothing.modulesConfigField = new ModulesConfigFields("nothing", true);
        tagOccurrences.forEach((key, value) -> {
            if (value < 2)
                Logger.warn("Tag " + key + " is only ever present once, which means it will never be valid and get the system stuck!");
        });
    }

    public static void shutdown() {
        modulesContainers.clear();
        tagOccurrences.clear();
    }

    public static List<ModulesContainer> getValidModulesFromSurroundings(GridCell gridCell) {
        List<ModulesContainer> validModules = null;

        for (Map.Entry<Direction, GridCell> buildBorderChunkDataEntry : gridCell.getOrientedNeighbours().entrySet()) {
            Direction direction = buildBorderChunkDataEntry.getKey();
            //Handle the neighbor not being generated yet
            if (buildBorderChunkDataEntry.getValue() == null || buildBorderChunkDataEntry.getValue().getModulesContainer() == null)
                continue;
            List<ModulesContainer> validBorderSpecificModules = new ArrayList<>();

            if (buildBorderChunkDataEntry.getValue().getModulesContainer().validBorders.get(direction.getOpposite()) == null)
                Logger.debug("no valid direction " + direction.getOpposite() + " for " + buildBorderChunkDataEntry.getValue().getModulesContainer().getClipboardFilename() + " as it was " + buildBorderChunkDataEntry.getValue().getModulesContainer().validBorders);

            for (ModulesContainer modulesContainer : buildBorderChunkDataEntry.getValue().getModulesContainer().validBorders.get(direction.getOpposite())) {
                boolean repeatStop = false;
                for (GridCell neighbourData : gridCell.getOrientedNeighbours().values()) {
                    if (neighbourData == null || neighbourData.getModulesContainer() == null) continue;
                    if (!modulesContainer.nothing && modulesContainer.getModulesConfigField().isNoRepeat() &&
                            neighbourData.getModulesContainer().getClipboardFilename().equals(modulesContainer.getClipboardFilename())) {
                        repeatStop = true;
                        break;
                    }
                }

                if (repeatStop) continue;

                if (!checkVerticalRotationValidity(direction, buildBorderChunkDataEntry.getValue().getModulesContainer(), modulesContainer) ||
                        !checkHorizontalRotationValidity(direction, buildBorderChunkDataEntry.getValue().getModulesContainer(), modulesContainer)) {
                    continue;
                }

                Vector3i loc = gridCell.getChunkLocation();
                if (loc.y < modulesContainer.modulesConfigField.getMinY() ||
                        loc.y > modulesContainer.modulesConfigField.getMaxY()) {
                    continue;
                }

                validBorderSpecificModules.add(modulesContainer);
            }

            if (validModules == null) {
                validModules = new ArrayList<>(validBorderSpecificModules);
            } else {
                if (!validBorderSpecificModules.isEmpty())
                    validModules.retainAll(validBorderSpecificModules);
            }
        }

        if (validModules == null || validModules.isEmpty()) {
            return new ArrayList<>();
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

    public static ModulesContainer pickRandomModuleFromSurroundings(GridCell gridCell) {
        List<ModulesContainer> validModules = getValidModulesFromSurroundings(gridCell);
        if (validModules == null) return null;
        return pickWeightedRandomModule(validModules, gridCell);
    }

    public static ModulesContainer pickRandomModule(List<ModulesContainer> modulesContainerList, GridCell gridCell) {
        if (modulesContainerList == null || modulesContainerList.isEmpty()) return null;
        return pickWeightedRandomModule(modulesContainerList, gridCell);
    }


    public static ModulesContainer pickWeightedRandomModule(List<ModulesContainer> modules,
                                                            GridCell gridCell) {
        Map<Integer, Double> weightMap = new HashMap<>();
        Map<Integer, ModulesContainer> moduleMap = new HashMap<>();
        for (int i = 0; i < modules.size(); i++) {
            ModulesContainer modulesContainer = modules.get(i);
            double weight = modulesContainer.getWeight();
            if (!modulesContainer.nothing && modulesContainer.getModulesConfigField().getRepetitionPenalty() != 0) {
                for (GridCell value : gridCell.getOrientedNeighbours().values()) {
                    if (value.getModulesContainer() != null && modules.get(i).getClipboardFilename().equals(value.getModulesContainer().getClipboardFilename()))
                        weight += modules.get(i).getModulesConfigField().getRepetitionPenalty();
                }
            }
            weightMap.put(i, weight);
            moduleMap.put(i, modules.get(i));
        }
        return moduleMap.get(WeighedProbability.pickWeightedProbability(weightMap));
    }

    private double getWeight() {
        if (nothing) return 50;
        else return modulesConfigField.getWeight();
    }


    private void processBorders(Map<String, Object> borderMap) {
        for (Map.Entry<String, Object> entry : borderMap.entrySet()) {
            String direction = entry.getKey();
            List<String> borderList = processBorderList(entry.getValue());
            List<NeighborTag> processedBorderList = new ArrayList<>();
            for (String tag : borderList) {
                processedBorderList.add(new NeighborTag(tag));
            }
            Direction border = Direction.fromString(direction);
            if (border == null) {
                Logger.warn("Invalid border " + direction + " for module " + configFilename);
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

    private List<String> processBorderList(Object rawBorderList) {
        List<String> stringList = (List<String>) rawBorderList;
        for (String string : stringList) {
            // Increment the occurrence count for the string
            tagOccurrences.put(string, tagOccurrences.getOrDefault(string, 0) + 1);
        }

        return stringList;
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

        public NeighborTag(String tag) {
            this.tag = tag;
            if (tag.contains("no-mirror_")) {
                canMirror = false;
                this.tag = this.tag.replace("no-mirror_", "");
            }
        }
    }

}