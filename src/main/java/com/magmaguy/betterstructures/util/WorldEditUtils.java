package com.magmaguy.betterstructures.util;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldEditUtils {

    private static final ArrayList<String> values = new ArrayList<>();

    public static Vector getSchematicOffset(Clipboard clipboard) {
        return new Vector(clipboard.getMinimumPoint().getX() - clipboard.getOrigin().getX(),
                clipboard.getMinimumPoint().getY() - clipboard.getOrigin().getY(),
                clipboard.getMinimumPoint().getZ() - clipboard.getOrigin().getZ());
    }

    /**
     * <p>Parses data from a sign's NBT and returns the specified line number.
     * Tested with <b>WorldEdit and FastAsyncWorldEdit</b> NBT format.</p>
     * <p>Compatibility with version 1.20.4 has been added.</p>
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
     * <p>Compatibility with version 1.20.4 has been added.</p>
     */
    private static String getLineWe(@NotNull CompoundTag data, @Positive int line) {
        try {
            String text = ((StringTag) data.getValue().get("Text"+line)).getValue();

            Pattern pattern = Pattern.compile("\\{\"text\":\"(.*?)\"\\}");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                String extractedText = matcher.group(1);
                return extractedText;
            } else {
                throw new Exception();
            }
        } catch (Exception ex) {
            Bukkit.getLogger().warning("Unexpected sign format!" + data);
        }

        return "";
    }
}
