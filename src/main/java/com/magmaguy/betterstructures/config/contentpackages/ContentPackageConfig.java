package com.magmaguy.betterstructures.config.contentpackages;

import com.magmaguy.betterstructures.content.BSPackage;
import com.magmaguy.magmacore.config.CustomConfig;
import lombok.Getter;

import java.util.HashMap;

public class ContentPackageConfig extends CustomConfig {

    @Getter
    private static final HashMap<String, ContentPackageConfigFields> contentPackages = new HashMap<>();

    public ContentPackageConfig() {
        super("content_packages","com.magmaguy.betterstructures.config.contentpackages.premade", ContentPackageConfigFields.class);
        for (String key : super.getCustomConfigFieldsHashMap().keySet()) {
            contentPackages.put(key, (ContentPackageConfigFields) super.getCustomConfigFieldsHashMap().get(key));
            new BSPackage((ContentPackageConfigFields) super.getCustomConfigFieldsHashMap().get(key));
        }
    }
}
