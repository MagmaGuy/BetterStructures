package com.magmaguy.betterstructures.modules;

import com.google.gson.Gson;
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
    @Getter
    private final ModulesConfigFields modulesConfigField;
    private final String configFilename;
    @Getter
    private final int rotation;
    @Getter
    private BorderTags borderTags = new BorderTags(new EnumMap<>(BuildBorder.class));
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

    public static void initialize() {
        //Initialize "nothing", a reserved name with special behavior
        ModulesContainer nothing = new ModulesContainer(null, "nothing", null, null, 0);
        nothing.borderTags = new BorderTags(Map.of(
                BuildBorder.NORTH, Collections.singletonList(new NeighborTag("nothing")),
                BuildBorder.SOUTH, Collections.singletonList(new NeighborTag("nothing")),
                BuildBorder.EAST, Collections.singletonList(new NeighborTag("nothing")),
                BuildBorder.WEST, Collections.singletonList(new NeighborTag("nothing")),
                BuildBorder.UP, Collections.singletonList(new NeighborTag("nothing")),
                BuildBorder.DOWN, Collections.singletonList(new NeighborTag("nothing"))));
        tagOccurrences.forEach((key, value) -> {
            if (value < 2)
                Logger.warn("Tag " + key + " is only ever present once, which means it will never be valid and get the system stuck!");
        });
    }

    public static void shutdown() {
        modulesContainers.clear();
        tagOccurrences.clear();
    }

    public static List<PastableModulesContainer> getValidModulesFromSurroundings(ChunkData chunkData) {
        List<PastableModulesContainer> validModules = new ArrayList<>();

        for (ModulesContainer module : modulesContainers.values()) {
            Vector3i loc = chunkData.getChunkLocation();
            if (!module.nothing)
                if (loc.y < module.modulesConfigField.getMinY() ||
                        loc.y > module.modulesConfigField.getMaxY())
                    continue;

            boolean repeatStop = false;
            for (ChunkData neighbourData : chunkData.getOrientedNeighbours().values()) {
                if (neighbourData.getModulesContainer() == null) continue;
                if (!module.nothing && module.getModulesConfigField().isNoRepeat() &&
                        neighbourData.getModulesContainer().getClipboardFilename().equals(module.getClipboardFilename())) {
                    repeatStop = true;
                    break;
                }
            }
            if (repeatStop) continue;

            boolean isValid = true;

            for (Map.Entry<BuildBorder, ChunkData> buildBorderChunkDataEntry : chunkData.getOrientedNeighbours().entrySet()) {
                BuildBorder direction = buildBorderChunkDataEntry.getKey();
                ChunkData iteratedeNeighbourChunkData = buildBorderChunkDataEntry.getValue();
                if (!iteratedeNeighbourChunkData.isGenerated()) continue;

                //check rotation enforcement
                if (!checkVerticalRotationValidity(direction, module, iteratedeNeighbourChunkData.getModulesContainer()) ||
                        !checkHorizontalRotationValidity(direction, module, iteratedeNeighbourChunkData.getModulesContainer())) {
                    isValid = false;
                    break;
                }

                List<NeighborTag> neighborTags = iteratedeNeighbourChunkData.getModulesContainer().getBorderTags().neighborMap.get(direction.getOpposite());

                // Get the module's border tags for the transformed direction
                List<NeighborTag> moduleTags = module.getBorderTags().neighborMap.get(direction);

                boolean commonTags = false;
                for (NeighborTag neighborTag : neighborTags) {
                    if (commonTags) break;
                    String neighborTagString = neighborTag.getTag();
                    for (NeighborTag moduleTag : moduleTags) {
                        String moduleTagString = moduleTag.getTag();
                        if (neighborTagString.equals(moduleTagString) && (neighborTag.isCanMirror() || moduleTag.isCanMirror())
                        ) {
                            commonTags = true;
                            break;
                        }
                    }
                }

                if (!commonTags) {
                    isValid = false;
                    break;
                }
            }

            if (isValid) {
                validModules.add(new PastableModulesContainer(module, module.rotation));
//                    Logger.debug("Adding module " + module.getClipboardFilename());
//                    Logger.debug("Module borders: " + new Gson().toJson(module.getBorderTags()));
            }
        }
        return validModules;
    }

    private static boolean checkVerticalRotationValidity(BuildBorder direction, ModulesContainer module, ModulesContainer neighbour) {
        if (direction != BuildBorder.UP && direction != BuildBorder.DOWN) return true;
        if (module.nothing || neighbour.nothing) return true;
        if (!neighbour.modulesConfigField.isEnforceVerticalRotation() && !module.modulesConfigField.isEnforceVerticalRotation())
            return true;
        return module.rotation == neighbour.rotation;
    }

    private static boolean checkHorizontalRotationValidity(BuildBorder direction, ModulesContainer module, ModulesContainer neighbour) {
        if (direction == BuildBorder.UP || direction == BuildBorder.DOWN) return true;
        if (module.nothing || neighbour.nothing) return true;
        if (!module.modulesConfigField.isEnforceHorizontalRotation() &&
                !neighbour.modulesConfigField.isEnforceHorizontalRotation())
            return true;
        else
            return module.rotation == neighbour.rotation;
    }

    public static PastableModulesContainer pickRandomModuleFromSurroundings(ChunkData chunkData) {
        List<PastableModulesContainer> validModules = getValidModulesFromSurroundings(chunkData);
        if (validModules.isEmpty()) return null;
        return pickWeightedRandomModule(validModules, chunkData);
    }

    public static PastableModulesContainer pickWeightedRandomModule(List<PastableModulesContainer> modules,
                                                                    ChunkData chunkData) {
        Map<Integer, Double> weightMap = new HashMap<>();
        Map<Integer, PastableModulesContainer> moduleMap = new HashMap<>();
        for (int i = 0; i < modules.size(); i++) {
            ModulesContainer modulesContainer = modules.get(i).modulesContainer();
            double weight =modulesContainer.getWeight();
            if (!modulesContainer.nothing && modulesContainer.getModulesConfigField().getRepetitionPenalty() != 0) {
                for (ChunkData value : chunkData.getOrientedNeighbours().values()) {
                    if (value.getModulesContainer() != null && modules.get(i).modulesContainer.getClipboardFilename().equals(value.getModulesContainer().getClipboardFilename()))
                        weight += modules.get(i).modulesContainer().getModulesConfigField().getRepetitionPenalty();
                }
            }
            weightMap.put(i, weight);
            moduleMap.put(i, modules.get(i));
        }
        return moduleMap.get(WeighedProbability.pickWeightedProbability(weightMap));
    }

    private double getWeight(){
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
            BuildBorder border = BuildBorder.fromString(direction);
            if (border == null) {
                Logger.warn("Invalid border " + direction + " for module " + configFilename);
                continue;
            }

            borderTags.put(BuildBorder.transformDirection(border, rotation), processedBorderList);
        }

        // Check for missing borders
        for (BuildBorder border : BuildBorder.values()) {
            if (!borderTags.containsKey(border)) {
                Logger.warn("Failed to get module border " + border.toString() + " for module " + configFilename);
            }
        }

//        Logger.debug("Finished initializing module " + clipboardFilename + " with rotation " + rotation + " now its " + new Gson().toJson(borderTags));
    }

    private List<String> processBorderList(Object rawBorderList) {
        List<String> stringList = (List<String>) rawBorderList;
        for (String string : stringList) {
            // Increment the occurrence count for the string
            tagOccurrences.put(string, tagOccurrences.getOrDefault(string, 0) + 1);
        }

        return stringList;
    }

    public record BorderTags(Map<BuildBorder, List<NeighborTag>> neighborMap) {
        public void put(BuildBorder direction, List<NeighborTag> tags) {
            neighborMap.put(direction, tags);
        }

        public boolean containsKey(BuildBorder direction) {
            return neighborMap.containsKey(direction);
        }

        public Set<Map.Entry<BuildBorder, List<NeighborTag>>> entrySet() {
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

    public record PastableModulesContainer(ModulesContainer modulesContainer, Integer rotation) {
    }

}