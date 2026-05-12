package com.nmlp.repository;

import com.nmlp.model.PlayerRow;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Player persistence.
 */
public interface PlayerRepository {

    @NotNull CompletableFuture<Void> upsert(@NotNull UUID uuid, @NotNull String usernameLast, long now);

    @NotNull CompletableFuture<Optional<PlayerRow>> find(@NotNull UUID uuid);

    @NotNull CompletableFuture<Void> setGender(@NotNull UUID uuid, int genderId, long now);

    @NotNull CompletableFuture<Void> setPronouns(@NotNull UUID uuid, int pronounsId, long now);

    @NotNull CompletableFuture<Void> setSetupComplete(@NotNull UUID uuid, boolean done, long now);

    @NotNull CompletableFuture<java.util.List<String>> searchUsernames(@NotNull String prefix, int limit);
}
