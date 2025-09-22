package com.magmaguy.betterstructures.config.contentpackages.premade;

import com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfigFields;

import java.util.List;

public class CavesAndLostCivilizationsPremium extends ContentPackageConfigFields {
    public CavesAndLostCivilizationsPremium() {
        super("caves_and_lost_civilizations_premium",
                true,
                "&2Caves and Lost Civilizations Premium",
                List.of("&fA pack of 101 underground lost landmarks!"),
                "https://nightbreak.io/plugin/betterstructures/#caves-and-lost-civilizations-premium",
                "caves_and_lost_civilizations_premium");
        setContentPackageType(ContentPackageType.STRUCTURE);
    }
}

