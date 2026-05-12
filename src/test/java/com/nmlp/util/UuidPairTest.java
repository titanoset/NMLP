package com.nmlp.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UuidPairTest {

    @Test
    void ordersLexicographically() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UuidPair p = UuidPair.of(a, b);
        assertEquals(b, p.low());
        assertEquals(a, p.high());
    }

    @Test
    void otherReturnsOpposite() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UuidPair p = UuidPair.of(a, b);
        assertEquals(b, p.other(a));
        assertEquals(a, p.other(b));
    }

    @Test
    void otherThrowsIfMissing() {
        UuidPair p = UuidPair.of(UUID.randomUUID(), UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> p.other(UUID.randomUUID()));
    }

    @Test
    void containsBothMembers() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UuidPair p = UuidPair.of(a, b);
        assertTrue(p.contains(a));
        assertTrue(p.contains(b));
        assertFalse(p.contains(UUID.randomUUID()));
    }
}
