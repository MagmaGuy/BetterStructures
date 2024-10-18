package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.modules.ChunkData;
import com.magmaguy.magmacore.util.Logger;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.World;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.List;

public class Module {
    private Module() {
    }

    public static void paste(Clipboard clipboard, Location location, Integer rotation) {
        World world = BukkitAdapter.adapt(location.getWorld());
        if (rotation == null) {
            Logger.debug("rotation was null at the time of the paste, this will not do");
            return;
        }
        //todo: this is scuffed af
        if (rotation == 90) rotation = 270;
        else if (rotation == 270) rotation = 90;
        switch (rotation) {
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
            editSession.setTrackingHistory(false);
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            holder.setTransform(new AffineTransform().rotateY(rotation));
            Operation operation = holder
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    // configure here
                    .build();
            editSession.setSideEffectApplier(SideEffectSet.none());
            Operations.complete(operation);
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
    }

    public static void batchPaste(List<ChunkData> chunkDataList, org.bukkit.World world) {
        World worldEditWorld = BukkitAdapter.adapt(world);
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(worldEditWorld)) {

            editSession.setTrackingHistory(false);
            editSession.setSideEffectApplier(SideEffectSet.none());

            for (ChunkData chunkData : chunkDataList) {
                if (chunkData == null || chunkData.getModulesContainer() == null) continue;
                //todo: this is scuffed af
                int rotation = chunkData.getModulesContainer().getRotation();
                Location location = chunkData.getRealLocation().add(-1, 0, -1);
                if (rotation == 90) rotation = 270;
                else if (rotation == 270) rotation = 90;
                switch (rotation) {
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

                ClipboardHolder holder = new ClipboardHolder(chunkData.getModulesContainer().getClipboard());
                holder.setTransform(new AffineTransform().rotateY(rotation));
                Operation operation = holder
                        .createPaste(editSession)
                        .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                        // configure here
                        .build();
                Operations.complete(operation);
            }

        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }

        HashSet<Chunk> chunks = new HashSet<>();
        chunkDataList.forEach(chunkData -> {
            if (chunkData != null) chunks.add(chunkData.getRealLocation().getChunk());
        });
        chunks.forEach(chunk -> chunk.unload(true));
    }
}
