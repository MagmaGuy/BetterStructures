package com.magmaguy.betterstructures.buildingfitter.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class LocationProjectorTest {
    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void projectsSchematicOffsetWithoutMutatingAnchorLocation() {
        Location anchor = new Location(world, 100, 64, -20);

        Location projected = LocationProjector.project(anchor, new Vector(-2, 5, 3));

        assertNotSame(anchor, projected);
        assertEquals(new Location(world, 98, 69, -17), projected);
        assertEquals(new Location(world, 100, 64, -20), anchor);
    }

    @Test
    void projectsRelativeBlockLocationAfterSchematicOffset() {
        Location anchor = new Location(world, 100, 64, -20);

        Location projected = LocationProjector.project(anchor, new Vector(-2, 5, 3), new Vector(4, -1, 6));

        assertEquals(new Location(world, 102, 68, -11), projected);
        assertEquals(new Location(world, 100, 64, -20), anchor);
    }
}
