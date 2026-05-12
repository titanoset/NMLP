package com.nmlp.service;

import com.nmlp.model.PlayerRow;
import com.nmlp.model.RelationshipRow;
import com.nmlp.repository.FamilyRepository;
import com.nmlp.repository.PlayerRepository;
import com.nmlp.repository.RelationshipRepository;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory profile cache for fast placeholder reads.
 */
public final class ProfileCache {

    private final PlayerRepository players;
    private final RelationshipRepository relationships;
    private final FamilyRepository family;
    private final Map<UUID, ProfileSnapshot> cache = new ConcurrentHashMap<>();

    public ProfileCache(
            @NotNull PlayerRepository players,
            @NotNull RelationshipRepository relationships,
            @NotNull FamilyRepository family
    ) {
        this.players = players;
        this.relationships = relationships;
        this.family = family;
    }

    public void invalidate(@NotNull UUID uuid) {
        cache.remove(uuid);
    }

    public void clear() {
        cache.clear();
    }

    public @NotNull ProfileSnapshot getCachedOrEmpty(@NotNull UUID uuid, @NotNull String fallbackName) {
        return cache.getOrDefault(uuid, ProfileSnapshot.empty(uuid, fallbackName));
    }

    public @NotNull CompletableFuture<ProfileSnapshot> warm(@NotNull UUID uuid, @NotNull String usernameLast) {
        return refresh(uuid, usernameLast);
    }

    public @NotNull CompletableFuture<ProfileSnapshot> refresh(@NotNull UUID uuid, @NotNull String usernameLast) {
        return players.find(uuid).thenCompose(opt -> {
            if (opt.isEmpty()) {
                return players.upsert(uuid, usernameLast, System.currentTimeMillis())
                        .thenCompose(v -> players.find(uuid))
                        .thenApply(o -> o.orElseThrow())
                        .thenCompose(this::buildSnapshot);
            }
            return buildSnapshot(opt.get());
        }).thenApply(s -> {
            cache.put(uuid, s);
            return s;
        });
    }

    private @NotNull CompletableFuture<ProfileSnapshot> buildSnapshot(@NotNull PlayerRow row) {
        return relationships.findActiveFor(row.uuid()).thenCompose(relOpt ->
                relationships.exPartners(row.uuid(), 16).thenCompose(exes ->
                        family.childrenOf(row.uuid()).thenApply(children -> {
                            List<String> exNames = new ArrayList<>();
                            for (UUID u : exes) {
                                exNames.add(resolveName(u));
                            }
                            RelationshipRow rel = relOpt.orElse(null);
                            if (rel == null) {
                                return new ProfileSnapshot(
                                        row.uuid(),
                                        row.usernameLast(),
                                        row.genderCode(),
                                        row.pronounsCode(),
                                        null,
                                        null,
                                        "SINGLE",
                                        0L,
                                        children,
                                        exNames
                                );
                            }
                            UUID partner = rel.playerLow().equals(row.uuid()) ? rel.playerHigh() : rel.playerLow();
                            String partnerName = resolveName(partner);
                            return new ProfileSnapshot(
                                    row.uuid(),
                                    row.usernameLast(),
                                    row.genderCode(),
                                    row.pronounsCode(),
                                    partner,
                                    partnerName,
                                    rel.status().name(),
                                    rel.startedAt(),
                                    children,
                                    exNames
                            );
                        })));
    }

    private static @NotNull String resolveName(@NotNull UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String n = op.getName();
        return n == null ? uuid.toString().substring(0, 8) : n;
    }
}
