package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.config.spawnpools.SpawnPoolsConfig;
import com.magmaguy.betterstructures.config.spawnpools.SpawnPoolsConfigFields;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfig;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfigFields;
import com.magmaguy.elitemobs.mobconstructor.custombosses.InstancedBossEntity;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedConstruction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModularWorldDifficultyTest {
    @TempDir Path folder;
    private World world;
    private ServerMock server;

    @BeforeEach void open() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("modular-difficulty");
    }

    @AfterEach void close() { MockBukkit.unmock(); }

    @ParameterizedTest(name = "{displayName} offset=({0},{1},{2}), difficulty={3}")
    @CsvSource({
            "0,0,0,2", "63.999,200,63.999,2", "-63.999,-100,-63.999,2",
            "64,0,0,1", "0,0,-64,1", "-64,500,64,1",
            "127.999,0,127.999,1", "128,0,0,0", "-128,0,128,0",
            "0,-1000,-192,0", "10000,0,10000,0"
    })
    void squareBandsUseTheTranslatedFootprintAndIgnoreHeight(double x, double y, double z, String difficulty) {
        Location center = new Location(world, -245.5, 16, 811.5);
        ModularWorld generated = new ModularWorld(world, folder.toFile(), List.of(), center, 384);

        assertEquals(difficulty, generated.getDifficultyId(center.clone().add(x, y, z)));
    }

    @Test
    void preservesFractionalGeometryAndDoesNotExposeItsMutableCenter() {
        Location supplied = new Location(world, -17.5, 20, 39.5);
        ModularWorld generated = new ModularWorld(world, folder.toFile(), List.of(), supplied, 99);
        supplied.add(1000, 0, 1000);
        generated.getCenter().add(-1000, 0, -1000);

        assertEquals(new Location(world, -17.5, 20, 39.5), generated.getCenter());
        assertEquals(99D, generated.getHorizontalSize());
        assertEquals("2", generated.getDifficultyId(new Location(world, -1.001, 200, 39.5)));
        assertEquals("1", generated.getDifficultyId(new Location(world, -1, 200, 39.5)));
        assertEquals("0", generated.getDifficultyId(new Location(world, 15.5, 200, 39.5)));
    }

    @Test
    void rejectsInvalidGeometryAndForeignWorldQueries() {
        Location center = new Location(world, 10, 20, 30);
        for (double size : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,
                    () -> new ModularWorld(world, folder.toFile(), List.of(), center, size));
        World other = server.addSimpleWorld("foreign");
        assertThrows(IllegalArgumentException.class, () -> new ModularWorld(world, folder.toFile(), List.of(),
                new Location(other, 10, 20, 30), 384));
        assertThrows(IllegalArgumentException.class, () -> new ModularWorld(world, folder.toFile(), List.of(),
                new Location(world, Double.NaN, 20, 30), 384));
        ModularWorld generated = new ModularWorld(world, folder.toFile(), List.of(), center, 384);
        assertThrows(IllegalArgumentException.class, () -> generated.getDifficultyId(new Location(other, 10, 20, 30)));
        assertThrows(IllegalArgumentException.class,
                () -> generated.getDifficultyId(new Location(world, Double.POSITIVE_INFINITY, 20, 30)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void spawnPoolsKeepOneLevelAndPassEachLocationsDifficultyWithoutMutatingTheBossDefinition() {
        String poolId = "modular_difficulty_fixture.yml", bossId = "modular_difficulty_boss.yml";
        SpawnPoolsConfigFields pool = mock(SpawnPoolsConfigFields.class);
        when(pool.getPoolStrings()).thenReturn(List.of(bossId));
        when(pool.getMaxLevel()).thenReturn(40);
        CustomBossesConfigFields boss = new CustomBossesConfigFields(bossId, EntityType.ZOMBIE, true, "Fixture", "7");
        boss.setInstanced(true);
        Map<String, CustomBossesConfigFields> bosses = (Map<String, CustomBossesConfigFields>) CustomBossesConfig.getCustomBosses();
        SpawnPoolsConfigFields previousPool = SpawnPoolsConfig.getSpawnPoolConfigFields().put(poolId, pool);
        CustomBossesConfigFields previousBoss = bosses.put(bossId, boss);
        Location center = new Location(world, 400, 20, -800);
        ModularWorld generated = new ModularWorld(world, folder.toFile(), List.of(), center, 384);
        List<Location> positions = List.of(center.clone().add(150, 30, 150), center.clone().add(90, -5, 0),
                center.clone().add(-30, 150, 30));
        List<List<?>> constructorArguments = new ArrayList<>();
        // This is the external EliteMobs boundary. BS's sign parsing, deferred pool routing and geometry run unchanged.
        // EliteMobs separately tests its real constructor and difficulty-owned combat state.
        try (MockedConstruction<InstancedBossEntity> captured = mockConstruction(InstancedBossEntity.class,
                (entity, context) -> constructorArguments.add(context.arguments()))) {
            for (Location position : positions)
                generated.spawnOtherEntitiesAt(new ModulePasting.InterpretedSign(position, List.of("[pool:modular_difficulty_fixture]")));
            assertTrue(captured.constructed().isEmpty(), "Instance mobs must wait for the completed world's owner to spawn them");

            List<InstancedBossEntity> spawned = generated.spawnInstancedEntities();

            assertEquals(3, spawned.size());
            for (int index = 0; index < spawned.size(); index++) {
                assertEquals(List.of(boss, positions.get(index).clone().add(.5, 0, .5), 40, Integer.toString(index)), constructorArguments.get(index));
                verify(spawned.get(index)).spawn(true);
                verify(spawned.get(index)).addCustomData(new NamespacedKey("betterstructures", "spawnpool"), poolId);
            }
            assertEquals(7, boss.getLevel(), "Spawning difficulty variants must not rewrite the shared boss definition");
            assertTrue(boss.isInstanced());
        } finally {
            if (previousPool == null) SpawnPoolsConfig.getSpawnPoolConfigFields().remove(poolId);
            else SpawnPoolsConfig.getSpawnPoolConfigFields().put(poolId, previousPool);
            if (previousBoss == null) bosses.remove(bossId);
            else bosses.put(bossId, previousBoss);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void immediatePoolSpawnsUseTheBlockCenterWithoutMovingTheAuthoredMarker() {
        String poolId = "center_fixture.yml", bossId = "center_boss.yml";
        SpawnPoolsConfigFields pool = mock(SpawnPoolsConfigFields.class);
        when(pool.getPoolStrings()).thenReturn(List.of(bossId));
        CustomBossesConfigFields boss = new CustomBossesConfigFields(bossId, EntityType.VINDICATOR, true, "Fixture", "7");
        Map<String, CustomBossesConfigFields> bosses = (Map<String, CustomBossesConfigFields>) CustomBossesConfig.getCustomBosses();
        SpawnPoolsConfigFields previousPool = SpawnPoolsConfig.getSpawnPoolConfigFields().put(poolId, pool);
        CustomBossesConfigFields previousBoss = bosses.put(bossId, boss);
        Location marker = new Location(world, -120, 16, 41);
        ModularWorld generated = new ModularWorld(world, folder.toFile(), List.of(), new Location(world, 0, 16, 0), 384);
        try (var captured = mockConstruction(com.magmaguy.elitemobs.mobconstructor.custombosses.CustomBossEntity.class)) {
            generated.spawnOtherEntitiesAt(new ModulePasting.InterpretedSign(marker, List.of("[pool:center_fixture]")));
            verify(captured.constructed().getFirst()).spawn(new Location(world, -119.5, 16, 41.5), true);
            assertEquals(new Location(world, -120, 16, 41), marker);
        } finally {
            if (previousPool == null) SpawnPoolsConfig.getSpawnPoolConfigFields().remove(poolId);
            else SpawnPoolsConfig.getSpawnPoolConfigFields().put(poolId, previousPool);
            if (previousBoss == null) bosses.remove(bossId);
            else bosses.put(bossId, previousBoss);
        }
    }

    @Test
    void playerSpawnsAreCenteredWhileContainerMarkersRemainBlockCoordinates() {
        Location marker = new Location(world, -120, 16, 41);
        ModularWorld generated = new ModularWorld(world, folder.toFile(), List.of(
                new ModulePasting.InterpretedSign(marker, List.of("[spawn]", "[chest]", "[barrel]"))),
                new Location(world, 0, 16, 0), 384);

        assertEquals(List.of(new Location(world, -119.5, 16, 41.5)), generated.getSpawnLocations());
        assertEquals(List.of(marker), generated.getChestLocations());
        assertEquals(List.of(marker), generated.getBarrelLocations());
        assertEquals(new Location(world, -120, 16, 41), marker);
    }
}
