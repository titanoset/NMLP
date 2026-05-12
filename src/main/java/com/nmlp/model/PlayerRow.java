package com.nmlp.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Persisted player row with gender and pronoun codes.
 */
public record PlayerRow(
        @NotNull UUID uuid,
        @NotNull String usernameLast,
        @Nullable String genderCode,
        @Nullable String pronounsCode,
        boolean setupWizardComplete,
        long firstSeen,
        long updatedAt
) {
}
