package com.magmaguy.betterstructures.config.treasures.premade;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;

public class OverworldUndergroundTreasureConfig extends TreasureConfigFields {
    public OverworldUndergroundTreasureConfig() {
        super("treasure_overworld_underground", true);
        setRawLoot(DefaultChestContents.overworldUndergroundContents());
    }
}
