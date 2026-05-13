package com.nmlp.command;

import com.nmlp.config.MessageService;
import com.nmlp.gui.GuiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the NMLP hub GUI ({@code /menu}).
 */
public final class MenuCommand implements CommandExecutor {

    private final GuiManager gui;
    private final MessageService messages;

    public MenuCommand(@NotNull GuiManager gui, @NotNull MessageService messages) {
        this.gui = gui;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            messages.send(sender, "errors.player-only", "<red>Players only</red>");
            return true;
        }
        gui.openHub(p);
        return true;
    }
}
