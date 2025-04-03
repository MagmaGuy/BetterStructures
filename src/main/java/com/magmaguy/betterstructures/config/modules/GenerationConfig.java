package com.magmaguy.betterstructures.config.modules;

import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Configuration class for the generator.
 */
public class GenerationConfig {
    @Getter private final String worldName;
    @Getter private final int radius;
    @Getter private final int chunkSize;
    @Getter private final boolean debug;
    @Getter private final int interval;
    @Getter private final boolean slowGeneration;
    @Getter private final String startingModule;
    @Getter private final Player player;
    @Getter private final int massPasteSize;
    @Getter private final boolean edgeModules;
    @Getter private final String spawnPoolSuffix;

    private GenerationConfig(Builder builder) {
        this.worldName = builder.worldName;
        this.radius = builder.radius;
        this.chunkSize = builder.chunkSize;
        this.debug = builder.debug;
        this.interval = builder.interval;
        this.slowGeneration = builder.slowGeneration;
        this.startingModule = builder.startingModule;
        this.player = builder.player;
        this.massPasteSize = builder.massPasteSize;
        this.edgeModules = builder.edgeModules;
        this.spawnPoolSuffix = builder.spawnPoolSuffix;
    }

    /**
     * Builder for GenerationConfig.
     */
    public static class Builder {
        private static final int DEFAULT_CHUNK_SIZE = 128;

        private final String worldName;
        private final int radius;
        private int chunkSize = DEFAULT_CHUNK_SIZE;
        private boolean debug = false;
        private int interval = 1;
        private boolean slowGeneration = false;
        private String startingModule;
        private Player player;
        private int massPasteSize = DefaultConfig.getModularChunkPastingSpeed();
        private boolean edgeModules = false;
        private String spawnPoolSuffix = null;

        public Builder(String worldName, int radius) {
            this.worldName = worldName;
            this.radius = radius;
        }

        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder interval(int interval) {
            this.interval = interval;
            return this;
        }

        public Builder slowGeneration(boolean slowGeneration) {
            this.slowGeneration = slowGeneration;
            return this;
        }

        public Builder startingModule(String startingModule) {
            this.startingModule = startingModule;
            return this;
        }

        public Builder startingModules(List<String> startingModules) {
            this.startingModule = startingModules.get(ThreadLocalRandom.current().nextInt(startingModules.size()))+ "_rotation_0";
//            Logger.debug("Using starting module " + startingModule);
            return this;
        }

        public Builder player(Player player) {
            this.player = player;
            return this;
        }

        public Builder massPasteSize(int massPasteSize) {
            this.massPasteSize = massPasteSize;
            return this;
        }

        public Builder edgeModules(boolean edgeModules) {
            this.edgeModules = edgeModules;
            return this;
        }

        public Builder spawnPoolSuffix(String spawnPoolSuffix){
            this.spawnPoolSuffix = spawnPoolSuffix;
            return this;
        }

        public GenerationConfig build() {
            validate();
            return new GenerationConfig(this);
        }


        private void validate() {
            if (worldName == null || worldName.isEmpty()) {
                throw new IllegalArgumentException("World name must be specified");
            }
            if (radius <= 0) {
                throw new IllegalArgumentException("Radius must be positive");
            }
            if (chunkSize <= 0) {
                throw new IllegalArgumentException("Chunk size must be positive");
            }
            if (interval <= 0) {
                throw new IllegalArgumentException("Interval must be positive");
            }
            if (massPasteSize <= 0) {
                throw new IllegalArgumentException("Mass paste size must be positive");
            }
        }
    }
}