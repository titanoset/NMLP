package com.nmlp.util;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Ordered pair of UUIDs (low &lt; high string order) for stable relationship keys.
 */
public record UuidPair(@NotNull UUID low, @NotNull UUID high) {

    public static @NotNull UuidPair of(@NotNull UUID a, @NotNull UUID b) {
        String as = a.toString();
        String bs = b.toString();
        if (as.compareTo(bs) <= 0) {
            return new UuidPair(a, b);
        }
        return new UuidPair(b, a);
    }

    public boolean contains(@NotNull UUID uuid) {
        return low.equals(uuid) || high.equals(uuid);
    }

    public @NotNull UUID other(@NotNull UUID uuid) {
        if (low.equals(uuid)) {
            return high;
        }
        if (high.equals(uuid)) {
            return low;
        }
        throw new IllegalArgumentException("UUID not in pair");
    }
}
