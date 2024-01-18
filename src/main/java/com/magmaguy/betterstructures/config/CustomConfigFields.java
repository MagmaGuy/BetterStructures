package com.magmaguy.betterstructures.config;


import com.magmaguy.betterstructures.util.ChatColorConverter;
import com.magmaguy.betterstructures.util.WarningMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

public class CustomConfigFields implements CustomConfigFieldsInterface {

    protected String filename;
    protected boolean isEnabled;
    protected FileConfiguration fileConfiguration;
    protected File file;

    /**
     * Used by plugin-generated files (defaults)
     *
     * @param filename
     * @param isEnabled
     */
    public CustomConfigFields(String filename, boolean isEnabled) {
        this.filename = filename.contains(".yml") ? filename : filename + ".yml";
        this.isEnabled = isEnabled;
    }

    public String getFilename() {
        return filename;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public FileConfiguration getFileConfiguration() {
        return fileConfiguration;
    }

    public void setFileConfiguration(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public void processConfigFields() {

    }

    protected boolean configHas(String configKey) {
        return fileConfiguration.contains(configKey);
    }

    protected String processString(String path, String value, String pluginDefault, boolean forceWriteDefault) {
        if (!configHas(path)) {
            if (forceWriteDefault || !Objects.equals(value, pluginDefault))
                fileConfiguration.addDefault(path, value);
            return value;
        }
        try {
            return ChatColorConverter.convert(fileConfiguration.getString(path));
        } catch (Exception ex) {
            new WarningMessage("File " + filename + " has an incorrect entry for " + path);
            new WarningMessage("Entry: " + value);
        }
        return value;
    }

    public List<String> processStringList(String path, List<String> value, List<String> pluginDefault, boolean forceWriteDefault) {
        if (!configHas(path)) {
            if (forceWriteDefault || value != pluginDefault)
                fileConfiguration.addDefault(path, value);
            return value;
        }
        try {
            List<String> list = new ArrayList<>();
            for (String string : fileConfiguration.getStringList(path))
                list.add(ChatColorConverter.convert(string));
            return list;
        } catch (Exception ex) {
            new WarningMessage("File " + filename + " has an incorrect entry for " + path);
            new WarningMessage("Entry: " + value);
        }
        return value;
    }

    /**
     * This not only gets a list of worlds, but gets a list of already loaded worlds. This might cause issues if the worlds
     * aren't loaded when the code for getting worlds runs.
     *
     * @param path          Configuration path
     * @param pluginDefault Default value - should be null or empty
     * @return Worlds from the list that are loaded at the time this runs, probably on startup
     */
    protected List<World> processWorldList(String path, List<World> value, List<World> pluginDefault, boolean forceWriteDefault) {
        if (!configHas(path)) {
            if (value != null && (forceWriteDefault || value != pluginDefault))
                processStringList(path, worldListToStringListConverter(value), worldListToStringListConverter(pluginDefault), forceWriteDefault);
            return value;
        }
        try {
            List<String> validWorldStrings = processStringList(path, worldListToStringListConverter(pluginDefault), worldListToStringListConverter(value), forceWriteDefault);
            List<World> validWorlds = new ArrayList<>();
            if (!validWorldStrings.isEmpty())
                for (String string : validWorldStrings) {
                    World world = Bukkit.getWorld(string);
                    if (world != null)
                        validWorlds.add(world);
                }
            return validWorlds;
        } catch (Exception ex) {
            new WarningMessage("File " + filename + " has an incorrect entry for " + path);
            new WarningMessage("Entry: " + value);
        }
        return value;
    }

    private List<String> worldListToStringListConverter(List<World> pluginDefault) {
        if (pluginDefault == null) return null;
        List<String> newList = new ArrayList<>();
        pluginDefault.forEach((element) -> newList.add(element.getName()));
        return newList;
    }


    protected <T extends Enum<T>> List<T> processEnumList(String path, List<T> value, List<T> pluginDefault, Class<T> enumClass, boolean forceWriteDefault) {
        if (!configHas(path)) {
            if (forceWriteDefault || value != pluginDefault)
                processStringList(path, enumListToStringListConverter(value), enumListToStringListConverter(pluginDefault), forceWriteDefault);
            return value;
        }
        try {
            List<T> newList = new ArrayList<>();
            List<String> stringList = processStringList(path, enumListToStringListConverter(value), enumListToStringListConverter(pluginDefault), forceWriteDefault);
            stringList.forEach(string -> {
                try {
                    newList.add(Enum.valueOf(enumClass, string.toUpperCase()));
                } catch (Exception ex) {
                    new WarningMessage(filename + " : " + "Value " + string + " is not a valid for " + path + " ! This may be due to your server version, or due to an invalid value!");
                }
            });
            return newList;
        } catch (
                Exception ex) {
            ex.printStackTrace();
            new WarningMessage("File " + filename + " has an incorrect entry for " + path);
            new WarningMessage("Entry: " + value);
        }
        return value;
    }

    private <T extends Enum<T>> List<String> enumListToStringListConverter(List<T> list) {
        if (list == null) return Collections.emptyList();
        List<String> newList = new ArrayList<>();
        list.forEach(element -> newList.add(element.toString()));
        return newList;
    }

    protected int processInt(String path, int value, int pluginDefault, boolean forceWriteDefault) {
        if (!configHas(path)) {
            if (forceWriteDefault || value != pluginDefault) fileConfiguration.addDefault(path, value);
            return value;
        }
        try {
            return fileConfiguration.getInt(path);
        } catch (Exception ex) {
            new WarningMessage("File " + filename + " has an incorrect entry for " + path);
            new WarningMessage("Entry: " + value);
        }
        return value;
    }

    protected long processLong(String path, long value, long pluginDefault, boolean forceWriteDefault) {
        if (!configHas(path)) {
            if (forceWriteDefault || value != pluginDefault) fileConfiguration.addDefault(path, value);
            return value;
        }
        try {
            return fileConfiguration.getLong(path);
        } catch (Exception ex) {
            new WarningMessage("File " + filename + " has an incorrect entry for " + path);
            new WarningMessage("Entry: " + value);
        }
        return value;
    }


    protected double processDouble(String path, double value, double pluginDefault, boolean forceWriteDefault) {
        if (!configHas(path)) {
            if (forceWriteDefault || value != pluginDefault) fileConfiguration.addDefault(path, value);
            return value;
        }
        try {
            return fileConfiguration.getDouble(path);
        } catch (Exception ex) {
            new WarningMessage("File " + filename + " has an incorrect entry for " + path);
            new WarningMessage("Entry: " + value);
        }
        return value;
    }

    protected Double processDouble(String path, Double value, Double pluginDefault, boolean forceWriteDefault) {
        if (!configHas(path)) {
            if (forceWriteDefault || !Objects.equals(value, pluginDefault)) fileConfiguration.addDefault(path, value);
            return value;
        }
        try {
            return fileConfiguration.getDouble(path);
        } catch (Exception ex) {
            new WarningMessage("File " + filename + " has an incorrect entry for " + path);
            new WarningMessage("Entry: " + value);
        }
        return value;
    }

    protected boolean processBoolean(String path, boolean value, boolean pluginDefault, boolean forceWriteDefault) {
        if (!configHas(path)) {
            if (forceWriteDefault || value != pluginDefault) fileConfiguration.addDefault(path, value);
            return value;
        }
        try {
            return fileConfiguration.getBoolean(path);
        } catch (Exception ex) {
            new WarningMessage("File " + filename + " has an incorrect entry for " + path);
            new WarningMessage("Entry: " + value);
        }
        return value;
    }

    public <T extends Enum<T>> T processEnum(String path, T value, T pluginDefault, Class<T> enumClass, boolean forceWriteDefault) {
        if (!configHas(path)) {
            if (forceWriteDefault || value != pluginDefault) {
                String valueString = null;
                if (value != null)
                    valueString = value.toString().toUpperCase();
                String pluginDefaultString = null;
                if (pluginDefault != null)
                    pluginDefaultString = pluginDefault.toString().toUpperCase();
                processString(path, valueString, pluginDefaultString, forceWriteDefault);
            }
            return value;
        }
        try {
            return Enum.valueOf(enumClass, fileConfiguration.getString(path).toUpperCase());
        } catch (Exception ex) {
            new WarningMessage("File " + filename + " has an incorrect entry for " + path);
            new WarningMessage("Entry: " + value);
        }
        return value;
    }

    private String itemStackDeserializer(ItemStack itemStack) {
        if (itemStack == null) return null;
        return itemStack.getType().toString();
    }

    public Map<String, Object> processMap(String path, Map<String, Object> value) {
        if (!configHas(path) && value != null) {
            fileConfiguration.addDefault(path, value);
            fileConfiguration.createSection(path, value);
        }
        if (fileConfiguration.get(path) == null)
            return Collections.emptyMap();
        return fileConfiguration.getConfigurationSection(path).getValues(false);
    }

    protected org.bukkit.util.Vector processVector(String path, org.bukkit.util.Vector value, org.bukkit.util.Vector pluginDefault, boolean forceWriteDefault) {
        if (!configHas(path)) {
            if (forceWriteDefault || !Objects.equals(value, pluginDefault))
                if (pluginDefault != null) {
                    String vectorString = value.getX() + "," + value.getY() + "," + value.getZ();
                    fileConfiguration.addDefault(path, vectorString);
                }
            return value;
        }
        try {
            String string = fileConfiguration.getString(path);
            if (string == null) return null;
            String[] strings = string.split(",");
            if (strings.length < 3) {
                new WarningMessage("File " + filename + " has an incorrect entry for " + path);
                return null;
            }
            return new Vector(Double.parseDouble(strings[0]), Double.parseDouble(strings[1]), Double.parseDouble(strings[2]));
        } catch (Exception ex) {
            new WarningMessage("File " + filename + " has an incorrect entry for " + path);
            new WarningMessage("Entry: " + value);
        }
        return null;
    }

}

