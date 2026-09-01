package com.mira.spawners.util;

public final class StackMath {
    private StackMath() {
    }

    public static int clamp(int value, int minimum, int maximum) {
        if (maximum < minimum) {
            throw new IllegalArgumentException("maximum must be >= minimum");
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static Transfer transfer(int current, int incoming, int maximum) {
        if (current < 0 || incoming < 0 || maximum < 1) {
            throw new IllegalArgumentException("Stack values must be non-negative and maximum must be positive");
        }
        int capacity = Math.max(0, maximum - current);
        int accepted = Math.min(capacity, incoming);
        return new Transfer(accepted, incoming - accepted);
    }

    public record Transfer(int accepted, int remainder) {
    }
}
