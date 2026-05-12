package com.nmlp.repository;

import com.nmlp.domain.RelationshipStatus;
import com.nmlp.model.RelationshipRow;
import com.nmlp.util.UuidPair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Relationship persistence.
 */
public interface RelationshipRepository {

    @NotNull CompletableFuture<Optional<RelationshipRow>> findActiveFor(@NotNull UUID player);

    @NotNull CompletableFuture<Boolean> hasActiveMarriage(@NotNull UUID player);

    @NotNull CompletableFuture<Long> insertActive(
            @NotNull UuidPair pair,
            @NotNull RelationshipStatus status,
            long startedAt
    );

    @NotNull CompletableFuture<Void> updateStatus(long relationshipId, @NotNull RelationshipStatus status);

    @NotNull CompletableFuture<Void> endRelationship(long relationshipId, long endedAt);

    @NotNull CompletableFuture<Void> insertEngagement(long relationshipId, long proposedAt, long acceptedAt);

    @NotNull CompletableFuture<Void> insertMarriage(long relationshipId, long marriedAt);

    @NotNull CompletableFuture<Void> divorceMarriage(long relationshipId, long divorcedAt);

    /**
     * Ends relationship row and closes open engagement/marriage records.
     */
    @NotNull CompletableFuture<Void> finalizeDivorce(long relationshipId, long endedAt);

    @NotNull CompletableFuture<List<RelationshipRow>> historyFor(@NotNull UUID player, int limit);

    @NotNull CompletableFuture<List<UUID>> exPartners(@NotNull UUID player, int limit);
}
