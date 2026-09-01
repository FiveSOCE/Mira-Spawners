package com.mira.spawners.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StackMathTest {
    @Test
    void transfersOnlyAvailableSpawnerCapacity() {
        StackMath.Transfer transfer = StackMath.transfer(60, 10, 64);
        assertEquals(4, transfer.accepted());
        assertEquals(6, transfer.remainder());
    }

    @Test
    void fullStackAcceptsNothing() {
        StackMath.Transfer transfer = StackMath.transfer(64, 12, 64);
        assertEquals(0, transfer.accepted());
        assertEquals(12, transfer.remainder());
    }

    @Test
    void clampHonoursBounds() {
        assertEquals(1, StackMath.clamp(-5, 1, 64));
        assertEquals(37, StackMath.clamp(37, 1, 64));
        assertEquals(64, StackMath.clamp(100, 1, 64));
    }

    @Test
    void rejectsInvalidTransferValues() {
        assertThrows(IllegalArgumentException.class, () -> StackMath.transfer(-1, 1, 64));
        assertThrows(IllegalArgumentException.class, () -> StackMath.transfer(1, -1, 64));
        assertThrows(IllegalArgumentException.class, () -> StackMath.transfer(1, 1, 0));
    }
}
