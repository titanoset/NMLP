package com.nmlp.service;

import com.nmlp.config.MainConfig;
import com.nmlp.config.MessageService;
import com.nmlp.config.ReloadManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AffectionServiceTest {

    @Mock
    private ReloadManager reload;
    @Mock
    private MessageService messages;
    @Mock
    private ProfileCache cache;
    @Mock
    private MainConfig mainConfig;
    @Mock
    private Player actor;

    @Test
    void disabledSendsMessage() {
        when(reload.main()).thenReturn(mainConfig);
        when(mainConfig.affectionEnabled()).thenReturn(false);

        AffectionService svc = new AffectionService(reload, messages, cache);
        svc.tryEmote(actor, AffectionKind.HUG, new String[]{}, "hug");

        verify(messages).send(eq(actor), eq("affection.disabled"), anyString());
    }
}
