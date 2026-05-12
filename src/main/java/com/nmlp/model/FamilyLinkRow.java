package com.nmlp.model;

import com.nmlp.domain.FamilyLinkType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record FamilyLinkRow(
        long id,
        @NotNull UUID fromUuid,
        @NotNull UUID toUuid,
        @NotNull FamilyLinkType linkType,
        long createdAt,
        Long endedAt
) {
}
