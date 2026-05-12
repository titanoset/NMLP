package com.nmlp.service;

import com.nmlp.repository.FamilyRepository;
import com.nmlp.repository.PlayerRepository;
import com.nmlp.repository.RelationshipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ProfileCacheTest {

    @Mock
    private PlayerRepository players;
    @Mock
    private RelationshipRepository relationships;
    @Mock
    private FamilyRepository family;

    @Test
    void getCachedOrEmptyUsesFallbackName() {
        ProfileCache cache = new ProfileCache(players, relationships, family);
        UUID u = UUID.fromString("60000000-0000-0000-0000-000000000006");
        ProfileSnapshot snap = cache.getCachedOrEmpty(u, "TestName");
        assertEquals("TestName", snap.username());
        assertEquals(u, snap.uuid());
        assertEquals("SINGLE", snap.relationshipStatus());
    }
}
