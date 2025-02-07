package com.magmaguy.betterstructures.config.modulegenerators;

import com.magmaguy.magmacore.config.CustomConfigFields;
import lombok.Getter;

import java.util.List;

public class ModuleGeneratorsConfigFields extends CustomConfigFields {
    @Getter
    protected int radius;
    @Getter
    protected boolean edges;
    @Getter
    protected List<String> startModules;
    @Getter
    protected int minChunkY;
    @Getter
    protected int maxChunkY;
    @Getter
    protected int chunkSize;
    @Getter
    protected boolean debug;
    @Getter
    protected boolean generateInstantly;
    @Getter
    protected int delayBetweenPastes;
    @Getter
    protected int pasteBatchSize;

    /**
     * Used by plugin-generated files (defaults)
     *
     * @param filename
     * @param isEnabled
     */
    public ModuleGeneratorsConfigFields(String filename, boolean isEnabled) {
        super(filename, isEnabled);
    }

    public ModuleGeneratorsConfigFields(String filename) {
        super(filename, true);
    }

    @Override
    public void processConfigFields() {
        this.radius = processInt("radius", radius, 1, true);
        this.edges = processBoolean("edges", edges, false, true);
        this.startModules = processStringList("startModule", startModules, null, true);
        this.minChunkY = processInt("minChunkY", minChunkY, 0, true);
        this.maxChunkY = processInt("maxChunkY", maxChunkY, 0, true);
        this.chunkSize = processInt("chunkSize", chunkSize, 16, true);
        this.debug = processBoolean("debug", debug, false, true);
        this.generateInstantly = processBoolean("generateInstantly", generateInstantly, true, true);
        this.delayBetweenPastes = processInt("delayBetweenPastes", delayBetweenPastes, 20, true);
        this.pasteBatchSize = processInt("pasteBatchSize", pasteBatchSize, 1, true);
    }
}
