package com.magmaguy.betterstructures.api;

import com.magmaguy.betterstructures.buildingfitter.FitAnything;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fired on the main thread once a structure has been completely pasted: its blocks, pedestal, container loot,
 * [spawn], [elitemobs] and [mythicmobs] mobs, entities, and every {@link BuildCustomMarkerEvent}.
 * Pasting is spread over several ticks, so this is the reliable point to work with the placed structure.
 */
public class BuildPasteCompleteEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final FitAnything fitAnything;
    private final Location lowestCorner;
    private final Location highestCorner;
    private final List<Location> containerLocations;

    public BuildPasteCompleteEvent(FitAnything fitAnything, Location lowestCorner, Location highestCorner,
                                   List<Location> containerLocations) {
        this.fitAnything = fitAnything;
        this.lowestCorner = lowestCorner.clone();
        this.highestCorner = highestCorner.clone();
        this.containerLocations = containerLocations.stream().map(Location::clone).toList();
    }

    /**
     * The structure that was pasted, as in {@link BuildPlaceEvent}.
     *
     * @return The structure's fitting and schematic data.
     */
    public FitAnything getFitAnything() {
        return fitAnything;
    }

    /**
     * @return The lowest block corner of the pasted schematic, in world coordinates.
     */
    public Location getLowestCorner() {
        return lowestCorner.clone();
    }

    /**
     * @return The highest block corner of the pasted schematic, in world coordinates (inclusive).
     */
    public Location getHighestCorner() {
        return highestCorner.clone();
    }

    /**
     * @return The space covered by every block of the pasted schematic.
     */
    public BoundingBox getBoundingBox() {
        return BoundingBox.of(lowestCorner.getBlock(), highestCorner.getBlock());
    }

    /**
     * @return Every chest, trapped chest, shulker box of any color and barrel from the schematic, in world
     * coordinates, whether or not BetterStructures filled it with loot.
     */
    public List<Location> getContainerLocations() {
        return containerLocations.stream().map(Location::clone).toList();
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
