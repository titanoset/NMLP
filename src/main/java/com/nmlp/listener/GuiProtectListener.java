package com.nmlp.listener;

import com.nmlp.gui.GuiManager;
import com.nmlp.gui.GuiSessionRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Blocks item moves in NMLP GUIs and routes simple navigation clicks.
 */
public final class GuiProtectListener implements Listener {

    private final GuiSessionRegistry sessions;
    private final GuiManager gui;

    public GuiProtectListener(@NotNull GuiSessionRegistry sessions, @NotNull GuiManager gui) {
        this.sessions = sessions;
        this.gui = gui;
    }

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        if (!sessions.isNmlp(p.getUniqueId())) {
            return;
        }
        if (sessions.kindOrDefault(p.getUniqueId(), GuiSessionRegistry.Kind.PROFILE) == GuiSessionRegistry.Kind.PROFILE
                && e.getRawSlot() == 31) {
            e.setCancelled(true);
            gui.openHistory(p);
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(@NotNull InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        if (sessions.isNmlp(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            sessions.close(p.getUniqueId());
        }
    }
}
