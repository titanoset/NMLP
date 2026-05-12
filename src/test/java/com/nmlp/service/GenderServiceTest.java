package com.nmlp.service;

import com.nmlp.domain.GenderType;
import com.nmlp.domain.HistoryEventType;
import com.nmlp.model.PlayerRow;
import com.nmlp.repository.HistoryRepository;
import com.nmlp.repository.PlayerRepository;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GenderServiceTest {

    @Mock
    private PlayerRepository players;
    @Mock
    private HistoryRepository history;
    @Mock
    private ProfileCache cache;
    @Mock
    private Player player;

    private GenderService service;
    private final UUID uuid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @BeforeEach
    void setUp() {
        service = new GenderService(players, history, cache);
        when(player.getUniqueId()).thenReturn(uuid);
        when(players.setGender(any(), anyInt(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));
        when(players.setPronouns(any(), anyInt(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));
        when(history.append(any(), any(), any(), any(), any(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void setGenderDoesNotCompleteWizardWhenAlreadyComplete() {
        when(players.find(eq(uuid))).thenReturn(CompletableFuture.completedFuture(Optional.of(
                new PlayerRow(uuid, "user", "male", "he_him", true, 0L, 0L)
        )));
        when(players.setSetupComplete(any(), anyBoolean(), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

        Boolean wizardDone = service.setGender(player, GenderType.FEMALE).join();
        assertFalse(wizardDone);
        verify(cache).invalidate(uuid);
    }

    @Test
    void setGenderCompletesWizardWhenGenderSetButWizardOpen() {
        when(players.find(eq(uuid))).thenReturn(CompletableFuture.completedFuture(Optional.of(
                new PlayerRow(uuid, "user", "male", "he_him", false, 0L, 0L)
        )));
        when(players.setSetupComplete(eq(uuid), eq(true), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

        Boolean wizardDone = service.setGender(player, GenderType.FEMALE).join();
        assertTrue(wizardDone);
        verify(players).setSetupComplete(eq(uuid), eq(true), anyLong());
        verify(history).append(eq(HistoryEventType.GENDER_SET), eq(uuid), any(), any(), any(), anyLong());
        verify(history).append(eq(HistoryEventType.PRONOUNS_SET), eq(uuid), any(), any(), eq("she_her"), anyLong());
    }

    @Test
    void completeWizardInvalidatesCache() {
        when(players.setSetupComplete(eq(uuid), eq(true), anyLong())).thenReturn(CompletableFuture.completedFuture(null));

        service.completeWizard(player).join();
        verify(cache).invalidate(uuid);
    }
}
