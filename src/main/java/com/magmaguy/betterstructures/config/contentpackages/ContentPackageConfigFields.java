package com.magmaguy.betterstructures.config.contentpackages;

import com.magmaguy.magmacore.config.CustomConfigFields;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ContentPackageConfigFields extends CustomConfigFields {
    @Getter
    private int version = 0;
    @Getter
    private String name;
    @Getter
    private List<String> description;
    @Getter
    private String downloadLink;
    @Getter
    private String folderName;
    @Getter
    @Setter
    private ContentPackageType contentPackageType;

    public enum ContentPackageType {
        STRUCTURE,
        MODULAR
    }

    public ContentPackageConfigFields(String filename,
                                      boolean isEnabled,
                                      String name,
                                      List<String> description,
                                      String downloadLink,
                                      String folderName) {
        super(filename, isEnabled);
        this.name = name;
        this.description = description;
        this.downloadLink = downloadLink;
        this.folderName = folderName;
    }

    public ContentPackageConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    @Override
    public void processConfigFields() {
        this.isEnabled = processBoolean("isEnabled", isEnabled, true, true);
        this.name = processString("name", name, null, true);
        this.description = processStringList("description", description, null, true);
        this.downloadLink = processString("downloadLink" , downloadLink, downloadLink, false);
        this.version = processInt("version", version, 0, true);
        this.folderName = processString("folderNameV2", folderName, null, true);
        this.contentPackageType = processEnum("contentPackageType", contentPackageType, ContentPackageType.STRUCTURE, ContentPackageType.class, false);
    }
}
