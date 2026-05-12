package com.nmlp.domain;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

/**
 * Player gender selection (pronouns follow: male → he/him, female → she/her).
 */
public enum GenderType {
    MALE("male"),
    FEMALE("female");

    private final String code;

    GenderType(String code) {
        this.code = code;
    }

    public @NotNull String code() {
        return code;
    }

    public static @NotNull Optional<GenderType> fromInput(@NotNull String raw) {
        String n = raw.trim().toLowerCase(Locale.ROOT);
        return switch (n) {
            case "male", "m" -> Optional.of(MALE);
            case "female", "f" -> Optional.of(FEMALE);
            default -> Optional.empty();
        };
    }
}
