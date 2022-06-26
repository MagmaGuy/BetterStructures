package com.magmaguy.betterstructures.util;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.bukkit.util.Vector;

public class WorldEditUtils {
    public static Vector getSchematicOffset(Clipboard clipboard) {
        return new Vector(clipboard.getMinimumPoint().getX() - clipboard.getOrigin().getX(),
                clipboard.getMinimumPoint().getY() - clipboard.getOrigin().getY(),
                clipboard.getMinimumPoint().getZ() - clipboard.getOrigin().getZ());
    }
}
