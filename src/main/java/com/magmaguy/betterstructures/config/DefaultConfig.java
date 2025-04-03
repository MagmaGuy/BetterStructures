package com.magmaguy.betterstructures.config;

import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.ConfigurationFile;
import lombok.Getter;

import java.util.List;

public class DefaultConfig extends ConfigurationFile {
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
//    @Getter
//    private static double airStructuresPerThousandChunks;
//    @Getter
//    private static double oceanStructuresPerThousandChunks;
//    @Getter
//    private static double landStructuresPerThousandChunks;
//    @Getter
//    private static double shallowUndergroundStructuresPerThousandChunks;
//    @Getter
//    private static double deepUndergroundStructuresPerThousandChunks;
    @Getter
    private static boolean newBuildingWarn;
    @Getter
    private static String regionProtectedMessage;
    @Getter
    private static boolean protectEliteMobsRegions;
    private static DefaultConfig instance;
    @Getter
    private static boolean setupDone;
    @Getter
    private static int modularChunkPastingSpeed = 10;
    @Getter
    private static double percentageOfTickUsedForPasting = 0.2;

    // Adding getters for the new distance and offset variables
    @Getter
    private static int distanceSurface;
    @Getter
    private static int distanceShallow;
    @Getter
    private static int distanceDeep;
    @Getter
    private static int distanceSky;
    @Getter
    private static int distanceLiquid;

    @Getter
    private static int maxOffsetSurface;
    @Getter
    private static int maxOffsetShallow;
    @Getter
    private static int maxOffsetDeep;
    @Getter
    private static int maxOffsetSky;
    @Getter
    private static int maxOffsetLiquid;

    public DefaultConfig() {
        super("config.yml");
        instance = this;
    }

    public static void toggleSetupDone() {
        setupDone = !setupDone;
        ConfigurationEngine.writeValue(setupDone, instance.file, instance.getFileConfiguration(), "setupDone");
    }

    public static void toggleSetupDone(boolean value) {
        setupDone = value;
        ConfigurationEngine.writeValue(setupDone, instance.file, instance.getFileConfiguration(), "setupDone");
    }


    public static boolean toggleWarnings() {
        newBuildingWarn = !newBuildingWarn;
        ConfigurationEngine.writeValue(newBuildingWarn, instance.file, instance.fileConfiguration, "warnAdminsAboutNewBuildings");
        return newBuildingWarn;
    }

    @Override
    public void initializeValues() {
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
//        airStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "airStructuresPerThousandChunks", 0.5);
//        oceanStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "oceanStructuresPerThousandChunks", 0.5);
//        landStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "landStructuresPerThousandChunks", 2.0);
//        shallowUndergroundStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "shallowUndergroundStructuresPerThousandChunks", 2.0);
//        deepUndergroundStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "deepUndergroundStructuresPerThousandChunks", 2.0);
        newBuildingWarn = ConfigurationEngine.setBoolean(fileConfiguration, "warnAdminsAboutNewBuildings", true);
        regionProtectedMessage = ConfigurationEngine.setString(fileConfiguration, "regionProtectedMessage", "&8[BetterStructures] &cDefeat the zone's bosses to edit blocks!");
        protectEliteMobsRegions = ConfigurationEngine.setBoolean(fileConfiguration, "protectEliteMobsRegions", true);
        setupDone = ConfigurationEngine.setBoolean(fileConfiguration, "setupDone", false);
        modularChunkPastingSpeed = ConfigurationEngine.setInt(fileConfiguration, "modularChunkPastingSpeed", 10);
        percentageOfTickUsedForPasting = ConfigurationEngine.setDouble(List.of("Sets the maximum percentage of a tick that BetterStructures will use to paste builds, however many it maybe trying to generate.", "Ranges from 0.01 to 1, where 0.01 is 1% and 1 is 100%.", "Slower speeds will lower performance impact, but can lead to other problems such as builds suddenly popping in."),fileConfiguration, "percentageOfTickUsedForPasting", 0.2);

        // Initialize the distances from configuration
        distanceSurface = ConfigurationEngine.setInt(fileConfiguration, "distanceSurface", 31);
        distanceShallow = ConfigurationEngine.setInt(fileConfiguration, "distanceShallow", 22);
        distanceDeep = ConfigurationEngine.setInt(fileConfiguration, "distanceDeep", 22);
        distanceSky = ConfigurationEngine.setInt(fileConfiguration, "distanceSky", 95);
        distanceLiquid = ConfigurationEngine.setInt(fileConfiguration, "distanceLiquid", 65);

        // Initialize the maximum offsets from configuration
        maxOffsetSurface = ConfigurationEngine.setInt(fileConfiguration, "maxOffsetSurface", 5);
        maxOffsetShallow = ConfigurationEngine.setInt(fileConfiguration, "maxOffsetShallow", 5);
        maxOffsetDeep = ConfigurationEngine.setInt(fileConfiguration, "maxOffsetDeep", 5);
        maxOffsetSky = ConfigurationEngine.setInt(fileConfiguration, "maxOffsetSky", 5);
        maxOffsetLiquid = ConfigurationEngine.setInt(fileConfiguration, "maxOffsetLiquid", 5);

        ConfigurationEngine.fileSaverOnlyDefaults(fileConfiguration, file);
    }
}