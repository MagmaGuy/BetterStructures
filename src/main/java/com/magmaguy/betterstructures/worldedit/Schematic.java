package com.magmaguy.betterstructures.worldedit;

import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

public class Schematic {
    private Schematic() {
    }

    private static boolean erroredOnce = false;

    public static Clipboard load(File schematicFile) {
        /*
        File file = Path.of(Bukkit.getPluginManager().getPlugin("BetterStructures").getDataFolder().getAbsolutePath()
                + File.separatorChar + "schematics" + File.separatorChar + schematicFile).toFile();
         */
        Clipboard clipboard;

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            clipboard = reader.read();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchElementException e) {
            Logger.warn("Failed to get element from schematic " + schematicFile.getName());
            e.printStackTrace();
            return null;
        } catch (Exception e){
            Logger.warn("Failed to load schematic " + schematicFile.getName() + " ! 99% of the time, this is because you are not using the correct WorldEdit version for your Minecraft server. You should be downloading WorldEdit from here https://dev.bukkit.org/projects/worldedit . You can check which versions the download links are compatible with by hovering over them.");
            erroredOnce = true;
            if (!erroredOnce)            e.printStackTrace();
            else Logger.warn("Hiding stacktrace for this error, as it has already been printed once");
            return null;
        }
        return clipboard;
    }

    public static void paste(Clipboard clipboard, Location location) {
        World world = BukkitAdapter.adapt(location.getWorld());
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    // configure here
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }
}
