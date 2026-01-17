package com.magmaguy.betterstructures.config.treasures.premade;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import org.bukkit.loot.LootTables;

public class VanillaBastionTreasureConfig extends TreasureConfigFields {
    public VanillaBastionTreasureConfig() {
        super("vanilla_bastion_treasure", true);
        super.setVanillaTreasure(LootTables.BASTION_TREASURE);
    }
}
