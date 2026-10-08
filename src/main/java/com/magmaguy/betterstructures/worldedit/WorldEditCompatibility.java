package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.betterstructures.MetadataHandler;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tells admins, loudly, when WorldEdit itself is what stops BetterStructures from working.
 * <p>
 * WorldEdit ships one adapter per Minecraft version, and that adapter (block state conversion,
 * Mojang's data fixers, NBT) is the part that breaks when a WorldEdit build does not match the
 * server or has a version-specific bug. Those failures surface as BetterStructures errors, so
 * admins report them here even though only a WorldEdit update can fix them. A failure is blamed on
 * WorldEdit only when the evidence says so: it started inside WorldEdit's version adapter, or it is
 * a linkage error thrown by WorldEdit code. A damaged schematic fails in WorldEdit's file readers
 * instead and keeps the ordinary message.
 */
public final class WorldEditCompatibility {
    // WorldEdit's and FastAsyncWorldEdit's per-Minecraft-version adapter code, data converters included.
    private static final List<String> VERSION_ADAPTER_PACKAGES = List.of(
            "com.sk89q.worldedit.bukkit.adapter.",
            "com.fastasyncworldedit.bukkit.adapter.");
    // Frames passed through on the way down to the code that made the failing call. A failure in
    // Mojang's code still belongs to the WorldEdit adapter that called it for this version.
    private static final List<String> PASS_THROUGH_PACKAGES = List.of(
            "java.", "javax.", "jdk.", "sun.",
            "net.minecraft.", "com.mojang.", "org.bukkit.craftbukkit.", "ca.spottedleaf.");
    private static final String WORLDEDIT_DOWNLOAD = "https://modrinth.com/plugin/worldedit/versions";
    private static final String FAWE_DOWNLOAD = "https://modrinth.com/plugin/fastasyncworldedit/versions";
    private static final String BORDER = "############################################################";

    private static final AtomicReference<Fault> firstFault = new AtomicReference<>();

    private WorldEditCompatibility() {
    }

    /**
     * Logs the WorldEdit warning if WorldEdit caused this schematic failure.
     *
     * @param failure   what WorldEdit threw while reading the schematic
     * @param schematic the schematic as the admin knows it, not a temporary copy
     * @return true if the failure was WorldEdit's and has been reported
     */
    public static boolean report(Throwable failure, File schematic) {
        StackTraceElement origin = worldEditOrigin(failure);
        if (origin == null) return false;
        Fault fault = new Fault(schematic.getPath(), origin.getClassName() + "." + origin.getMethodName(), describe(failure));
        if (!firstFault.compareAndSet(null, fault)) {
            log().severe("WorldEdit also failed on " + fault.schematic() + " (" + fault.exception() + ").");
            return true;
        }
        List<String> lines = new ArrayList<>();
        lines.add("BetterStructures can't load schematics because of " + worldEditName() + ".");
        lines.add("This is a " + worldEditName() + " problem, not a BetterStructures bug.");
        lines.add("");
        lines.add("Installed: " + worldEditName() + " " + worldEditVersion());
        lines.add("Server:    " + serverDescription());
        lines.add("Schematic: " + fault.schematic());
        lines.add("Failed in: " + fault.origin() + " (" + fault.exception() + ")");
        lines.add("");
        lines.add(worldEditName() + "'s code for this Minecraft version failed while reading the schematic.");
        lines.add("Update " + worldEditName() + " to its newest build for Minecraft " + Bukkit.getBukkitVersion().split("-")[0]
                + " and restart the server:");
        lines.add("  " + downloadUrl());
        lines.add("BetterStructures can't work around this. Updating " + worldEditName() + " is the fix.");
        logBox(lines);
        log().log(Level.SEVERE, "Full error from " + worldEditName() + ":", failure);
        return true;
    }

    /**
     * Warns once at startup when WorldEdit found no adapter for this Minecraft version. WorldEdit then
     * runs without block state conversion and NBT support, which breaks schematic loading and pasting.
     */
    public static void checkAdapterOnStartup() {
        int dataVersion;
        try {
            Platform platform = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING);
            dataVersion = platform.getDataVersion();
        } catch (RuntimeException | LinkageError exception) {
            // No ready platform or an API this check cannot read; schematic loading reports real failures.
            return;
        }
        if (dataVersion != -1) return;
        List<String> lines = new ArrayList<>();
        lines.add(worldEditName() + " " + worldEditVersion() + " does not support this server version.");
        lines.add("This is a " + worldEditName() + " problem, not a BetterStructures bug.");
        lines.add("");
        lines.add("Server: " + serverDescription());
        lines.add("");
        lines.add(worldEditName() + " found no adapter for this Minecraft version, so it can't read block data,");
        lines.add("signs or chests from schematics. BetterStructures structures will fail to load or paste.");
        lines.add("Update " + worldEditName() + " to a build for Minecraft " + Bukkit.getBukkitVersion().split("-")[0]
                + " and restart the server:");
        lines.add("  " + downloadUrl());
        logBox(lines);
    }

    /**
     * @return the first WorldEdit failure since the last {@link #reset()}, or null if there was none
     */
    public static Fault firstFault() {
        return firstFault.get();
    }

    /**
     * Forgets earlier failures. Called before each schematic load pass.
     */
    public static void reset() {
        firstFault.set(null);
    }

    /**
     * @return a short chat line for admins, or null if no WorldEdit failure happened
     */
    public static String chatWarning() {
        if (firstFault.get() == null) return null;
        return "&c" + worldEditName() + " " + worldEditVersion()
                + " failed to read BetterStructures schematics on this server version. This is a "
                + worldEditName() + " problem: update " + worldEditName() + " and restart. Details are in the console.";
    }

    static StackTraceElement worldEditOrigin(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = failure; current != null && seen.add(current); current = current.getCause()) {
            StackTraceElement caller = firstNonPassThroughFrame(current);
            if (caller == null) continue;
            if (isVersionAdapter(caller.getClassName())) return caller;
            // WorldEdit compiled against server internals that are not there: wrong build for this server.
            if (current instanceof LinkageError && isWorldEdit(caller.getClassName())) return caller;
        }
        return null;
    }

    private static StackTraceElement firstNonPassThroughFrame(Throwable throwable) {
        for (StackTraceElement frame : throwable.getStackTrace()) {
            if (!startsWithAny(frame.getClassName(), PASS_THROUGH_PACKAGES)) return frame;
        }
        return null;
    }

    private static boolean isVersionAdapter(String className) {
        return startsWithAny(className, VERSION_ADAPTER_PACKAGES);
    }

    private static boolean isWorldEdit(String className) {
        return className.startsWith("com.sk89q.") || className.startsWith("com.fastasyncworldedit.");
    }

    private static boolean startsWithAny(String className, List<String> prefixes) {
        for (String prefix : prefixes) if (className.startsWith(prefix)) return true;
        return false;
    }

    private static String describe(Throwable failure) {
        String message = failure.getMessage();
        return failure.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static Plugin worldEditPlugin() {
        Plugin fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        return fawe != null ? fawe : Bukkit.getPluginManager().getPlugin("WorldEdit");
    }

    private static boolean isFawe() {
        return Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
    }

    private static String worldEditName() {
        return isFawe() ? "FastAsyncWorldEdit" : "WorldEdit";
    }

    private static String worldEditVersion() {
        Plugin plugin = worldEditPlugin();
        return plugin == null ? "(unknown version)" : plugin.getDescription().getVersion();
    }

    private static String downloadUrl() {
        return isFawe() ? FAWE_DOWNLOAD : WORLDEDIT_DOWNLOAD;
    }

    private static String serverDescription() {
        return Bukkit.getName() + " " + Bukkit.getVersion();
    }

    private static void logBox(List<String> lines) {
        Logger logger = log();
        logger.severe(BORDER);
        for (String line : lines) logger.severe(line.isEmpty() ? "#" : "# " + line);
        logger.severe(BORDER);
    }

    private static Logger log() {
        return MetadataHandler.PLUGIN != null ? MetadataHandler.PLUGIN.getLogger() : Bukkit.getLogger();
    }

    /**
     * One schematic that WorldEdit failed to read.
     *
     * @param schematic path of the schematic the admin installed
     * @param origin    the WorldEdit method the failure started in
     * @param exception the exception type and message
     */
    public record Fault(String schematic, String origin, String exception) {
    }
}
