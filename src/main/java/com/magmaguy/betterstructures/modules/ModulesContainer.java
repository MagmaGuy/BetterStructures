package com.magmaguy.betterstructures.modules;

import com.google.gson.Gson;
import com.magmaguy.betterstructures.config.modules.ModulesConfigFields;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import lombok.Getter;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ModulesContainer {

    @Getter
    private static final HashMap<String, ModulesContainer> modulesContainers = new HashMap<>();

    @Getter
    private static final HashMap<String, Integer> tagOccurrences = new HashMap<>();
    @Getter
    private static final HashMap<String, HashSet<ModulesContainer>> sideTags = new HashMap<>();
    @Getter
    private static final HashMap<String, HashSet<ModulesContainer>> topTags = new HashMap<>();
    @Getter
    private static final HashMap<String, HashSet<ModulesContainer>> bottomTags = new HashMap<>();
    private static final List<Integer> validRotations = Arrays.asList(0, 90, 180, 270);
    @Getter
    private final Clipboard clipboard;
    @Getter
    private final String clipboardFilename;
    private final ModulesConfigFields modulesConfigField;
    private final String configFilename;
    @Getter
    private final BorderTags borderTags = new BorderTags(new EnumMap<>(ModulesConfigFields.BuildBorder.class));

    public ModulesContainer(Clipboard clipboard, String clipboardFilename, ModulesConfigFields modulesConfigField, String configFilename) {
        this.clipboard = clipboard;
        this.clipboardFilename = clipboardFilename;
        this.modulesConfigField = modulesConfigField;
        this.configFilename = configFilename;
        processBorders(modulesConfigField.getBorderMap());
        modulesContainers.put(clipboardFilename, this);
    }

    public static List<String> getAllTags() {
        return new ArrayList<>(tagOccurrences.keySet());
    }

    public static void shutdown() {
        modulesContainers.clear();
        tagOccurrences.clear();
        sideTags.clear();
        topTags.clear();
        bottomTags.clear();
    }

    public static ModulesConfigFields.BuildBorder transformDirection(ModulesConfigFields.BuildBorder direction, Integer rotation) {
        return switch (rotation % 360) {
            case 90 -> switch (direction) {
                case NORTH -> ModulesConfigFields.BuildBorder.WEST;
                case EAST -> ModulesConfigFields.BuildBorder.NORTH;
                case SOUTH -> ModulesConfigFields.BuildBorder.EAST;
                case WEST -> ModulesConfigFields.BuildBorder.SOUTH;
                default -> direction;
            };
            case 180 -> switch (direction) {
                case NORTH -> ModulesConfigFields.BuildBorder.SOUTH;
                case EAST -> ModulesConfigFields.BuildBorder.WEST;
                case SOUTH -> ModulesConfigFields.BuildBorder.NORTH;
                case WEST -> ModulesConfigFields.BuildBorder.EAST;
                default -> direction;
            };
            case 270 -> switch (direction) {
                case NORTH -> ModulesConfigFields.BuildBorder.EAST;
                case EAST -> ModulesConfigFields.BuildBorder.SOUTH;
                case SOUTH -> ModulesConfigFields.BuildBorder.WEST;
                case WEST -> ModulesConfigFields.BuildBorder.NORTH;
                default -> direction;
            };
            default -> direction; // 0 degrees or full rotation or up/down
        };
    }

    public static List<PastableModulesContainer> getValidModulesFromSurroundings(
            ChunkData chunkData,
            Integer enforcedRotation) {
        List<PastableModulesContainer> validModules = new ArrayList<>();

        BorderTags border = chunkData.collectValidBordersFromNeighbours();

//        Logger.debug("All borders Entry: map = " + new Gson().toJson(border.neighborMap));
//        Logger.debug("containers size " + modulesContainers.size());

        List<Integer> rotationsToTry = enforcedRotation != null
                ? Collections.singletonList(enforcedRotation)
                : validRotations;

        for (ModulesContainer module : modulesContainers.values()) {
            Vector3i loc = chunkData.getChunkLocation();
            if (loc.y < module.modulesConfigField.getMinY() ||
                    loc.y > module.modulesConfigField.getMaxY())
                continue;

            for (int rotation : rotationsToTry) {
                boolean isValid = true;

                for (Map.Entry<ModulesConfigFields.BuildBorder, List<String>> neighborEntry : border.entrySet()) {
                    ModulesConfigFields.BuildBorder direction = neighborEntry.getKey();
                    List<String> neighborTags = neighborEntry.getValue();

                    if (neighborTags == null) continue; // No neighbor in this direction, skip

                    // Get the module's border tags for the transformed direction
                    List<String> moduleTags = module.getBorderTags().getRotatedTagsForDirection(direction, rotation);

//                    Logger.debug("checking direction " + direction + " for module " + module.getClipboardFilename() + " and got tags " + moduleTags);

                    if (Collections.disjoint(moduleTags, neighborTags)) {
                        isValid = false;
                        break;
                    }
                }

                if (isValid) {
                    validModules.add(new PastableModulesContainer(module, rotation));
//                    Logger.debug("Adding module " + module.getClipboardFilename());
//                    Logger.debug("Module borders: " + new Gson().toJson(module.getBorderTags()));
                }
            }
        }
        return validModules;
    }

    public static PastableModulesContainer pickRandomModuleFromSurroundings(
            ChunkData chunkData,
            Integer rotation) {
        List<PastableModulesContainer> validModules = getValidModulesFromSurroundings(chunkData, rotation);
        if (validModules.isEmpty()) return null;
        return validModules.get(ThreadLocalRandom.current().nextInt(0, validModules.size()));
    }

    private void processBorders(Map<String, Object> borderMap) {
        for (Map.Entry<String, Object> entry : borderMap.entrySet()) {
            String direction = entry.getKey().toLowerCase();
            List<String> borderList = processBorderList(entry.getValue());
            ModulesConfigFields.BuildBorder border = ModulesConfigFields.BuildBorder.fromString(direction);
            if (border == null) {
                Logger.warn("Invalid border " + direction + " for module " + configFilename);
                continue;
            }

            Logger.debug("Adding " + border + " / " + new Gson().toJson(borderList) + " to " + configFilename);

            borderTags.put(border, borderList);

            // Update appropriate tag map
            switch (border) {
                case NORTH:
                case SOUTH:
                case EAST:
                case WEST:
                    updateTags(sideTags, borderList);
                    break;
                case UP:
                    updateTags(topTags, borderList);
                    break;
                case DOWN:
                    updateTags(bottomTags, borderList);
                    break;

            }
        }

        // Check for missing borders
        for (ModulesConfigFields.BuildBorder border : ModulesConfigFields.BuildBorder.values()) {
            if (!borderTags.containsKey(border)) {
                Logger.warn("Failed to get module border " + border.toString() + " for module " + configFilename);
            }
        }

//        Logger.debug("Finished initializing, now its " + new Gson().toJson(borderTags));
    }

    private void updateTags(Map<String, HashSet<ModulesContainer>> tagMap, List<String> tags) {
        for (String tag : tags) {
            tagMap.computeIfAbsent(tag, k -> new HashSet<>()).add(this);
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

    public record BorderTags(Map<ModulesConfigFields.BuildBorder, List<String>> neighborMap) {
        public List<String> getRotatedTagsForDirection(ModulesConfigFields.BuildBorder buildBorder, int rotation) {
            return neighborMap.get(transformDirection(buildBorder, rotation));
        }

        public void put(ModulesConfigFields.BuildBorder direction, List<String> tags) {
            neighborMap.put(direction, tags);
        }

        public boolean containsKey(ModulesConfigFields.BuildBorder direction) {
            return neighborMap.containsKey(direction);
        }

        public Set<Map.Entry<ModulesConfigFields.BuildBorder, List<String>>> entrySet() {
            return neighborMap.entrySet();
        }

        public Collection<List<String>> values() {
            return neighborMap.values();
        }

        public void clear() {
            neighborMap.clear();
        }
    }

    public record PastableModulesContainer(ModulesContainer modulesContainer, Integer rotation) {
    }

}