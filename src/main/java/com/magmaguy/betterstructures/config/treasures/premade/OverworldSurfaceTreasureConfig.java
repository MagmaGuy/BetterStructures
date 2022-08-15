package com.magmaguy.betterstructures.config.treasures.premade;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;

public class OverworldSurfaceTreasureConfig extends TreasureConfigFields {
    public OverworldSurfaceTreasureConfig() {
        super("treasure_overworld_surface", true);
        super.setRawLoot(DefaultChestContents.overworldContents());
    }
}
