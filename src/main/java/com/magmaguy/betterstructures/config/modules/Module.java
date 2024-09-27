package com.magmaguy.betterstructures.config.modules;

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
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

public class Module {
    private Module() {
    }

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

    public static void paste(Clipboard clipboard, Location location, Integer rotation) {
        World world = BukkitAdapter.adapt(location.getWorld());
        if (rotation == null) rotation = 0; //todo: rotations not working yet need to fix
        switch (rotation){
            case 0:
                break;
            case 90:
                location.add(0, 0, 17);
                break;
            case 180:
                location.add(17, 0, 17);
                break;
            case 270:
                location.add(17, 0, 0);
                break;
            default:
                Logger.warn("How did that even happen? Invalid rotation!");
        }
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            holder.setTransform(new AffineTransform().rotateY(rotation));
            Operation operation = holder
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
