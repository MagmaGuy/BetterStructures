package com.magmaguy.betterstructures.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldEditUtils {

    public static Vector getSchematicOffset(Clipboard clipboard) {
        return new Vector(clipboard.getMinimumPoint().x() - clipboard.getOrigin().x(), clipboard.getMinimumPoint().y() - clipboard.getOrigin().y(), clipboard.getMinimumPoint().z() - clipboard.getOrigin().z());
    }

    @Nullable
    public static Material adaptMaterial(@NotNull BlockState blockState) {
        return BukkitAdapter.adapt(blockState.getBlockType());
    }

    @Nullable
    public static Material adaptMaterial(@NotNull BaseBlock baseBlock) {
        return adaptMaterial(baseBlock.toImmutableState());
    }

    @Nullable
    public static BlockData createBlockDataOrNull(@NotNull BaseBlock baseBlock) {
        return createBlockDataOrNull(baseBlock.toImmutableState());
    }

    @Nullable
    public static BlockData createBlockDataOrNull(@NotNull BlockState blockState) {
        try {
            return Bukkit.createBlockData(blockState.getAsString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isAir(@NotNull BlockState blockState) {
        Material material = adaptMaterial(blockState);
        if (material != null) return material.isAir();
        return blockState.getBlockType().getMaterial().isAir();
    }

    public static boolean isSolid(@NotNull BlockState blockState) {
        Material material = adaptMaterial(blockState);
        if (material != null) return material.isSolid();
        return blockState.getBlockType().getMaterial().isSolid();
    }

    public static List<String> getLines(@NotNull BaseBlock baseBlock) {
        List<String> lines = new ArrayList<>();
        if (baseBlock.getNbtData() == null) {
            return lines;
        }

        for (int i = 1; i < 5; i++) {
            String line = getLine(baseBlock, i);
            if (line == null) return new ArrayList<>();
            if (!line.isEmpty() && !line.isBlank())
                lines.add(line);
        }

        return lines;
    }

    /**
     * <p>Parses data from a sign's NBT and returns the specified line number.
     * Tested with <b>WorldEdit and FastAsyncWorldEdit</b> NBT format.</p>
     */
    public static String getLine(@NotNull BaseBlock baseBlock, @Positive int line) {
        if (baseBlock.getNbtData() == null) {
            return "";
        }
        CompoundTag data = baseBlock.getNbtData();
        return getLineWe(data, line);
    }

    /**
     * <p>Parses data from a sign's NBT and returns the specified line number.
     * Designed for <b>WorldEdit</b> NBT format.</p>
     */
    private static String getLineWe(@NotNull CompoundTag data, @Positive int line) {
        try {
            if (data.getValue().containsKey("Text" + line)) {
                return getOldWEFormat(data, line);
            } else {
                return getNewWEFormat(data, line);
            }

        } catch (Exception ex) {
            Bukkit.getLogger().warning("Unexpected sign format!" + data);
        }

        return "";
    }

    private static final Pattern OLD_WE_TEXT_PATTERN = Pattern.compile("\\{\"text\":\"(.*?)\"\\}");

    private static String getOldWEFormat(@NotNull CompoundTag data, @Positive int line) {
        try {
            String text = ((StringTag) data.getValue().get("Text" + line)).getValue();

            Matcher matcher = OLD_WE_TEXT_PATTERN.matcher(text);

            if (matcher.find()) {
                String extractedText = matcher.group(1);
                return extractedText;
            } else {
                throw new Exception();
            }
        } catch (Exception ex) {
            Bukkit.getLogger().warning("Unexpected sign format in legacy read!\n" + data);
        }
        return null;
    }

    private static String getNewWEFormat(@NotNull CompoundTag data, @Positive int line) {
        try {
            //Get front text
            CompoundTag frontText = (CompoundTag) data.getValue().get("front_text");
            //Get messages
            ListTag messages = (ListTag) frontText.getValue().get("messages");
            //Typed lines are plain strings; plugins write text components, and 1.20-1.21.4 schematics JSON strings.
            String text = componentText(messages.getValue().get(line - 1));
            text = text.replaceAll("\"", "");
            return text;

        } catch (Exception ex) {
            Bukkit.getLogger().warning("Unexpected sign format in new read!\n" + data);
        }
        return null;
    }

    /** Plain text of a sign line stored as a string, a {text, extra} component, a component list, or a wrapped element. */
    private static String componentText(Object tag) {
        if (tag instanceof StringTag string) return jsonText(string.getValue());
        if (tag instanceof ListTag list) {
            StringBuilder text = new StringBuilder();
            for (Object element : list.getValue()) text.append(componentText(element));
            return text.toString();
        }
        if (tag instanceof CompoundTag compound) {
            Map<String, ?> value = compound.getValue();
            // NBT lists mixing strings and components wrap every element as {"": element}.
            if (value.containsKey("")) return componentText(value.get(""));
            StringBuilder text = new StringBuilder(value.get("text") instanceof StringTag string ? string.getValue() : "");
            if (value.get("extra") != null) text.append(componentText(value.get("extra")));
            return text.toString();
        }
        return "";
    }

    /** A JSON component string becomes its plain text; any other string already is plain text, "[spawn]" included. */
    private static String jsonText(String value) {
        if (!value.startsWith("{") && !value.startsWith("\"")) return value;
        try {
            return jsonComponentText(JsonParser.parseString(value));
        } catch (RuntimeException notJson) {
            return value;
        }
    }

    private static String jsonComponentText(JsonElement element) {
        if (element.isJsonPrimitive()) return element.getAsString();
        StringBuilder text = new StringBuilder();
        if (element.isJsonArray()) {
            for (JsonElement part : element.getAsJsonArray()) text.append(jsonComponentText(part));
        } else if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            if (object.has("text")) text.append(jsonComponentText(object.get("text")));
            if (object.has("extra")) text.append(jsonComponentText(object.get("extra")));
        }
        return text.toString();
    }

    public static Clipboard createSingleBlockClipboard(BaseBlock baseBlock, BlockState blockState) {
        return new Clipboard() {
            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
                return false;
            }

            @Nullable
            @Override
            public Operation commit() {
                return null;
            }

            @Override
            public BlockState getBlock(BlockVector3 position) {
                return blockState;
            }

            @Override
            public BaseBlock getFullBlock(BlockVector3 position) {
                return baseBlock;
            }

            @Override
            public BlockVector3 getMinimumPoint() {
                return BlockVector3.at(0,0,0);
            }

            @Override
            public BlockVector3 getMaximumPoint() {
                return BlockVector3.at(0,0,0);
            }

            @Override
            public List<? extends Entity> getEntities(Region region) {
                return new ArrayList<>();
            }

            @Override
            public List<? extends Entity> getEntities() {
                return new ArrayList<>();
            }

            @Nullable
            @Override
            public Entity createEntity(com.sk89q.worldedit.util.Location location, BaseEntity entity) {
                return null;
            }

            @Override
            public Region getRegion() {
                return new CuboidRegion(BlockVector3.at(0,0,0), BlockVector3.at(0,0,0));
            }

            @Override
            public BlockVector3 getDimensions() {
                return BlockVector3.at(1,1,1);
            }

            @Override
            public BlockVector3 getOrigin() {
                return BlockVector3.at(0,0,0);
            }

            @Override
            public void setOrigin(BlockVector3 origin) {

            }
        };
    }

    public static void pasteArmorStandsOnlyFromTransformed(Clipboard transformedClipboard, Location location) {
        com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(location.getWorld());

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(adaptedWorld)) {
            editSession.setTrackingHistory(false);
            editSession.setSideEffectApplier(SideEffectSet.none());

            ClipboardHolder clipboardHolder = new ClipboardHolder(transformedClipboard);

            BlockVector3 minPoint = transformedClipboard.getMinimumPoint();
            BlockVector3 origin   = transformedClipboard.getOrigin();

            // Align entities the same way you aligned blocks: min -> base
            BlockVector3 pastePosition = BlockVector3.at(
                    location.getBlockX() + (origin.x() - minPoint.x()),
                    location.getBlockY() + (origin.y() - minPoint.y()),
                    location.getBlockZ() + (origin.z() - minPoint.z())
            );

            Operation operation = clipboardHolder
                    .createPaste(editSession)
                    .to(pastePosition)
                    .copyEntities(true)
                    .copyBiomes(false)
                    .ignoreAirBlocks(true)
                    .maskSource(new BlockTypeMask(transformedClipboard, new BlockType[0]))
                    .build();

            Operations.complete(operation);
            
        } catch (Exception e) {
            Logger.warn("Failed to paste entities at " + location + ": " + e.getMessage());
        }
    }

}
