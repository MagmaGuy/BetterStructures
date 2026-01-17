package com.magmaguy.betterstructures.config.treasures.premade;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import org.bukkit.loot.LootTables;

public class VanillaBuriedTreasureConfig extends TreasureConfigFields {
    public VanillaBuriedTreasureConfig() {
        super("vanilla_buried_treasure", true);
        super.setVanillaTreasure(LootTables.BURIED_TREASURE);
    }
}
