package com.magmaguy.betterstructures.config.schematics;

import com.magmaguy.betterstructures.config.CustomConfigFields;
import com.magmaguy.betterstructures.config.generators.GeneratorConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

public class SchematicConfigField extends CustomConfigFields {


    @Getter
    @Setter
    private double weight = 1;
    @Getter
    @Setter
    private String generatorConfigFilename = "";
    @Getter
    @Setter
    private GeneratorConfigFields generatorConfigFields;
    @Getter
    @Setter
    private Material pedestalMaterial = null;


    /**
     * Used by plugin-generated files (defaults)
     *
     * @param filename
     * @param isEnabled
     */
    public SchematicConfigField(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    @Override
    public void processConfigFields() {
        this.isEnabled = processBoolean("isEnabled", isEnabled, true, true);
        this.weight = processDouble("weight", weight, 1, true);
        this.pedestalMaterial = processEnum("pedestalMaterial", pedestalMaterial, null, Material.class, false);
        this.generatorConfigFilename = processString("generatorConfigFilename", generatorConfigFilename, null, true);
        this.generatorConfigFields = GeneratorConfig.getConfigFields(generatorConfigFilename);
    }
}
