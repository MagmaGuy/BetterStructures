package com.magmaguy.betterstructures.config.treasures.premade;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import org.bukkit.loot.LootTables;

public class VanillaFishingTreasureConfig extends TreasureConfigFields {
    public VanillaFishingTreasureConfig() {
        super("vanilla_fishing_treasure", true);
        super.setVanillaTreasure(LootTables.FISHING_TREASURE);
    }
}
