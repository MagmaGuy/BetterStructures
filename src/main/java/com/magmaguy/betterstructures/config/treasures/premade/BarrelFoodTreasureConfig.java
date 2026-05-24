package com.magmaguy.betterstructures.config.treasures.premade;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;

public class BarrelFoodTreasureConfig extends TreasureConfigFields {
    public BarrelFoodTreasureConfig() {
        super("treasure_barrel_food", true);
        super.setRawLoot(DefaultChestContents.barrelFoodContents());
        super.setMean(1);
        super.setStandardDeviation(0.7);
    }
}
