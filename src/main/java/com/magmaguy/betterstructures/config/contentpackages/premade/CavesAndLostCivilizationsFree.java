package com.magmaguy.betterstructures.config.contentpackages.premade;

import com.magmaguy.betterstructures.config.contentpackages.ContentPackageConfigFields;

import java.util.List;

public class CavesAndLostCivilizationsFree extends ContentPackageConfigFields {
    public CavesAndLostCivilizationsFree() {
        super("caves_and_lost_civilizations_free",
                true,
                "&2Caves and Lost Civilizations Free",
                List.of("&fA pack of 49 underground lost landmarks!"),
                "https://nightbreak.io/plugin/betterstructures/#caves-and-lost-civilizations-free",
                "caves_and_lost_civilizations_free");
        setContentPackageType(ContentPackageType.STRUCTURE);
    }
}
