package com.magmaguy.betterstructures.api;

import com.magmaguy.betterstructures.modules.ModularWorld;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class WorldGenerationFinishEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final ModularWorld modularWorld;


    public WorldGenerationFinishEvent(ModularWorld modularWorld) {
        this.modularWorld = modularWorld;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}