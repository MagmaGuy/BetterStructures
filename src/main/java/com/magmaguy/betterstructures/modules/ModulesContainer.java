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
    private static final List<Integer> validRotations = Arrays.asList(0, 90, 180, 270);
    @Getter
    private final Clipboard clipboard;
    @Getter
    private final String clipboardFilename;
    private final ModulesConfigFields modulesConfigField;
    private final String configFilename;
    @Getter
    private BorderTags borderTags = new BorderTags(new EnumMap<>(BuildBorder.class));
    @Getter
    private boolean nothing = false;

    public ModulesContainer(Clipboard clipboard, String clipboardFilename, ModulesConfigFields modulesConfigField, String configFilename) {
        this.clipboard = clipboard;
        this.clipboardFilename = clipboardFilename;
        this.modulesConfigField = modulesConfigField;
        this.configFilename = configFilename;
        if (!clipboardFilename.equalsIgnoreCase("nothing"))
            processBorders(modulesConfigField.getBorderMap());
        else nothing = true;
        modulesContainers.put(clipboardFilename, this);
    }

    public static void initialize() {
        //Initialize "nothing", a reserved name with special behavior
        ModulesContainer nothing = new ModulesContainer(null, "nothing", null, null);
        nothing.borderTags = new BorderTags(Map.of(
                BuildBorder.NORTH, Collections.singletonList("nothing"),
                BuildBorder.SOUTH, Collections.singletonList("nothing"),
                BuildBorder.EAST, Collections.singletonList("nothing"),
                BuildBorder.WEST, Collections.singletonList("nothing"),
                BuildBorder.UP, Collections.singletonList("nothing"),
                BuildBorder.DOWN, Collections.singletonList("nothing")));
    }

    public static void shutdown() {
        modulesContainers.clear();
        tagOccurrences.clear();
    }

    public static List<PastableModulesContainer> getValidModulesFromSurroundings(
            ChunkData chunkData,
            Integer enforcedRotation) {
        List<PastableModulesContainer> validModules = new ArrayList<>();

        BorderTags border = chunkData.collectValidBordersFromNeighbours();

        List<Integer> rotationsToTry = enforcedRotation != null
                ? Collections.singletonList(enforcedRotation)
                : validRotations;

        for (ModulesContainer module : modulesContainers.values()) {
            Vector3i loc = chunkData.getChunkLocation();
            if (!module.nothing)
                if (loc.y < module.modulesConfigField.getMinY() ||
                        loc.y > module.modulesConfigField.getMaxY())
                    continue;

//            Logger.debug("Checking chunk " + new Gson().toJson(chunkData.collectValidBordersFromNeighbours()));

            for (int rotation : rotationsToTry) {
                boolean isValid = true;

                for (Map.Entry<BuildBorder, List<String>> neighborEntry : border.entrySet()) {
                    BuildBorder direction = neighborEntry.getKey();
                    List<String> neighborTags = neighborEntry.getValue();

                    // Get the module's border tags for the transformed direction
                    List<String> moduleTags = module.getBorderTags().getRotatedTagsForDirection(direction, rotation);

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
            BuildBorder border = BuildBorder.fromString(direction);
            if (border == null) {
                Logger.warn("Invalid border " + direction + " for module " + configFilename);
                continue;
            }

            Logger.debug("Adding " + border + " / " + new Gson().toJson(borderList) + " to " + configFilename);

            borderTags.put(border, borderList);
        }

        // Check for missing borders
        for (BuildBorder border : BuildBorder.values()) {
            if (!borderTags.containsKey(border)) {
                Logger.warn("Failed to get module border " + border.toString() + " for module " + configFilename);
            }
        }

//        Logger.debug("Finished initializing, now its " + new Gson().toJson(borderTags));
    }

    private List<String> processBorderList(Object rawBorderList) {
        List<String> stringList = (List<String>) rawBorderList;
        for (String string : stringList) {
            // Increment the occurrence count for the string
            tagOccurrences.put(string, tagOccurrences.getOrDefault(string, 0) + 1);
        }

        return stringList;
    }

    public record BorderTags(Map<BuildBorder, List<String>> neighborMap) {
        public List<String> getRotatedTagsForDirection(BuildBorder buildBorder, int rotation) {
            return neighborMap.get(BuildBorder.transformDirection(buildBorder, rotation));
        }

        public void put(BuildBorder direction, List<String> tags) {
            neighborMap.put(direction, tags);
        }

        public boolean containsKey(BuildBorder direction) {
            return neighborMap.containsKey(direction);
        }

        public Set<Map.Entry<BuildBorder, List<String>>> entrySet() {
            return neighborMap.entrySet();
        }

        public Collection<List<String>> values() {
            return neighborMap.values();
        }
    }

    public record PastableModulesContainer(ModulesContainer modulesContainer, Integer rotation) {
    }

}