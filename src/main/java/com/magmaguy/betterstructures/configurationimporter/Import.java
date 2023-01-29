package com.magmaguy.betterstructures.configurationimporter;

import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.thirdparty.EliteMobs;
import com.magmaguy.betterstructures.util.InfoMessage;
import com.magmaguy.betterstructures.util.WarningMessage;
import com.magmaguy.betterstructures.util.ZipFile;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Import {
    //let's get some files
    private static File importFolder;
    private static File schematicsFolder;
    private static File generatorsFolder;
    private static File treasuresFolder;
    private static File eliteMobsBossFolder;


    public static void initialize() {
        String importFolderDirectory = MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "imports";
        String schematicsFolderDirectory = MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "schematics";
        String generatorsFolderDirectory = MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "generators";
        String treasuresFolderDirectory = MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "treasures";

        String eliteMobsBossFolderDirectory = MetadataHandler.PLUGIN.getDataFolder().getParentFile().getAbsolutePath() + File.separatorChar + "EliteMobs" + File.separatorChar;

        importFolder = new File(importFolderDirectory);
        //Create imports folder if there isn't one yet
        if (!importFolder.exists()) importFolder.mkdir();

        //BetterStructures files are already initialized at this point
        schematicsFolder = new File(schematicsFolderDirectory);
        generatorsFolder = new File(generatorsFolderDirectory);
        treasuresFolder = new File(treasuresFolderDirectory);

        //EliteMobs may not be installed, but if content is installed that uses its files a directory will be created as a placeholder in preparation for EliteMobs
        eliteMobsBossFolder = new File(eliteMobsBossFolderDirectory);

        //Time to import
        importPackages();
    }

    private static void importPackages() {
        boolean eliteFilesFound = false;
        for (File zippedFile : importFolder.listFiles()) {
            File unzippedFile;
            try {
                if (zippedFile.getName().contains(".zip"))
                    unzippedFile = ZipFile.unzip(zippedFile.getName());
                else unzippedFile = zippedFile;
            } catch (Exception e) {
                new WarningMessage("Failed to unzip config file " + zippedFile.getName() + " ! Tell the dev!");
                e.printStackTrace();
                continue;
            }
            try {
                for (File file : unzippedFile.listFiles()) {
                    switch (file.getName()) {
                        case "schematics":
                            moveDirectory(file, schematicsFolder.toPath(), false);
                            break;
                        case "generators":
                            moveDirectory(file, generatorsFolder.toPath(), false);
                            break;
                        case "treasures":
                            moveDirectory(file, treasuresFolder.toPath(), false);
                            break;
                        case "elitemobs":
                            if (!eliteMobsBossFolder.exists()) eliteMobsBossFolder.mkdirs();
                            moveDirectory(file, eliteMobsBossFolder.toPath(), false);
                            eliteFilesFound = true;
                            break;
                        default:
                            new WarningMessage("Directory " + file.getName() + " for zipped file " + zippedFile.getName() + " was not a recognized directory for the file import system! Was the zipped file packaged correctly?");
                    }
                    deleteDirectory(file);
                }
            } catch (Exception e) {
                new WarningMessage("Failed to move files from " + zippedFile.getName() + " ! Tell the dev!");
                e.printStackTrace();
                continue;
            }
            try {
                unzippedFile.delete();
                zippedFile.delete();
            } catch (Exception ex) {
                new WarningMessage("Failed to delete zipped file " + zippedFile.getName() + "! Tell the dev!");
                ex.printStackTrace();
            }
        }

        //Reload EliteMobs to use the newly imported files
        if (eliteFilesFound && Bukkit.getPluginManager().getPlugin("EliteMobs") != null)
            EliteMobs.Reload();
    }

    private static void deleteDirectory(File file) {
        if (file == null)
            return;
        if (file.isDirectory())
            for (File iteratedFile : file.listFiles())
                if (iteratedFile != null)
                    deleteDirectory(iteratedFile);
        new InfoMessage("Cleaning up " + file.getPath());
        file.delete();
    }


    private static void moveDirectory(File unzippedDirectory, Path targetPath, boolean force) {
        for (File file : unzippedDirectory.listFiles())
            try {
                new InfoMessage("Adding " + file.getCanonicalPath());
                moveFile(file, targetPath, force);
            } catch (Exception exception) {
                new WarningMessage("Failed to move directories for " + file.getName() + "! Tell the dev!");
                exception.printStackTrace();
            }
    }

    private static void moveFile(File file, Path targetPath, boolean force) {
        try {
            if (file.isDirectory()) {
                if (Paths.get(targetPath + "" + File.separatorChar + file.getName()).toFile().exists())
                    for (File iteratedFile : file.listFiles())
                        moveFile(iteratedFile, Paths.get(targetPath + "" + File.separatorChar + file.getName()), force);
                else
                    Files.move(file.toPath(), Paths.get(targetPath + "" + File.separatorChar + file.getName()), StandardCopyOption.REPLACE_EXISTING);
            } else if (targetPath.toFile().exists())
                Files.move(file.toPath(), Paths.get(targetPath + "" + File.separatorChar + file.getName()), StandardCopyOption.REPLACE_EXISTING);
            else if (!Paths.get(targetPath + "" + File.separatorChar + file.getName()).toFile().exists() && force) {
                File newFile = Paths.get(targetPath + "" + File.separatorChar + file.getName()).toFile();
                newFile.mkdirs();
                newFile.createNewFile();
                Files.move(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            new WarningMessage("Failed to move directories for " + file.getName() + "! Tell the dev!");
            exception.printStackTrace();
        }
    }
}

