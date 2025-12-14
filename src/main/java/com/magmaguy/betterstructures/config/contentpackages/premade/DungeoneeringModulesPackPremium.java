package com.magmaguy.betterstructures.config.contentpackages.premade;

import com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfigFields;

import java.util.List;

public class DungeoneeringModulesPackPremium extends ContentPackageConfigFields {
    public DungeoneeringModulesPackPremium() {
        super("dungeoneering_modules_premium",
                true,
                "&2Dungeoneering Modules Premium",
                List.of("&fThe first modular dungeon for BetterStructures!"),
                "https://nightbreak.io/plugin/betterstructures/#dungeoneering-modules-premium",
                "dungeoneering_modules_premium");
        setContentPackageType(ContentPackageType.MODULAR);
    }
}
