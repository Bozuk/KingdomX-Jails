package org.kingdoms.jails.managers;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestrictionMatchingTest {
    @Test
    void commandsAreNormalized() {
        assertEquals("home bed", RestrictionListener.normalize("/Essentials:Home   bed"));
        assertEquals("k jail status", RestrictionListener.normalize("/k jail status"));
    }

    @Test
    void commandEntriesMatchWholeWords() {
        List<String> entries = Arrays.asList("k home", "tpa", "/spawn");
        assertTrue(RestrictionListener.matchesCommand("k home", entries));
        assertTrue(RestrictionListener.matchesCommand("k home bed", entries));
        assertTrue(RestrictionListener.matchesCommand("tpa steve", entries));
        assertTrue(RestrictionListener.matchesCommand("spawn", entries));
        assertFalse(RestrictionListener.matchesCommand("k homes", entries));
        assertFalse(RestrictionListener.matchesCommand("k", entries));
    }

    @Test
    void materialPatterns() {
        List<String> patterns = Arrays.asList("*_BED", "DIAMOND_*", "*SHULKER*", "BREAD");
        assertTrue(RestrictionListener.matches("RED_BED", patterns));
        assertTrue(RestrictionListener.matches("DIAMOND_SWORD", patterns));
        assertTrue(RestrictionListener.matches("RED_SHULKER_BOX", patterns));
        assertTrue(RestrictionListener.matches("BREAD", patterns));
        assertFalse(RestrictionListener.matches("BREADS", patterns));
        assertFalse(RestrictionListener.matches("BEDROCK", patterns));
    }
}
