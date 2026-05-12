package com.nmlp.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Snapshot used for PlaceholderAPI and TAB (read from memory, no main-thread DB).
 */
public record ProfileSnapshot(
        @NotNull UUID uuid,
        @NotNull String username,
        @Nullable String genderCode,
        @Nullable String pronounsCode,
        @Nullable UUID partnerUuid,
        @Nullable String partnerName,
        @NotNull String relationshipStatus,
        long relationSinceEpochMs,
        @NotNull List<UUID> childrenIds,
        @NotNull List<String> exNames
) {
    public static @NotNull ProfileSnapshot empty(@NotNull UUID uuid, @NotNull String username) {
        return new ProfileSnapshot(uuid, username, null, null, null, null, "SINGLE", 0L, List.of(), List.of());
    }
}
