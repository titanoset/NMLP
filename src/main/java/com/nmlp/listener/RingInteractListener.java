package com.nmlp.listener;

import com.nmlp.config.MessageService;
import com.nmlp.config.ReloadManager;
import com.nmlp.service.DocumentItemService;
import com.nmlp.service.RelationshipService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Right-click player with ring to propose.
 */
public final class RingInteractListener implements Listener {

    private final JavaPlugin plugin;
    private final ReloadManager reload;
    private final DocumentItemService items;
    private final RelationshipService relationships;
    private final MessageService messages;

    public RingInteractListener(
            @NotNull JavaPlugin plugin,
            @NotNull ReloadManager reload,
            @NotNull DocumentItemService items,
            @NotNull RelationshipService relationships,
            @NotNull MessageService messages
    ) {
        this.plugin = plugin;
        this.reload = reload;
        this.items = items;
        this.relationships = relationships;
        this.messages = messages;
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(e.getRightClicked() instanceof Player target)) {
            return;
        }
        Player from = e.getPlayer();
        ItemStack hand = from.getInventory().getItemInMainHand();
        if (!items.isRing(hand)) {
            return;
        }
        e.setCancelled(true);
        double maxDist = reload.main().proposalRadius();
        if (from.getLocation().distanceSquared(target.getLocation()) > maxDist * maxDist) {
            return;
        }
        relationships.propose(from, target).whenComplete((res, ex) ->
                Bukkit.getScheduler().runTask(plugin, () -> handleResult(from, target, res, ex)));
    }

    private void handleResult(@NotNull Player from, @NotNull Player target, RelationshipService.Result res, Throwable ex) {
        if (ex != null) {
            messages.send(from, "errors.db-error", "<red>DB error</red>");
            return;
        }
        switch (res) {
            case OK -> {
                messages.send(from, "engage.proposed", "<gold>Proposed</gold>", Map.of(
                        "actor", from.getName(),
                        "target", target.getName()
                ));
                messages.send(target, "engage.received", "<gold>Received</gold>", Map.of(
                        "actor", from.getName()
                ));
            }
            case COOLDOWN -> messages.send(from, "errors.cooldown", "<red>Wait</red>");
            case CANNOT_SELF -> messages.send(from, "errors.cannot-self", "<red>Self</red>");
            case ALREADY_MARRIED -> messages.send(from, "errors.already-married", "<red>Married</red>");
            case TARGET_MARRIED -> messages.send(from, "errors.target-married", "<red>They married</red>");
            case HAS_ACTIVE, TARGET_HAS_ACTIVE -> messages.send(from, "errors.no-active-partner", "<red>Active rel</red>");
            case PENDING -> messages.send(from, "errors.proposal-pending", "<yellow>Pending</yellow>");
            default -> messages.send(from, "errors.db-error", "<red>Failed</red>");
        }
    }
}
