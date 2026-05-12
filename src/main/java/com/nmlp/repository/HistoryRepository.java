package com.nmlp.repository;

import com.nmlp.domain.HistoryEventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Append-only history log.
 */
public interface HistoryRepository {

    @NotNull CompletableFuture<Void> append(
            @NotNull HistoryEventType type,
            @Nullable UUID actor,
            @Nullable UUID target,
            @Nullable UUID related,
            @Nullable String payload,
            long createdAt
    );
}
