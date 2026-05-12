package com.nmlp.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired when two players become married (after DB commit).
 */
public final class MarriageEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerA;
    private final UUID playerB;
    private final @Nullable Player onlineA;
    private final @Nullable Player onlineB;

    public MarriageEvent(
            @NotNull UUID playerA,
            @NotNull UUID playerB,
            @Nullable Player onlineA,
            @Nullable Player onlineB
    ) {
        this.playerA = playerA;
        this.playerB = playerB;
        this.onlineA = onlineA;
        this.onlineB = onlineB;
    }

    public @NotNull UUID playerA() {
        return playerA;
    }

    public @NotNull UUID playerB() {
        return playerB;
    }

    public @Nullable Player onlineA() {
        return onlineA;
    }

    public @Nullable Player onlineB() {
        return onlineB;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
