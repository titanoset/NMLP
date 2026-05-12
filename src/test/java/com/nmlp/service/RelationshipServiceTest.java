package com.nmlp.service;

import com.nmlp.config.MainConfig;
import com.nmlp.domain.HistoryEventType;
import com.nmlp.domain.RelationshipStatus;
import com.nmlp.model.RelationshipRow;
import com.nmlp.repository.HistoryRepository;
import com.nmlp.repository.RelationshipRepository;
import com.nmlp.util.UuidPair;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelationshipServiceTest {

    @Mock
    private JavaPlugin plugin;
    @Mock
    private EffectService effects;
    @Mock
    private RelationshipRepository rel;
    @Mock
    private HistoryRepository history;
    @Mock
    private ProfileCache cache;
    @Mock
    private MainConfig config;
    @Mock
    private Player from;
    @Mock
    private Player to;
    @Mock
    private Player other;

    private RelationshipService service;

    private final UUID fromId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final UUID toId = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private final UUID otherId = UUID.fromString("30000000-0000-0000-0000-000000000003");

    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        service = new RelationshipService(plugin, effects, rel, history, cache, config);
        when(config.proposeCooldownMs()).thenReturn(60_000L);
        when(from.getUniqueId()).thenReturn(fromId);
        when(to.getUniqueId()).thenReturn(toId);
        when(history.append(any(), any(), any(), any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @AfterEach
    void tearDownBukkit() {
        if (bukkit != null) {
            bukkit.close();
            bukkit = null;
        }
    }

    @Test
    void proposeCannotTargetSelf() {
        when(from.getUniqueId()).thenReturn(fromId);
        when(to.getUniqueId()).thenReturn(fromId);

        RelationshipService.Result r = service.propose(from, to).join();
        assertEquals(RelationshipService.Result.CANNOT_SELF, r);
    }

    @Test
    void proposeRespectsCooldown() {
        when(other.getUniqueId()).thenReturn(otherId);
        when(rel.hasActiveMarriage(any())).thenReturn(CompletableFuture.completedFuture(false));
        when(rel.findActiveFor(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertEquals(RelationshipService.Result.OK, service.propose(from, to).join());
        assertEquals(RelationshipService.Result.COOLDOWN, service.propose(from, other).join());
    }

    @Test
    void proposeOkStoresPending() {
        when(rel.hasActiveMarriage(any())).thenReturn(CompletableFuture.completedFuture(false));
        when(rel.findActiveFor(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        RelationshipService.Result r = service.propose(from, to).join();
        assertEquals(RelationshipService.Result.OK, r);
        assertTrue(service.isPendingFor(toId));
        assertEquals(Optional.of(fromId), service.pendingProposer(toId));
        verify(history).append(eq(HistoryEventType.PROPOSAL_SENT), eq(fromId), eq(toId), any(), any(), anyLong());
    }

    @Test
    void acceptCreatesEngagement() {
        when(rel.hasActiveMarriage(any())).thenReturn(CompletableFuture.completedFuture(false));
        when(rel.findActiveFor(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        service.propose(from, to).join();

        when(rel.insertActive(any(UuidPair.class), eq(RelationshipStatus.ENGAGED), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(42L));
        when(rel.insertEngagement(eq(42L), anyLong(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

        RelationshipService.Result r = service.accept(to, fromId).join();
        assertEquals(RelationshipService.Result.OK, r);
        verify(rel).insertActive(any(UuidPair.class), eq(RelationshipStatus.ENGAGED), anyLong());
        verify(cache).invalidate(fromId);
        verify(cache).invalidate(toId);
    }

    @Test
    void marryRunsCelebrationOnMainThread() {
        bukkit = mockStatic(Bukkit.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        doAnswer(invocation -> {
            invocation.getArgument(1, Runnable.class).run();
            return null;
        }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

        Player partnerPlayer = mock(Player.class);
        when(partnerPlayer.isOnline()).thenReturn(true);
        bukkit.when(() -> Bukkit.getPlayer(toId)).thenReturn(partnerPlayer);

        PluginManager pm = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pm);

        RelationshipRow engaged = new RelationshipRow(7L, fromId, toId, RelationshipStatus.ENGAGED, 100L, null);
        when(rel.findActiveFor(fromId)).thenReturn(CompletableFuture.completedFuture(Optional.of(engaged)));
        when(rel.updateStatus(7L, RelationshipStatus.MARRIED)).thenReturn(CompletableFuture.completedFuture(null));
        when(rel.insertMarriage(eq(7L), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

        RelationshipService.MarryOutcome out = service.marry(from).join();
        assertEquals(RelationshipService.Result.OK, out.result());
        assertEquals(Optional.of(toId), Optional.ofNullable(out.partner()));
        verify(effects).playMarriageCelebration(from, partnerPlayer);
        verify(pm).callEvent(any());
    }
}
