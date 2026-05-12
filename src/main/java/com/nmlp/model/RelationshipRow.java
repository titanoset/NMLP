package com.nmlp.model;

import com.nmlp.domain.RelationshipStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Active or historical relationship between two players.
 */
public record RelationshipRow(
        long id,
        @NotNull UUID playerLow,
        @NotNull UUID playerHigh,
        @NotNull RelationshipStatus status,
        long startedAt,
        Long endedAt
) {
}
