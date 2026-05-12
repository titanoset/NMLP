package com.nmlp.service;

import com.nmlp.domain.FamilyLinkType;
import com.nmlp.domain.HistoryEventType;
import com.nmlp.repository.FamilyRepository;
import com.nmlp.repository.HistoryRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Family links and adoption.
 */
public final class FamilyService {

    private final FamilyRepository family;
    private final HistoryRepository history;
    private final ProfileCache cache;

    public FamilyService(
            @NotNull FamilyRepository family,
            @NotNull HistoryRepository history,
            @NotNull ProfileCache cache
    ) {
        this.family = family;
        this.history = history;
        this.cache = cache;
    }

    public @NotNull CompletableFuture<Void> adopt(@NotNull Player parent, @NotNull UUID childUuid) {
        long now = System.currentTimeMillis();
        UUID p = parent.getUniqueId();
        return family.addChildParent(childUuid, p, false, now)
                .thenCompose(v -> family.addLink(p, childUuid, FamilyLinkType.ADOPTIVE_PARENT, now))
                .thenCompose(v -> family.addLink(childUuid, p, FamilyLinkType.ADOPTIVE_CHILD, now))
                .thenCompose(v -> history.append(HistoryEventType.ADOPTION, p, childUuid, null, null, now))
                .thenRun(() -> {
                    cache.invalidate(p);
                    cache.invalidate(childUuid);
                });
    }

    public @NotNull CompletableFuture<Void> registerBirthChild(@NotNull UUID parentA, @NotNull UUID parentB, @NotNull UUID child) {
        long now = System.currentTimeMillis();
        return family.addChildParent(child, parentA, true, now)
                .thenCompose(v -> family.addChildParent(child, parentB, true, now))
                .thenCompose(v -> history.append(HistoryEventType.FAMILY_LINK, parentA, child, parentB, "birth", now))
                .thenRun(() -> {
                    cache.invalidate(parentA);
                    cache.invalidate(parentB);
                    cache.invalidate(child);
                });
    }
}
