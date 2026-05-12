package com.nmlp.service;

import com.nmlp.api.event.DivorceEvent;
import com.nmlp.api.event.MarriageEvent;
import com.nmlp.config.MainConfig;
import com.nmlp.domain.HistoryEventType;
import com.nmlp.domain.RelationshipStatus;
import com.nmlp.model.RelationshipRow;
import com.nmlp.repository.HistoryRepository;
import com.nmlp.repository.RelationshipRepository;
import com.nmlp.util.UuidPair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proposals, engagement, marriage, divorce.
 */
public final class RelationshipService {

    private final EffectService effects;
    private final RelationshipRepository rel;
    private final HistoryRepository history;
    private final ProfileCache cache;
    private final MainConfig config;
    private final JavaPlugin plugin;
    /** Target player -> proposer for pending engagement proposal */
    private final Map<UUID, UUID> pendingIncoming = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastProposalAt = new ConcurrentHashMap<>();

    public RelationshipService(
            @NotNull JavaPlugin plugin,
            @NotNull EffectService effects,
            @NotNull RelationshipRepository rel,
            @NotNull HistoryRepository history,
            @NotNull ProfileCache cache,
            @NotNull MainConfig config
    ) {
        this.plugin = plugin;
        this.effects = effects;
        this.rel = rel;
        this.history = history;
        this.cache = cache;
        this.config = config;
    }

    public boolean isPendingFor(@NotNull UUID target) {
        return pendingIncoming.containsKey(target);
    }

    public @NotNull Optional<UUID> pendingProposer(@NotNull UUID target) {
        return Optional.ofNullable(pendingIncoming.get(target));
    }

    public @NotNull CompletableFuture<Result> propose(@NotNull Player from, @NotNull Player to) {
        if (from.getUniqueId().equals(to.getUniqueId())) {
            return CompletableFuture.completedFuture(Result.CANNOT_SELF);
        }
        long now = System.currentTimeMillis();
        Long last = lastProposalAt.get(from.getUniqueId());
        if (last != null && now - last < config.proposeCooldownMs()) {
            return CompletableFuture.completedFuture(Result.COOLDOWN);
        }
        return rel.hasActiveMarriage(from.getUniqueId()).thenCompose(fm -> {
            if (Boolean.TRUE.equals(fm)) {
                return CompletableFuture.completedFuture(Result.ALREADY_MARRIED);
            }
            return rel.hasActiveMarriage(to.getUniqueId()).thenCompose(tm -> {
                if (Boolean.TRUE.equals(tm)) {
                    return CompletableFuture.completedFuture(Result.TARGET_MARRIED);
                }
                return rel.findActiveFor(from.getUniqueId()).thenCompose(fo -> {
                    if (fo.isPresent()) {
                        return CompletableFuture.completedFuture(Result.HAS_ACTIVE);
                    }
                    return rel.findActiveFor(to.getUniqueId()).thenCompose(toActive -> {
                        if (toActive.isPresent()) {
                            return CompletableFuture.completedFuture(Result.TARGET_HAS_ACTIVE);
                        }
                        if (pendingIncoming.containsKey(to.getUniqueId())) {
                            return CompletableFuture.completedFuture(Result.PENDING);
                        }
                        lastProposalAt.put(from.getUniqueId(), now);
                        pendingIncoming.put(to.getUniqueId(), from.getUniqueId());
                        return history.append(HistoryEventType.PROPOSAL_SENT, from.getUniqueId(), to.getUniqueId(), null, null, now)
                                .thenApply(v -> Result.OK);
                    });
                });
            });
        });
    }

    public @NotNull CompletableFuture<Result> accept(@NotNull Player accepter, @NotNull UUID proposerId) {
        UUID target = accepter.getUniqueId();
        UUID expected = pendingIncoming.get(target);
        if (expected == null || !expected.equals(proposerId)) {
            return CompletableFuture.completedFuture(Result.NO_PENDING);
        }
        long now = System.currentTimeMillis();
        UuidPair pair = UuidPair.of(target, proposerId);
        pendingIncoming.remove(target);
        return rel.insertActive(pair, RelationshipStatus.ENGAGED, now).thenCompose(rid ->
                rel.insertEngagement(rid, now, now).thenCompose(v ->
                        history.append(HistoryEventType.PROPOSAL_ACCEPTED, target, proposerId, null, null, now)
                                .thenCompose(x -> history.append(HistoryEventType.ENGAGED, target, proposerId, null, null, now))
                ).thenRun(() -> {
                    cache.invalidate(target);
                    cache.invalidate(proposerId);
                }).thenApply(v -> Result.OK)
        );
    }

    public record MarryOutcome(@NotNull Result result, @Nullable UUID partner) {
    }

    public @NotNull CompletableFuture<MarryOutcome> marry(@NotNull Player player) {
        long now = System.currentTimeMillis();
        return rel.findActiveFor(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) {
                return CompletableFuture.completedFuture(new MarryOutcome(Result.NO_PARTNER, null));
            }
            RelationshipRow row = opt.get();
            if (row.status() != RelationshipStatus.ENGAGED) {
                return CompletableFuture.completedFuture(new MarryOutcome(Result.MUST_ENGAGE, null));
            }
            long id = row.id();
            UUID partner = partnerOf(row, player.getUniqueId());
            return rel.updateStatus(id, RelationshipStatus.MARRIED)
                    .thenCompose(v -> rel.insertMarriage(id, now))
                    .thenCompose(v -> history.append(HistoryEventType.MARRIED, player.getUniqueId(), partner, null, null, now))
                    .thenRun(() -> {
                        cache.invalidate(player.getUniqueId());
                        cache.invalidate(partner);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player other = Bukkit.getPlayer(partner);
                            Bukkit.getPluginManager().callEvent(new MarriageEvent(player.getUniqueId(), partner, player, other));
                            effects.playMarriageCelebration(player, other);
                        });
                    }).thenApply(v -> new MarryOutcome(Result.OK, partner));
        });
    }

    public @NotNull CompletableFuture<Result> divorce(@NotNull Player player) {
        long now = System.currentTimeMillis();
        return rel.findActiveFor(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) {
                return CompletableFuture.completedFuture(Result.NO_PARTNER);
            }
            RelationshipRow row = opt.get();
            if (row.status() != RelationshipStatus.MARRIED && row.status() != RelationshipStatus.ENGAGED) {
                return CompletableFuture.completedFuture(Result.NO_PARTNER);
            }
            long id = row.id();
            UUID partner = partnerOf(row, player.getUniqueId());
            return rel.finalizeDivorce(id, now)
                    .thenCompose(v -> history.append(HistoryEventType.DIVORCED, player.getUniqueId(), partner, null, null, now))
                    .thenRun(() -> {
                        cache.invalidate(player.getUniqueId());
                        cache.invalidate(partner);
                        Bukkit.getScheduler().runTask(plugin, () ->
                                Bukkit.getPluginManager().callEvent(new DivorceEvent(player.getUniqueId(), partner, player))
                        );
                    }).thenApply(v -> Result.OK);
        });
    }

    private static @NotNull UUID partnerOf(@NotNull RelationshipRow row, @NotNull UUID self) {
        return row.playerLow().equals(self) ? row.playerHigh() : row.playerLow();
    }

    public enum Result {
        OK,
        COOLDOWN,
        CANNOT_SELF,
        ALREADY_MARRIED,
        TARGET_MARRIED,
        HAS_ACTIVE,
        TARGET_HAS_ACTIVE,
        PENDING,
        NO_PENDING,
        NO_PARTNER,
        MUST_ENGAGE
    }
}
