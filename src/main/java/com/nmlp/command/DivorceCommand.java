package com.nmlp.command;

import com.nmlp.config.MessageService;
import com.nmlp.service.RelationshipService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class DivorceCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RelationshipService relationships;
    private final MessageService messages;

    public DivorceCommand(@NotNull JavaPlugin plugin, @NotNull RelationshipService relationships, @NotNull MessageService messages) {
        this.plugin = plugin;
        this.relationships = relationships;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            messages.send(sender, "errors.player-only", "<red>Players only</red>");
            return true;
        }
        relationships.divorce(p).whenComplete((res, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (ex != null) {
                messages.send(p, "errors.db-error", "<red>DB</red>");
                return;
            }
            if (res == RelationshipService.Result.OK) {
                messages.send(p, "divorce.success", "<gray>Divorced</gray>");
            } else {
                messages.send(p, "errors.no-active-partner", "<red>No partner</red>");
            }
        }));
        return true;
    }
}
