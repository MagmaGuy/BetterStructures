package com.magmaguy.betterstructures.api;

import org.bukkit.block.Container;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ChestFillEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean isCancelled = false;
    private Container container;
    private String treasureConfigFilename;

    public ChestFillEvent(Container container, String treasureConfigFilename) {
        this.container = container;
        this.treasureConfigFilename = treasureConfigFilename;
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
     * Returns the container in this event. This chest has the inventory snapshot modified, and if the
     * event is not cancelled it will force an update.
     * <p>
     * If you want to modify the chest contents, simply #addItem(itemStack) or #removeItem(itemStack) to the
     * Inventory.getSnapshotInventory()
     *
     * @return The container to be filled
     */
    public Container getContainer() {
        return container;
    }

    /**
     * Returns the filename of the treasure configuration used to fill this chest.
     * This can be used to identify which loot table was used.
     *
     * @return The treasure config filename, or null if not set
     */
    public String getTreasureConfigFilename() {
        return treasureConfigFilename;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
