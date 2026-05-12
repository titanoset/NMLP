package com.nmlp.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired when an active engagement or marriage is dissolved.
 */
public final class DivorceEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerA;
    private final UUID playerB;
    private final @Nullable Player onlineInitiator;

    public DivorceEvent(
            @NotNull UUID playerA,
            @NotNull UUID playerB,
            @Nullable Player onlineInitiator
    ) {
        this.playerA = playerA;
        this.playerB = playerB;
        this.onlineInitiator = onlineInitiator;
    }

    public @NotNull UUID playerA() {
        return playerA;
    }

    public @NotNull UUID playerB() {
        return playerB;
    }

    public @Nullable Player onlineInitiator() {
        return onlineInitiator;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
