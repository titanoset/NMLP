package com.nmlp.command;

import com.nmlp.config.MessageService;
import com.nmlp.service.DocumentItemService;
import com.nmlp.service.RelationshipService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EngageCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final RelationshipService relationships;
    private final MessageService messages;
    private final DocumentItemService items;

    public EngageCommand(
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
        if (args.length >= 2 && "accept".equalsIgnoreCase(args[0])) {
            String name = args[1];
            Player proposer = Bukkit.getPlayerExact(name);
            if (proposer == null) {
                messages.send(p, "errors.not-found", "<red>Not online</red>");
                return true;
            }
            UUID pid = proposer.getUniqueId();
            relationships.accept(p, pid).whenComplete((res, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (ex != null) {
                    messages.send(p, "errors.db-error", "<red>DB</red>");
                    return;
                }
                if (res == RelationshipService.Result.OK) {
                    messages.send(p, "engage.accepted", "<green>OK</green>");
                    messages.send(proposer, "engage.notify-partner", "<green>Partner accepted</green>");
                    ItemStack doc = items.createDocument("engagement_certificate", UUID.randomUUID());
                    p.getInventory().addItem(doc);
                    proposer.getInventory().addItem(items.createDocument("engagement_certificate", UUID.randomUUID()));
                } else if (res == RelationshipService.Result.NO_PENDING) {
                    messages.send(p, "errors.no-pending", "<gray>No pending</gray>");
                } else {
                    messages.send(p, "errors.db-error", "<red>Failed</red>");
                }
            }));
            return true;
        }
        messages.send(p, "engage.usage", "<gray>...</gray>");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("accept");
        }
        if (args.length == 2 && "accept".equalsIgnoreCase(args[0])) {
            List<String> out = new ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    out.add(pl.getName());
                }
            }
            return out;
        }
        return List.of();
    }
}
