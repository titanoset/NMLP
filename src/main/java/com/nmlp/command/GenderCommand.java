package com.nmlp.command;

import com.nmlp.config.MessageService;
import com.nmlp.domain.GenderType;
import com.nmlp.model.PlayerRow;
import com.nmlp.service.GenderService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class GenderCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final GenderService gender;
    private final MessageService messages;

    public GenderCommand(@NotNull JavaPlugin plugin, @NotNull GenderService gender, @NotNull MessageService messages) {
        this.plugin = plugin;
        this.gender = gender;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            messages.send(sender, "errors.player-only", "<red>Players only</red>");
            return true;
        }
        if (args.length == 0) {
            gender.playerRow(p.getUniqueId()).whenComplete((opt, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (!p.isOnline()) {
                    return;
                }
                if (ex != null) {
                    messages.send(p, "errors.db-error", "<red>DB</red>");
                    return;
                }
                PlayerRow row = opt.orElse(null);
                if (row == null || !row.setupWizardComplete()) {
                    messages.showGenderWizardPrompt(p);
                    return;
                }
                String value = row.genderCode() == null ? "—" : row.genderCode();
                messages.send(p, "gender.current", "<gray>Gender</gray>", Map.of("value", value));
            }));
            return true;
        }
        GenderType.fromInput(args[0]).ifPresentOrElse(
                g -> gender.setGender(p, g).whenComplete((wizardDone, ex) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (ex != null) {
                        messages.send(p, "errors.db-error", "<red>DB</red>");
                        return;
                    }
                    messages.send(p, "gender.set", "<green>Set</green>", java.util.Map.of("value", g.name()));
                    if (Boolean.TRUE.equals(wizardDone)) {
                        messages.send(p, "wizard.done", "<green>Done</green>");
                    }
                })),
                () -> messages.send(p, "errors.invalid-gender", "<red>Invalid</red>")
        );
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return java.util.stream.Stream.of("male", "female")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
