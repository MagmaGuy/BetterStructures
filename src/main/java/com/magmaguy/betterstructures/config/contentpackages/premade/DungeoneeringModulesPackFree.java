package com.magmaguy.betterstructures.config.contentpackages.premade;

import com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfigFields;

import java.util.List;

public class DungeoneeringModulesPackFree extends ContentPackageConfigFields {
    public DungeoneeringModulesPackFree() {
        super("dungeoneering_modules_free",
                true,
                "&2Dungeoneering Modules Free",
                List.of("&fThe first modular dungeon for BetterStructures!"),
                "https://nightbreak.io/plugin/betterstructures/#echoes-of-the-past",
                "dungeoneering_pack_free");
        setContentPackageType(ContentPackageType.MODULAR);
    }
}
