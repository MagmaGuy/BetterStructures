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
    @Getter
    private static double percentageOfTickUsedForPregeneration = 0.1;
    @Getter
    private static double pregenerationTPSPauseThreshold = 12.0;
    @Getter
    private static double pregenerationTPSResumeThreshold = 14.0;

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
    private static int distanceDungeon;

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
    @Getter
    private static int maxOffsetDungeon;

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
        newBuildingWarn = ConfigurationEngine.setBoolean(fileConfiguration, "warnAdminsAboutNewBuildings", true);
        regionProtectedMessage = ConfigurationEngine.setString(fileConfiguration, "regionProtectedMessage", "&8[BetterStructures] &cDefeat the zone's bosses to edit blocks!");
        protectEliteMobsRegions = ConfigurationEngine.setBoolean(fileConfiguration, "protectEliteMobsRegions", true);
        setupDone = ConfigurationEngine.setBoolean(fileConfiguration, "setupDone", false);
        modularChunkPastingSpeed = ConfigurationEngine.setInt(fileConfiguration, "modularChunkPastingSpeed", 10);
        percentageOfTickUsedForPasting = ConfigurationEngine.setDouble(List.of("Sets the maximum percentage of a tick that BetterStructures will use to paste builds, however many it maybe trying to generate.", "Ranges from 0.01 to 1, where 0.01 is 1% and 1 is 100%.", "Slower speeds will lower performance impact, but can lead to other problems such as builds suddenly popping in."),fileConfiguration, "percentageOfTickUsedForPasting", 0.2);
        percentageOfTickUsedForPregeneration = ConfigurationEngine.setDouble(List.of("Sets the maximum percentage of a tick that BetterStructures will use for world pregeneration when using the pregenerate command.", "Ranges from 0.01 to 1, where 0.01 is 1% and 1 is 100%.", "This controls how much of each server tick is dedicated to generating chunks, allowing you to balance generation speed with server performance.", "Lower values will generate chunks more slowly but reduce server lag, while higher values will generate faster but may impact server performance."), fileConfiguration, "percentageOfTickUsedForPregeneration", 0.1);
        pregenerationTPSPauseThreshold = ConfigurationEngine.setDouble(List.of("The TPS threshold at which chunk pregeneration will pause to protect server performance.", "When server TPS drops below this value, pregeneration will pause until TPS recovers.", "Default: 12.0"), fileConfiguration, "pregenerationTPSPauseThreshold", 12.0);
        pregenerationTPSResumeThreshold = ConfigurationEngine.setDouble(List.of("The TPS threshold at which chunk pregeneration will resume after being paused.", "Pregeneration will only resume when server TPS is at or above this value.", "Should be higher than the pause threshold to prevent rapid pause/resume cycles.", "Default: 14.0"), fileConfiguration, "pregenerationTPSResumeThreshold", 14.0);

        // Initialize the distances from configuration
        distanceSurface = ConfigurationEngine.setInt(
                List.of(
                        "Sets the distance between structures in the surface of a world.",
                        "Shorter distances between structures will result in more structures overall."),
                fileConfiguration, "distanceSurface", 31);
        distanceShallow = ConfigurationEngine.setInt(
                List.of(
                        "Sets the distance between structures in shallow underground structure generation.",
                        "Shorter distances between structures will result in more structures overall."),fileConfiguration, "distanceShallow", 22);
        distanceDeep = ConfigurationEngine.setInt(
                List.of(
                        "Sets the distance between structures in deep underground structure generation.",
                        "Shorter distances between structures will result in more structures overall."),
                fileConfiguration, "distanceDeep", 22);
        distanceSky = ConfigurationEngine.setInt(
                List.of(
                        "Sets the distance between structures in placed in the air.",
                        "Shorter distances between structures will result in more structures overall."),
                fileConfiguration, "distanceSky", 95);
        distanceLiquid = ConfigurationEngine.setInt(
                List.of(
                        "Sets the distance between structures liquid surfaces such as oceans.",
                        "Shorter distances between structures will result in more structures overall."),
                fileConfiguration, "distanceLiquid", 65);
        distanceDungeon = ConfigurationEngine.setInt(
                List.of(
                        "Sets the distance between dungeons.",
                        "Shorter distances between dungeons will result in more dungeons overall."
                ),
                fileConfiguration, "distanceDungeonV2", 80);

        // Initialize the maximum offsets from configuration
        maxOffsetSurface = ConfigurationEngine.setInt(
                List.of(
                        "Used to tweak the randomization of the distance between structures in the surface of a world.",
                        "Smaller values will result in structures being more on a grid, and larger values will result in them being less predictably placed."),
                fileConfiguration, "maxOffsetSurface", 5);
        maxOffsetShallow = ConfigurationEngine.setInt(
                List.of(
                        "Used to tweak the randomization of the distance between structures in the shallow underworld of a world.",
                        "Smaller values will result in structures being more on a grid, and larger values will result in them being less predictably placed."),
                fileConfiguration, "maxOffsetShallow", 5);
        maxOffsetDeep = ConfigurationEngine.setInt(
                List.of(
                        "Used to tweak the randomization of the distance between structures in the deep underground of a world.",
                        "Smaller values will result in structures being more on a grid, and larger values will result in them being less predictably placed."),
                fileConfiguration, "maxOffsetDeep", 5);
        maxOffsetSky = ConfigurationEngine.setInt(
                List.of(
                        "Used to tweak the randomization of the distance between structures in the sky.",
                        "Smaller values will result in structures being more on a grid, and larger values will result in them being less predictably placed."),
                fileConfiguration, "maxOffsetSky", 5);
        maxOffsetLiquid = ConfigurationEngine.setInt(
                List.of(
                        "Used to tweak the randomization of the distance between structures on oceans.",
                        "Smaller values will result in structures being more on a grid, and larger values will result in them being less predictably placed."),
                fileConfiguration, "maxOffsetLiquid", 5);
        maxOffsetDungeon = ConfigurationEngine.setInt(
                List.of(
                        "Used to tweak the randomization of the distance between dungeons.",
                        "Smaller values will result in dungeons being more on a grid, and larger values will result in them being less predictably placed."),
                fileConfiguration, "maxOffsetDungeonV2", 18);

        ConfigurationEngine.fileSaverOnlyDefaults(fileConfiguration, file);
    }
}