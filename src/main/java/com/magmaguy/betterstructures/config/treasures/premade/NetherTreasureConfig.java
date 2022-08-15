package com.magmaguy.betterstructures.config.treasures.premade;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;

public class NetherTreasureConfig extends TreasureConfigFields {
    public NetherTreasureConfig() {
        super("treasure_nether", true);
        setRawLoot(DefaultChestContents.netherContents());
    }
}
