package com.magmaguy.betterstructures.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public class DefaultConfig {
    private DefaultConfig() {
    }

    @Getter
    private static int lowestYNormalCustom;
    @Getter
    private static int highestYNormalCustom;
    @Getter
    private static int lowestYNether;
    @Getter
    private static int highestYNether;
    @Getter
    private static int lowestYEnd;
    @Getter
    private static int highestYEnd;
    @Getter
    private static int normalCustomAirBuildingMinAltitude;
    @Getter
    private static int normalCustomAirBuildingMaxAltitude;
    @Getter
    private static int endAirBuildMinAltitude;
    @Getter
    private static int endAirBuildMaxAltitude;
    /*
    @Getter
    private static double airStructureRarityMultiplier;
    @Getter
    private static double surfaceStructureRarityMultiplier;
    @Getter
    private static double shallowUndergroundStructureRarityMultiplier;
    @Getter
    private static double deepUndergroundStructureRarityMultiplier;
    @Getter
    private static double liquidSurfaceStructureRarityMultiplier;
     */
    @Getter
    private static double airStructuresPerThousandChunks;
    @Getter
    private static double oceanStructuresPerThousandChunks;
    @Getter
    private static double landStructuresPerThousandChunks;
    @Getter
    private static double shallowUndergroundStructuresPerThousandChunks;
    @Getter
    private static double deepUndergroundStructuresPerThousandChunks;


    public static void initializeConfig() {
        File file = ConfigurationEngine.fileCreator("config.yml");
        FileConfiguration fileConfiguration = ConfigurationEngine.fileConfigurationCreator(file);
        lowestYNormalCustom = ConfigurationEngine.setInt(fileConfiguration, "lowestYNormalCustom", -60);
        highestYNormalCustom = ConfigurationEngine.setInt(fileConfiguration, "highestYNormalCustom", 320);
        lowestYNether = ConfigurationEngine.setInt(fileConfiguration, "lowestYNether", 4);
        highestYNether = ConfigurationEngine.setInt(fileConfiguration, "highestYNether", 120);
        lowestYEnd = ConfigurationEngine.setInt(fileConfiguration, "lowestYEnd", 0);
        highestYEnd = ConfigurationEngine.setInt(fileConfiguration, "highestYEnd", 320);
        normalCustomAirBuildingMinAltitude = ConfigurationEngine.setInt(fileConfiguration, "normalCustomAirBuildingMinAltitude", 80);
        normalCustomAirBuildingMaxAltitude = ConfigurationEngine.setInt(fileConfiguration, "normalCustomAirBuildingMaxAltitude", 120);
        endAirBuildMinAltitude = ConfigurationEngine.setInt(fileConfiguration, "endAirBuildMinAltitude", 80);
        endAirBuildMaxAltitude = ConfigurationEngine.setInt(fileConfiguration, "endAirBuildMaxAltitude", 120);
        /*
        airStructureRarityMultiplier = ConfigurationEngine.setDouble(fileConfiguration, "airStructureRarityMultiplier", 1.5D);
        surfaceStructureRarityMultiplier = ConfigurationEngine.setDouble(fileConfiguration, "surfaceStructureRarityMultiplier", .5D);
        shallowUndergroundStructureRarityMultiplier = ConfigurationEngine.setDouble(fileConfiguration, "shallowUndergroundStructureRarityMultiplier", .5D);
        deepUndergroundStructureRarityMultiplier = ConfigurationEngine.setDouble(fileConfiguration, "deepUndergroundStructureRarityMultiplier", .5D);
        liquidSurfaceStructureRarityMultiplier = ConfigurationEngine.setDouble(fileConfiguration, "liquidSurfaceStructureRarityMultiplier", 11D);
         */
        airStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "airStructuresPerThousandChunks", 0.5);
        oceanStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "oceanStructuresPerThousandChunks", 0.5);
        landStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "landStructuresPerThousandChunks", 2.0);
        shallowUndergroundStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "shallowUndergroundStructuresPerThousandChunks", 2.0);
        deepUndergroundStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "deepUndergroundStructuresPerThousandChunks", 2.0);

        ConfigurationEngine.fileSaverOnlyDefaults(fileConfiguration, file);
    }
}
