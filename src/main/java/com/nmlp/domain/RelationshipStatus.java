package com.nmlp.domain;

import org.jetbrains.annotations.NotNull;

/**
 * Relationship status for a pair of players.
 */
public enum RelationshipStatus {
    SINGLE,
    ENGAGED,
    MARRIED,
    DIVORCED,
    BOYFRIEND,
    GIRLFRIEND,
    EX;

    public static @NotNull RelationshipStatus fromDb(@NotNull String s) {
        return RelationshipStatus.valueOf(s);
    }

    public @NotNull String toDb() {
        return name();
    }
}
