package com.magmaguy.betterstructures.config.modules;

import org.joml.Vector3i;

import java.util.NoSuchElementException;

/**
 * Helper class for spiral iteration through the grid.
 */
public class SpiralIterator {
    private final int maxRadius;
    private int x = 0, z = 0;
    private int dx = 0, dz = -1;
    private final int maxIterations;
    private int iteration = 0;

    public SpiralIterator(int radius) {
        this.maxRadius = radius;
        this.maxIterations = (2 * radius + 1) * (2 * radius + 1);
    }

    public boolean hasNext() {
        return iteration < maxIterations;
    }

    public Vector3i next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        Vector3i current = new Vector3i(x, 0, z);

        if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
            // Turn the spiral
            int temp = dx;
            dx = -dz;
            dz = temp;
        }

        x += dx;
        z += dz;
        iteration++;

        return current;
    }
}
