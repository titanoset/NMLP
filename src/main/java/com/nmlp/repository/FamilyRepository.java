package com.nmlp.repository;

import com.nmlp.domain.FamilyLinkType;
import com.nmlp.model.FamilyLinkRow;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Family graph persistence.
 */
public interface FamilyRepository {

    @NotNull CompletableFuture<Void> addLink(
            @NotNull UUID from,
            @NotNull UUID to,
            @NotNull FamilyLinkType type,
            long createdAt
    );

    @NotNull CompletableFuture<List<FamilyLinkRow>> activeLinksFor(@NotNull UUID player);

    @NotNull CompletableFuture<Void> addChildParent(@NotNull UUID child, @NotNull UUID parent, boolean biological, long createdAt);

    @NotNull CompletableFuture<List<UUID>> parentsOf(@NotNull UUID child);

    @NotNull CompletableFuture<List<UUID>> childrenOf(@NotNull UUID parent);
}
