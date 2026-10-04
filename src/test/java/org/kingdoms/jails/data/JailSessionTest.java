package org.kingdoms.jails.data;

import org.junit.jupiter.api.Test;
import org.kingdoms.jails.util.Codec;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JailSessionTest {
    private final UUID prisoner = UUID.randomUUID();
    private final UUID kingdom = UUID.randomUUID();

    @Test
    void survivesTheStorageRoundTrip() {
        JailSession session = new JailSession(prisoner, kingdom, null, JailType.INVASION, 1000L, 60_000L, 12.5,
                "a&b=c\nd%e", "world;1.000;2.000;3.000;0.000;0.000");
        session.setPendingTeleport(true);

        List<Map<String, String>> records = Codec.decodeAll(Codec.encodeAll(Arrays.asList(session.serialize(), session.serialize())));
        assertEquals(2, records.size());

        JailSession read = JailSession.deserialize(records.get(0), kingdom);
        assertEquals(prisoner, read.getPrisoner());
        assertNull(read.getIssuer());
        assertEquals(JailType.INVASION, read.getType());
        assertEquals(12.5, read.getBail());
        assertEquals(60_000L, read.getDuration());
        assertEquals("a&b=c\nd%e", read.getReason());
        assertTrue(read.isPendingTeleport());
        assertEquals("world;1.000;2.000;3.000;0.000;0.000", read.getPreviousLocationRaw());
    }

    @Test
    void corruptedRecordsAreSkipped() {
        assertNull(JailSession.deserialize(Codec.decode("prisoner=garbage"), kingdom));
    }

    @Test
    void onlineClockOnlyCountsOnlineTime() {
        JailSession session = new JailSession(prisoner, kingdom, null, JailType.MANUAL, System.currentTimeMillis(), 10_000L, 0, null, null);
        session.tickOnline(1000);
        session.tickOnline(4000);
        assertEquals(3000, session.getOnlineServed());
        assertEquals(7000, session.getRemaining(false));

        session.pauseClock(); // logged out
        session.tickOnline(100_000);
        session.tickOnline(101_000);
        assertEquals(4000, session.getOnlineServed());
    }

    @Test
    void sentenceWithoutLimitIsNeverServed() {
        JailSession session = new JailSession(prisoner, kingdom, null, JailType.MANUAL, 0, 0, 0, null, null);
        assertTrue(session.isPermanent());
        assertEquals(-1, session.getRemaining(true));
        assertFalse(session.isServed(true));
    }

    @Test
    void offlineTimeCountsFromTheJailing() {
        JailSession session = new JailSession(prisoner, kingdom, null, JailType.MANUAL,
                System.currentTimeMillis() - 20_000L, 10_000L, 0, null, null);
        assertTrue(session.isServed(true));
        assertFalse(session.isServed(false));
    }
}
