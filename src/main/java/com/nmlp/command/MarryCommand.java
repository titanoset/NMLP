package com.nmlp.command;

import com.nmlp.config.MessageService;
import com.nmlp.service.DocumentItemService;
import com.nmlp.service.RelationshipService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class MarryCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final RelationshipService relationships;
    private final MessageService messages;
    private final DocumentItemService items;

    public MarryCommand(
            @NotNull JavaPlugin plugin,
            @NotNull RelationshipService relationships,
            @NotNull MessageService messages,
            @NotNull DocumentItemService items
    ) {
        this.plugin = plugin;
        this.relationships = relationships;
        this.messages = messages;
        this.items = items;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            messages.send(sender, "errors.player-only", "<red>Players only</red>");
            return true;
        }
        relationships.marry(p).whenComplete((out, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (ex != null) {
                messages.send(p, "errors.db-error", "<red>DB</red>");
                return;
            }
            switch (out.result()) {
                case OK -> {
                    messages.send(p, "marry.success", "<purple>Married</purple>");
                    ItemStack cert = items.createDocument("marriage_certificate", UUID.randomUUID());
                    p.getInventory().addItem(cert);
                    if (out.partner() != null) {
                        Player other = Bukkit.getPlayer(out.partner());
                        if (other != null && other.isOnline()) {
                            other.getInventory().addItem(items.createDocument("marriage_certificate", UUID.randomUUID()));
                            messages.send(other, "marry.success", "<purple>Married</purple>");
                        }
                    }
                }
                case NO_PARTNER -> messages.send(p, "errors.no-active-partner", "<red>No partner</red>");
                case MUST_ENGAGE -> messages.send(p, "errors.must-engage-first", "<red>Engage first</red>");
                default -> messages.send(p, "errors.db-error", "<red>Failed</red>");
            }
        }));
        return true;
    }
}
