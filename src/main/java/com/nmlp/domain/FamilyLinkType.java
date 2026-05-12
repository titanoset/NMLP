package com.nmlp.domain;

import org.jetbrains.annotations.NotNull;

/**
 * Typed edges in the family graph.
 */
public enum FamilyLinkType {
    PARENT,
    CHILD,
    ADOPTIVE_PARENT,
    ADOPTIVE_CHILD,
    SIBLING,
    STEP_PARENT,
    EX_FAMILY;

    public @NotNull String toDb() {
        return name();
    }

    public static @NotNull FamilyLinkType fromDb(@NotNull String s) {
        return FamilyLinkType.valueOf(s);
    }
}
