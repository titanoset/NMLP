package com.nmlp.command;

import com.nmlp.service.AffectionKind;
import com.nmlp.service.AffectionService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AffectionCommand implements CommandExecutor, TabCompleter {

    private final AffectionService affection;
    private final AffectionKind kind;

    public AffectionCommand(@NotNull AffectionService affection, @NotNull AffectionKind kind) {
        this.affection = affection;
        this.kind = kind;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            return true;
        }
        affection.tryEmote(p, kind, args, command.getName());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1 || !(sender instanceof Player)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        String prefix = args[0].toLowerCase();
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (pl.getName().toLowerCase().startsWith(prefix)) {
                out.add(pl.getName());
            }
        }
        return out;
    }
}
