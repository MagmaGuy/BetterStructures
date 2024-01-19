package com.magmaguy.betterstructures.util;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.checkerframework.checker.index.qual.Positive;
import org.jetbrains.annotations.NotNull;

import java.util.*;
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
        Optional<Plugin> fawe = Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .filter(pl -> pl.getName().contains("FastAsyncWorldEdit"))
                .findFirst();
        if (fawe.isPresent()) {
            return getLineFawe(data, line);
        }
        return getLineWe(data, line);
    }

    /**
     * <p>Parses data from a sign's NBT and returns the specified line number.
     * Designed for <b>WorldEdit</b> NBT format.</p>
     * <p>Compatibility with version 1.20.4 has been added.</p>
     */
    private static String getLineWe(@NotNull CompoundTag data, @Positive int line) {
        List<String> lines = data.toString().lines().toList();

        for (String s : lines) {
            s = s.trim().replace("{\"text\":", "").replace("}", ""); // support for versions below 1.20.4

            if (s.startsWith("TAG_String(\"") && !s.equals("TAG_String(\"\")")) {
                values.add(s.replace("TAG_String(\"", "").replace("\")", ""));
            }
        }
        // lines in versions below 1.20.4 duplicated twice - note this is just for versions prior to 1.20.4
        if (data.toString().contains("{\"text\":")) {
            Matcher matcher = Pattern.compile("Text[1-4]=LinStringTag\\[\\{\"text\":\"(.*?)\"").matcher(data.toString());
            while (matcher.find()) {
                if (!matcher.group(1).isEmpty() && !matcher.group(1).isBlank())
                    values.add(matcher.group(1));
            }
            Collections.reverse(values);
        }

        if (line <= values.size()) {
            return values.get(line - 1);
        }
        return "";
    }

    /**
     * <p>Parses data from a sign's NBT and returns the specified line number.
     * Designed for <b>FastAsyncWorldEdit</b> NBT format.</p>
     * <p>Compatibility with version 1.20.4 has been added.</p>
     */
    private static String getLineFawe(@NotNull CompoundTag data, @Positive int line) {
        // 1.20.4 have a different format of NBT
        String str = data.toString()
                .replace("{\\\\\"text\\\\\":", "")
                .replace("}", "");

        Matcher matcher = Pattern.compile("value=\"\\\\\\\\\"(.*?)\\\\\\\\\"\"")
                .matcher(str);

        while (values.size() < 5 && matcher.find()) {
            String match = matcher.group(1);
            if (match.isEmpty()) continue;
            values.add(match);
        }
        // versions below 1.20.4 have reversed order of the lines (from 4 to 1)
        if (data.toString().contains("{\\\\\"text\\\\\":")) {
            Collections.reverse(values);
        }

        if (line <= values.size()) {
            return values.get(line - 1);
        }
        return "";
    }
}
