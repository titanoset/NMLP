package com.nmlp.listener;

import com.nmlp.config.MessageService;
import com.nmlp.model.PlayerRow;
import com.nmlp.repository.PlayerRepository;
import com.nmlp.service.ProfileCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * First-join setup wizard and profile cache warm.
 */
public final class PlayerJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerRepository players;
    private final MessageService messages;
    private final ProfileCache cache;

    public PlayerJoinListener(
            @NotNull JavaPlugin plugin,
            @NotNull PlayerRepository players,
            @NotNull MessageService messages,
            @NotNull ProfileCache cache
    ) {
        this.plugin = plugin;
        this.players = players;
        this.messages = messages;
        this.cache = cache;
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID u = player.getUniqueId();
        String name = player.getName();
        long now = System.currentTimeMillis();
        players.upsert(u, name, now).thenCompose(v -> players.find(u)).whenComplete((opt, ex) -> {
            if (ex != null || opt.isEmpty()) {
                return;
            }
            PlayerRow row = opt.get();
            cache.refresh(u, name);
            if (!row.setupWizardComplete()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        messages.send(player, "wizard.welcome", "<yellow>Welcome</yellow>");
                    }
                });
            }
        });
    }
}
