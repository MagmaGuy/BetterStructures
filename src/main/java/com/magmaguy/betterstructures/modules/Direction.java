package com.magmaguy.betterstructures.modules;

import com.magmaguy.magmacore.util.Logger;
import org.joml.Vector3i;

import javax.annotation.Nullable;

public enum Direction {
    NORTH, SOUTH, EAST, WEST, UP, DOWN;

    @Nullable
    public static Direction fromString(String s) {
        for (Direction border : Direction.values()) {
            if (border.name().equalsIgnoreCase(s)) {
                return border;
            }
        }
        return null;
    }

    public Vector3i getOffsetVector(){
        return switch (this) {
            case NORTH -> new Vector3i(0, 0, -1);
            case SOUTH -> new Vector3i(0, 0, 1);
            case EAST -> new Vector3i(1, 0, 0);
            case WEST -> new Vector3i(-1, 0, 0);
            case UP -> new Vector3i(0, 1, 0);
            case DOWN -> new Vector3i(0, -1, 0);
            default -> throw new IllegalArgumentException("Invalid BuildBorder");
        };
    }

    public Direction getOpposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
            case UP -> DOWN;
            case DOWN -> UP;
            default -> throw new IllegalArgumentException("Invalid BuildBorder");
        };
    }

    public static Direction transformDirection(Direction direction, Integer rotation) {
        if (rotation == null || rotation == 0) return direction;
        rotation = ((rotation % 360) + 360) % 360; //normalizes negative rotations
        return switch (rotation) {
            case 90 -> switch (direction) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                default -> direction;
            };
            case 180 -> switch (direction) {
                case NORTH -> Direction.SOUTH;
                case EAST -> Direction.WEST;
                case SOUTH -> Direction.NORTH;
                case WEST -> Direction.EAST;
                default -> direction;
            };
            case 270 -> switch (direction) {
                case NORTH -> Direction.WEST;
                case EAST -> Direction.NORTH;
                case SOUTH -> Direction.EAST;
                case WEST -> Direction.SOUTH;
                default -> direction;
            };
            default -> {
                Logger.warn("Invalid rotation detected! " + rotation);
                yield direction;
            }
        };
    }
}