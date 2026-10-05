package org.kingdoms.jails.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurationsTest {
    @Test
    void parsesUnitsAndCombinations() {
        assertEquals(30_000L, Durations.parse("30s"));
        assertEquals(600_000L, Durations.parse("10m"));
        assertEquals(5_400_000L, Durations.parse("1h30m"));
        assertEquals(216_000_000L, Durations.parse("2d 12h"));
        assertEquals(604_800_000L, Durations.parse("1w"));
        assertEquals(90_000L, Durations.parse("90"));
        assertEquals(0L, Durations.parse("0"));
    }

    @Test
    void rejectsGarbage() {
        assertEquals(-1, Durations.parse("abc"));
        assertEquals(-1, Durations.parse(""));
        assertEquals(-1, Durations.parse(null));
        assertEquals(-1, Durations.parse("m"));
        assertEquals(-1, Durations.parse("5x"));
        assertEquals(42, Durations.parseOr("nope", 42));
    }

    @Test
    void knowsWhenAUnitIsWritten() {
        assertTrue(Durations.hasUnit("5m"));
        assertFalse(Durations.hasUnit("500"));
        assertFalse(Durations.hasUnit("mm"));
    }
}
