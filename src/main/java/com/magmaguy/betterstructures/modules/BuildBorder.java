package com.magmaguy.betterstructures.modules;

import com.magmaguy.betterstructures.config.modules.ModulesConfigFields;

import javax.annotation.Nullable;

public enum BuildBorder {
    NORTH, SOUTH, EAST, WEST, UP, DOWN;

    @Nullable
    public static BuildBorder fromString(String s) {
        for (BuildBorder border : BuildBorder.values()) {
            if (border.name().equalsIgnoreCase(s)) {
                return border;
            }
        }
        return null;
    }

    public BuildBorder getOpposite() {
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

    public static BuildBorder transformDirection(BuildBorder direction, Integer rotation) {
        return switch (rotation % 360) {
            case 90 -> switch (direction) {
                case NORTH -> BuildBorder.EAST;
                case EAST -> BuildBorder.SOUTH;
                case SOUTH -> BuildBorder.WEST;
                case WEST -> BuildBorder.NORTH;
                default -> direction;
            };
            case 180 -> switch (direction) {
                case NORTH -> BuildBorder.SOUTH;
                case EAST -> BuildBorder.WEST;
                case SOUTH -> BuildBorder.NORTH;
                case WEST -> BuildBorder.EAST;
                default -> direction;
            };
            case 270 -> switch (direction) {
                case NORTH -> BuildBorder.WEST;
                case EAST -> BuildBorder.NORTH;
                case SOUTH -> BuildBorder.EAST;
                case WEST -> BuildBorder.SOUTH;
                default -> direction;
            };
            default -> direction; // 0 degrees or full rotation or up/down
        };
    }
}