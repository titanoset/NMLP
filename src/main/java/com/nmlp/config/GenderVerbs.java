package com.nmlp.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Past-tense verb snippets from {@code messages.yml} {@code verbs.<id>.(male|female|unknown)} for Russian agreement.
 */
public final class GenderVerbs {

    private GenderVerbs() {
    }

    /**
     * @param verbId e.g. {@code propose}, {@code hug}, {@code kiss}
     */
    public static @NotNull String past(@NotNull MessagesConfig cfg, @NotNull String verbId, @Nullable String genderCode) {
        String branch = branch(genderCode);
        String path = "verbs." + verbId + "." + branch;
        String def = cfg.raw("verbs." + verbId + ".unknown", "");
        if (def.isEmpty()) {
            def = "?";
        }
        return cfg.raw(path, def);
    }

    private static @NotNull String branch(@Nullable String genderCode) {
        if (genderCode == null) {
            return "unknown";
        }
        return switch (genderCode.toLowerCase(Locale.ROOT)) {
            case "male" -> "male";
            case "female" -> "female";
            default -> "unknown";
        };
    }
}
