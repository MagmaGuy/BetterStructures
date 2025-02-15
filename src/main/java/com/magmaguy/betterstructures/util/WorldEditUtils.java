package com.magmaguy.betterstructures.util;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Bukkit;
import org.bukkit.util.Vector;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldEditUtils {

    private static final ArrayList<String> values = new ArrayList<>();

    public static Vector getSchematicOffset(Clipboard clipboard) {
        return new Vector(clipboard.getMinimumPoint().x() - clipboard.getOrigin().x(), clipboard.getMinimumPoint().y() - clipboard.getOrigin().y(), clipboard.getMinimumPoint().z() - clipboard.getOrigin().z());
    }

    /**
     * <p>Parses data from a sign's NBT and returns the specified line number.
     * Tested with <b>WorldEdit and FastAsyncWorldEdit</b> NBT format.</p>
     */
    public static String getLine(@NotNull BaseBlock baseBlock, @Positive int line) {
        values.clear();
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

    private static String getOldWEFormat(@NotNull CompoundTag data, @Positive int line) {
        try {
            String text = ((StringTag) data.getValue().get("Text" + line)).getValue();

            Pattern pattern = Pattern.compile("\\{\"text\":\"(.*?)\"\\}");
            Matcher matcher = pattern.matcher(text);

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
            //Get the line
            String text = messages.getString(line - 1);

            if (text.contains("\"text\":")) text = text.split("text\":\"")[1].split("\"")[0];
            text = text.replaceAll("\"", "");
            if (text.contains("test")) Bukkit.getLogger().warning("boss name:" + text);

            return text;

        } catch (Exception ex) {
            Bukkit.getLogger().warning("Unexpected sign format in new read!\n" + data);
        }
        return null;
    }
}
