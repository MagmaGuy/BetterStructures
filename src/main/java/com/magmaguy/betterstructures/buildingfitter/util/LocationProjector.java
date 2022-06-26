package com.magmaguy.betterstructures.buildingfitter.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;


public class LocationProjector {

    /**
     * Projects a location to where it is going to be after the paste, taking the schematic anchor point into account.
     *
     * @param worldAnchorPoint The real paste location.
     * @param schematicOffset  The schematic's offset from its lowest point.
     * @return
     */
    public static Location project(Location worldAnchorPoint, Vector schematicOffset) {
        return worldAnchorPoint.clone().add(schematicOffset);
    }

    /**
     * Projects a location to where it is going to be after the paste, taking the schematic point into account. Intended
     * for use with iterators where points are arbitrarily offset by a distance.
     *
     * @param worldAnchorPoint      The real paste location.
     * @param schematicOffset       The schematic's offset from its lowest point.
     * @param relativeBlockLocation Arbitrary distance the point is from the anchor point.
     * @return
     */
    public static Location project(Location worldAnchorPoint, Vector schematicOffset, Vector relativeBlockLocation) {
        return project(worldAnchorPoint, schematicOffset).add(relativeBlockLocation);
    }
}
