package com.nmlp.domain;

import org.jetbrains.annotations.NotNull;

/**
 * Append-only history events.
 */
public enum HistoryEventType {
    PROPOSAL_SENT,
    PROPOSAL_ACCEPTED,
    ENGAGED,
    MARRIED,
    DIVORCED,
    GENDER_SET,
    PRONOUNS_SET,
    ADOPTION,
    FAMILY_LINK;

    public @NotNull String toDb() {
        return name();
    }
}
