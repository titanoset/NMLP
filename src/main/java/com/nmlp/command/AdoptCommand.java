package com.nmlp.command;

import com.nmlp.config.MessageService;
import com.nmlp.service.FamilyService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AdoptCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final FamilyService family;
    private final MessageService messages;

    public AdoptCommand(@NotNull JavaPlugin plugin, @NotNull FamilyService family, @NotNull MessageService messages) {
        this.plugin = plugin;
        this.family = family;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            messages.send(sender, "errors.player-only", "<red>Players only</red>");
            return true;
        }
        if (args.length < 1) {
            messages.send(p, "adoption.usage", "<gray>/adopt <ник></gray>");
            return true;
        }
        Player child = Bukkit.getPlayerExact(args[0]);
        if (child == null) {
            messages.send(p, "errors.not-found", "<red>Child must be online</red>");
            return true;
        }
        family.adopt(p, child.getUniqueId()).whenComplete((v, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (ex != null) {
                messages.send(p, "errors.db-error", "<red>DB</red>");
            } else {
            messages.send(p, "adoption.success", "<green>OK</green>");
            }
        }));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    out.add(pl.getName());
                }
            }
            return out;
        }
        return List.of();
    }
}
