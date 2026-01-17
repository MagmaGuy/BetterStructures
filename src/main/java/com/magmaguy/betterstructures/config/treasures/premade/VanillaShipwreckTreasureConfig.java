package com.magmaguy.betterstructures.config.treasures.premade;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import org.bukkit.loot.LootTables;

public class VanillaShipwreckTreasureConfig extends TreasureConfigFields {
    public VanillaShipwreckTreasureConfig() {
        super("vanilla_shipwreck_treasure", true);
        super.setVanillaTreasure(LootTables.SHIPWRECK_TREASURE);
    }
}
