package com.magmaguy.betterstructures.buildingfitter.util;

import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields.StructureType;
import com.magmaguy.betterstructures.config.schematics.SchematicConfigField;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Generators can limit their structures to a distance ring round block coordinates 0, 0 (GitHub #72). */
final class SchematicPickerTest {
    private final World world = mock(World.class);
    private final List<String> warnings = new ArrayList<>();
    private final Handler logCapture = new Handler() {
        @Override
        public void publish(LogRecord record) {
            if (record.getLevel() == Level.WARNING) warnings.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };
    private ServerMock server;

    @BeforeEach
    void startServer() {
        server = MockBukkit.mock();
        server.getLogger().addHandler(logCapture);
        when(world.getBlockAt(any(Location.class))).thenReturn(mock(Block.class));
    }

    @AfterEach
    void stopServer() {
        SchematicContainer.shutdown();
        server.getLogger().removeHandler(logCapture);
        MockBukkit.unmock();
    }

    @Test
    void minimumAloneKeepsTheGeneratorAwayFromTheOrigin() {
        SchematicContainer far = register(generator("far.yml", "minDistanceFromOrigin: 1000"));

        assertEquals(List.of(), candidatesAt(999, 0));
        assertEquals(List.of(far), candidatesAt(1000, 0));
        assertEquals(List.of(far), candidatesAt(0, -1000));
        assertEquals(List.of(far), candidatesAt(-20_000_000, 20_000_000));
        assertNull(SchematicPicker.pick(at(8, 8), StructureType.SURFACE));
        assertSame(far, SchematicPicker.pick(at(5000, 5000), StructureType.SURFACE));
    }

    @Test
    void maximumAloneKeepsTheGeneratorNearTheOrigin() {
        SchematicContainer near = register(generator("near.yml", "maxDistanceFromOrigin: 500"));

        assertEquals(List.of(near), candidatesAt(0, 0));
        assertEquals(List.of(near), candidatesAt(-500, 0));
        assertEquals(List.of(near), candidatesAt(300, 400));
        assertEquals(List.of(), candidatesAt(301, 400));
        assertEquals(List.of(), candidatesAt(0, 501));
    }

    @Test
    void bothLimitsFormARingAndLeaveOtherGeneratorsEligible() {
        SchematicContainer ring = register(generator("ring.yml",
                "minDistanceFromOrigin: 1000\nmaxDistanceFromOrigin: 2000"));
        SchematicContainer anywhere = register(generator("anywhere.yml", ""));

        assertEquals(List.of(anywhere), candidatesAt(999, 0));
        assertEquals(List.of(ring, anywhere), candidatesAt(1000, 0));
        assertEquals(List.of(ring, anywhere), candidatesAt(0, -2000));
        assertEquals(List.of(anywhere), candidatesAt(1500, 1500));
        assertTrue(warnings.stream().noneMatch(warning -> warning.contains("DistanceFromOrigin")));
    }

    @Test
    void invertedPairWarnsOnceWithTheFileNameAndIsIgnored() {
        GeneratorConfigFields inverted = generator("inverted.yml",
                "minDistanceFromOrigin: 2000\nmaxDistanceFromOrigin: 1000");
        SchematicContainer container = register(inverted);

        List<String> distanceWarnings = warnings.stream()
                .filter(warning -> warning.contains("DistanceFromOrigin")).toList();
        assertEquals(1, distanceWarnings.size());
        assertTrue(distanceWarnings.get(0).contains("inverted.yml"));
        assertEquals(0, inverted.getMinDistanceFromOrigin());
        assertEquals(0, inverted.getMaxDistanceFromOrigin());
        assertEquals(List.of(container), candidatesAt(0, 0));
        assertEquals(List.of(container), candidatesAt(1500, 0));
        assertEquals(List.of(container), candidatesAt(50_000, 0));
    }

    @Test
    void unsetAndNonPositiveLimitsLeaveTheCandidateListUnchanged() {
        YamlConfiguration untouched = new YamlConfiguration();
        GeneratorConfigFields unset = generator("unset.yml", untouched);
        List<SchematicContainer> registered = List.of(
                register(unset),
                register(generator("zero.yml", "minDistanceFromOrigin: 0\nmaxDistanceFromOrigin: 0")),
                register(generator("negative.yml", "minDistanceFromOrigin: -5\nmaxDistanceFromOrigin: -1")));

        assertEquals(registered, candidatesAt(0, 0));
        assertEquals(registered, candidatesAt(8, -8));
        assertEquals(registered, candidatesAt(29_999_984, -29_999_984));
        // A generator file without the keys gains neither a value nor a default to save.
        assertFalse(untouched.contains("minDistanceFromOrigin"));
        assertFalse(untouched.contains("maxDistanceFromOrigin"));
        assertEquals(0, unset.getMinDistanceFromOrigin());
        assertEquals(0, unset.getMaxDistanceFromOrigin());
        assertTrue(warnings.stream().noneMatch(warning -> warning.contains("DistanceFromOrigin")));
    }

    private List<SchematicContainer> candidatesAt(int blockX, int blockZ) {
        return SchematicPicker.candidates(at(blockX, blockZ), StructureType.SURFACE);
    }

    private Location at(int blockX, int blockZ) {
        return new Location(world, blockX, 64, blockZ);
    }

    private static GeneratorConfigFields generator(String filename, String yaml) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return generator(filename, configuration);
    }

    private static GeneratorConfigFields generator(String filename, YamlConfiguration configuration) {
        GeneratorConfigFields generator = new GeneratorConfigFields(filename, true,
                new ArrayList<>(List.of(StructureType.SURFACE)));
        generator.setFileConfiguration(configuration);
        generator.processConfigFields();
        return generator;
    }

    private static SchematicContainer register(GeneratorConfigFields generator) {
        SchematicConfigField schematic = mock(SchematicConfigField.class);
        when(schematic.getGeneratorConfigFields()).thenReturn(generator);
        when(schematic.getWeight()).thenReturn(1D);
        Clipboard clipboard = mock(Clipboard.class);
        when(clipboard.getDimensions()).thenReturn(BlockVector3.ZERO);
        return new SchematicContainer(clipboard, generator.getFilename() + ".schem", schematic, generator.getFilename());
    }
}
