package com.nmlp.service;

import com.nmlp.domain.HistoryEventType;
import com.nmlp.repository.FamilyRepository;
import com.nmlp.repository.HistoryRepository;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

    @Mock
    private FamilyRepository family;
    @Mock
    private HistoryRepository history;
    @Mock
    private ProfileCache cache;
    @Mock
    private Player parent;

    @Test
    void adoptChainsRepositoriesAndInvalidatesCache() {
        UUID parentId = UUID.fromString("40000000-0000-0000-0000-000000000004");
        UUID childId = UUID.fromString("50000000-0000-0000-0000-000000000005");
        when(parent.getUniqueId()).thenReturn(parentId);
        when(family.addChildParent(eq(childId), eq(parentId), eq(false), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(family.addLink(eq(parentId), eq(childId), any(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(family.addLink(eq(childId), eq(parentId), any(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(history.append(eq(HistoryEventType.ADOPTION), eq(parentId), eq(childId), any(), any(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(null));

        FamilyService svc = new FamilyService(family, history, cache);
        svc.adopt(parent, childId).join();

        verify(cache).invalidate(parentId);
        verify(cache).invalidate(childId);
    }
}
