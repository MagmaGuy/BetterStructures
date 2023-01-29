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
    @Getter
    private static boolean newBuildingWarn;
    @Getter
    private static String regionProtectedMessage;

    private static File file;
    private static FileConfiguration fileConfiguration;


    public static void initializeConfig() {
        file = ConfigurationEngine.fileCreator("config.yml");
        fileConfiguration = ConfigurationEngine.fileConfigurationCreator(file);
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
        airStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "airStructuresPerThousandChunks", 0.5);
        oceanStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "oceanStructuresPerThousandChunks", 0.5);
        landStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "landStructuresPerThousandChunks", 2.0);
        shallowUndergroundStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "shallowUndergroundStructuresPerThousandChunks", 2.0);
        deepUndergroundStructuresPerThousandChunks = ConfigurationEngine.setDouble(fileConfiguration, "deepUndergroundStructuresPerThousandChunks", 2.0);
        newBuildingWarn = ConfigurationEngine.setBoolean(fileConfiguration, "warnAdminsAboutNewBuildings", true);
        regionProtectedMessage = ConfigurationEngine.setString(fileConfiguration, "regionProtectedMessage", "&8[BetterStructures] &cDefeat the zone's bosses to edit blocks!");

        ConfigurationEngine.fileSaverOnlyDefaults(fileConfiguration, file);
    }

    public static boolean toggleWarnings() {
        newBuildingWarn = !newBuildingWarn;
        ConfigurationEngine.writeValue(newBuildingWarn, file, fileConfiguration, "warnAdminsAboutNewBuildings");
        return newBuildingWarn;
    }

}

