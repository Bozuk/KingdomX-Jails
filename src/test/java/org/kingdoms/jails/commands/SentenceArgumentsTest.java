package org.kingdoms.jails.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentenceArgumentsTest {
    private static SentenceArguments parse(String... args) {
        return SentenceArguments.parse(args, 1);
    }

    @Test
    void bailDurationAndReason() {
        SentenceArguments parsed = parse("Steve", "500", "2h", "stole", "bread");
        assertEquals(500, parsed.bail);
        assertEquals(7_200_000L, parsed.duration);
        assertEquals("stole bread", parsed.reason);
    }

    @Test
    void everyPartIsOptional() {
        SentenceArguments durationOnly = parse("Steve", "1h30m");
        assertNull(durationOnly.bail);
        assertEquals(5_400_000L, durationOnly.duration);
        assertNull(durationOnly.reason);

        SentenceArguments reasonOnly = parse("Steve", "griefing", "the", "base");
        assertNull(reasonOnly.bail);
        assertEquals(-1, reasonOnly.duration);
        assertEquals("griefing the base", reasonOnly.reason);

        SentenceArguments nothing = parse("Steve");
        assertNull(nothing.bail);
        assertEquals(-1, nothing.duration);
        assertNull(nothing.reason);
    }

    @Test
    void keywords() {
        SentenceArguments parsed = parse("Steve", "-", "permanent", "x");
        assertTrue(parsed.noBail);
        assertEquals(0, parsed.duration);
        assertEquals("x", parsed.reason);
    }

    @Test
    void amountsAcceptCurrencyAndDecimalComma() {
        assertEquals(1.5, parse("Steve", "$1,5").bail);
    }

    @Test
    void reasonsLoseFormatting() {
        assertEquals("hello world$px", JailingChecks.sanitize("&chello §lworld{$p}%x%"));
    }
}
