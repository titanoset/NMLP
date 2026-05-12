package com.nmlp.service;

import com.nmlp.domain.GenderType;
import com.nmlp.domain.HistoryEventType;
import com.nmlp.model.PlayerRow;
import com.nmlp.repository.HistoryRepository;
import com.nmlp.repository.PlayerRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Gender setup; pronouns are stored automatically from gender (male → he_him, female → she_her).
 */
public final class GenderService {

    private final PlayerRepository players;
    private final HistoryRepository history;
    private final ProfileCache cache;

    public GenderService(
            @NotNull PlayerRepository players,
            @NotNull HistoryRepository history,
            @NotNull ProfileCache cache
    ) {
        this.players = players;
        this.history = history;
        this.cache = cache;
    }

    /**
     * Sets gender and matching pronouns (1=he_him for male, 2=she_her for female).
     *
     * @return true if the first-time setup wizard was just completed
     */
    public @NotNull CompletableFuture<Boolean> setGender(@NotNull Player player, @NotNull GenderType type) {
        UUID u = player.getUniqueId();
        long now = System.currentTimeMillis();
        int genderId = type == GenderType.MALE ? 1 : 2;
        int pronounId = type == GenderType.MALE ? 1 : 2;
        String pronounCode = type == GenderType.MALE ? "he_him" : "she_her";
        return players.setGender(u, genderId, now)
                .thenCompose(v -> players.setPronouns(u, pronounId, now))
                .thenCompose(v -> history.append(HistoryEventType.GENDER_SET, u, null, null, type.code(), now))
                .thenCompose(v -> history.append(HistoryEventType.PRONOUNS_SET, u, null, null, pronounCode, now))
                .thenCompose(v -> maybeFinishWizard(u, now))
                .thenApply(wizardDone -> {
                    cache.invalidate(u);
                    return wizardDone;
                });
    }

    private @NotNull CompletableFuture<Boolean> maybeFinishWizard(@NotNull UUID u, long now) {
        return players.find(u).thenCompose(opt -> {
            if (opt.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }
            PlayerRow r = opt.get();
            if (r.genderCode() != null && !r.setupWizardComplete()) {
                return players.setSetupComplete(u, true, now).thenApply(v -> true);
            }
            return CompletableFuture.completedFuture(false);
        });
    }

    public @NotNull CompletableFuture<Void> completeWizard(@NotNull Player player) {
        UUID u = player.getUniqueId();
        long now = System.currentTimeMillis();
        return players.setSetupComplete(u, true, now).thenRun(() -> cache.invalidate(u));
    }
}
