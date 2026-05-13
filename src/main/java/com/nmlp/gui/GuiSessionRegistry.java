package com.nmlp.gui;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks which NMLP GUI a player has open (inventory holder may be null).
 */
public final class GuiSessionRegistry {

    public enum Kind {
        HUB,
        PROFILE,
        FAMILY,
        TREE,
        HISTORY,
        SETTINGS
    }

    private final ConcurrentMap<UUID, Kind> open = new ConcurrentHashMap<>();

    public void open(@NotNull UUID player, @NotNull Kind kind) {
        open.put(player, kind);
    }

    public void close(@NotNull UUID player) {
        open.remove(player);
    }

    public boolean isNmlp(@NotNull UUID player) {
        return open.containsKey(player);
    }

    public @NotNull Kind kindOrDefault(@NotNull UUID player, @NotNull Kind def) {
        return open.getOrDefault(player, def);
    }
}
