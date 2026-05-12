package com.nmlp.command;

import com.nmlp.config.MessageService;
import com.nmlp.config.ReloadManager;
import com.nmlp.service.RiskActionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * /risk &lt;действие&gt; — ключи из buffs.yml; алиасы команд — в commands.yml сервера.
 */
public final class RiskCommand implements CommandExecutor, TabCompleter {

    private final ReloadManager reload;
    private final RiskActionService risk;
    private final MessageService messages;

    public RiskCommand(
            @NotNull ReloadManager reload,
            @NotNull RiskActionService risk,
            @NotNull MessageService messages
    ) {
        this.reload = reload;
        this.risk = risk;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            messages.send(sender, "errors.player-only", "<red>Только для игроков.</red>");
            return true;
        }
        if (args.length < 1) {
            messages.send(p, "risk.usage", "<gray>/risk <действие></gray>");
            return true;
        }
        risk.tryUse(p, args[0]);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase();
        List<String> out = new ArrayList<>();
        for (String id : reload.buffs().actions().keySet()) {
            if (id.startsWith(prefix)) {
                out.add(id);
            }
        }
        return out;
    }
}
