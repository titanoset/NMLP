package com.nmlp.command;

import com.nmlp.NMLPPlugin;
import com.nmlp.config.MessageService;
import com.nmlp.config.ReloadManager;
import com.nmlp.service.DocumentItemService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /nmlp reload|debug|give ring
 */
public final class NmlpAdminCommand implements CommandExecutor {

    private final NMLPPlugin plugin;
    private final ReloadManager reload;
    private final MessageService messages;
    private final DocumentItemService items;

    public NmlpAdminCommand(
            @NotNull NMLPPlugin plugin,
            @NotNull ReloadManager reload,
            @NotNull MessageService messages,
            @NotNull DocumentItemService items
    ) {
        this.plugin = plugin;
        this.reload = reload;
        this.messages = messages;
        this.items = items;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            messages.send(sender, "admin.usage", "<gray>/nmlp …</gray>");
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("nmlp.reload")) {
                messages.send(sender, "errors.no-permission", "<red>No permission</red>");
                return true;
            }
            reload.loadAll();
            plugin.syncDebugFromConfig();
            messages.send(sender, "reload.done", "<green>Reloaded</green>");
            return true;
        }
        if ("debug".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("nmlp.debug")) {
                messages.send(sender, "errors.no-permission", "<red>No permission</red>");
                return true;
            }
            boolean v = args.length < 2 || Boolean.parseBoolean(args[1]);
            plugin.setDebug(v);
            messages.send(sender, "debug.enabled", "<yellow>Debug</yellow>", java.util.Map.of("value", String.valueOf(v)));
            return true;
        }
        if ("give".equalsIgnoreCase(args[0]) && args.length >= 2 && "ring".equalsIgnoreCase(args[1])) {
            if (!sender.hasPermission("nmlp.admin")) {
                messages.send(sender, "errors.no-permission", "<red>No permission</red>");
                return true;
            }
            if (!(sender instanceof Player p)) {
                messages.send(sender, "errors.player-only", "<red>Players only</red>");
                return true;
            }
            p.getInventory().addItem(items.createRing());
            return true;
        }
        return false;
    }
}
