package com.magmaguy.betterstructures.config.treasures.premade;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import org.bukkit.loot.LootTables;

public class VanillaEndCityTreasureConfig extends TreasureConfigFields {
    public VanillaEndCityTreasureConfig() {
        super("vanilla_end_city_treasure", true);
        super.setVanillaTreasure(LootTables.END_CITY_TREASURE);
    }
}
