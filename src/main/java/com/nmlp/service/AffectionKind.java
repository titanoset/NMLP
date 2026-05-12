package com.nmlp.service;

import org.jetbrains.annotations.NotNull;

/**
 * Non-explicit couple / consenting-nearby-player emotes (wording from messages.yml).
 */
public enum AffectionKind {
    HUG("hug"),
    KISS("kiss");

    private final String messageKey;

    AffectionKind(String messageKey) {
        this.messageKey = messageKey;
    }

    public @NotNull String messageKey() {
        return messageKey;
    }
}
