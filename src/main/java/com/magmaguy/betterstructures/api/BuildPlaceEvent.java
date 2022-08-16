package com.magmaguy.betterstructures.api;

import com.magmaguy.betterstructures.buildingfitter.FitAnything;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BuildPlaceEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean isCancelled = false;
    private final FitAnything fitAnything;

    public BuildPlaceEvent(FitAnything fitAnything) {
        this.fitAnything = fitAnything;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.isCancelled = b;
    }

    /**
     * {@FitAnything} contains all of the data related to the schematic that is about to get pasted.
     *
     * @return The key object involved.
     */
    public FitAnything getFitAnything() {
        return fitAnything;
    }

}
