package com.magmaguy.betterstructures.config.contentpackages.premade;

import com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfigFields;

import java.util.List;

public class AdventurePack extends ContentPackageConfigFields {
    public AdventurePack() {
        super("adventure_pack",
                true,
                "&2Adventure Pack",
                List.of("&f107 tough and massive adventure builds!"),
                "https://nightbreak.io/plugin/betterstructures/#adventure-pack",
                "adventure");
        setContentPackageType(ContentPackageType.STRUCTURE);
    }
}
