package com.nmlp.command;

import com.nmlp.config.MainConfig;
import com.nmlp.config.MessageService;
import com.nmlp.config.ReloadManager;
import com.nmlp.integration.VaultHook;
import com.nmlp.service.DocumentItemService;
import com.nmlp.service.RelationshipService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class EngageCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final ReloadManager reload;
    private final VaultHook vault;
    private final RelationshipService relationships;
    private final MessageService messages;
    private final DocumentItemService items;
    private final Map<UUID, Long> lastRingGrantMs = new ConcurrentHashMap<>();

    public EngageCommand(
            @NotNull JavaPlugin plugin,
            @NotNull ReloadManager reload,
            @NotNull VaultHook vault,
            @NotNull RelationshipService relationships,
            @NotNull MessageService messages,
            @NotNull DocumentItemService items
    ) {
        this.plugin = plugin;
        this.reload = reload;
        this.vault = vault;
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
        if (args.length >= 1 && "ring".equalsIgnoreCase(args[0])) {
            giveRing(p);
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

    private void giveRing(@NotNull Player p) {
        MainConfig mc = reload.main();
        UUID u = p.getUniqueId();
        long now = System.currentTimeMillis();
        long cd = mc.ringGrantCooldownMs();
        if (cd > 0) {
            Long prev = lastRingGrantMs.get(u);
            if (prev != null) {
                long elapsed = now - prev;
                if (elapsed < cd) {
                    long sec = Math.max(1L, (cd - elapsed + 999L) / 1000L);
                    messages.send(p, "errors.ring-grant-cooldown", "<red>Wait</red>", Map.of("seconds", String.valueOf(sec)));
                    return;
                }
            }
        }
        int diaCost = mc.ringPurchaseDiamonds();
        if (diaCost > 0 && countMaterial(p, Material.DIAMOND) < diaCost) {
            messages.send(p, "errors.insufficient-diamonds", "<red>Need diamonds</red>",
                    Map.of("amount", String.valueOf(diaCost)));
            return;
        }
        double cost = mc.ringCost();
        Economy eco = vault.economy();
        boolean charge = mc.economyEnabled() && cost > 0.0 && eco != null;
        if (charge && !eco.has(p, cost)) {
            messages.send(p, "errors.insufficient-funds", "<red>No money</red>");
            return;
        }
        if (charge) {
            EconomyResponse wr = eco.withdrawPlayer(p, cost);
            if (!wr.transactionSuccess()) {
                messages.send(p, "errors.insufficient-funds", "<red>No money</red>");
                return;
            }
        }
        if (diaCost > 0 && !removeMaterial(p, Material.DIAMOND, diaCost)) {
            if (charge && eco != null) {
                eco.depositPlayer(p, cost);
            }
            messages.send(p, "errors.insufficient-diamonds", "<red>Need diamonds</red>",
                    Map.of("amount", String.valueOf(diaCost)));
            return;
        }
        ItemStack ring = items.createRing();
        HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(ring);
        if (!leftover.isEmpty()) {
            if (charge && eco != null) {
                eco.depositPlayer(p, cost);
            }
            if (diaCost > 0) {
                refundMaterial(p, Material.DIAMOND, diaCost);
            }
            messages.send(p, "errors.inventory-full", "<red>Full</red>");
            return;
        }
        if (cd > 0) {
            lastRingGrantMs.put(u, now);
        }
        messages.send(p, "engage.ring-given", "<green>OK</green>");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String pref = args[0].toLowerCase(Locale.ROOT);
            return Stream.of("accept", "ring")
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(pref))
                    .toList();
        }
        if (args.length == 2 && "accept".equalsIgnoreCase(args[0])) {
            List<String> out = new ArrayList<>();
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(pl.getName());
                }
            }
            return out;
        }
        return List.of();
    }

    private static int countMaterial(@NotNull Player p, @NotNull Material mat) {
        PlayerInventory inv = p.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && !stack.getType().isAir() && stack.getType() == mat) {
                n += stack.getAmount();
            }
        }
        return n;
    }

    /**
     * @return false if inventory changed unexpectedly and not enough was removed
     */
    private static boolean removeMaterial(@NotNull Player p, @NotNull Material mat, int amount) {
        PlayerInventory inv = p.getInventory();
        int left = amount;
        for (int i = 0; i < inv.getSize() && left > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType().isAir() || stack.getType() != mat) {
                continue;
            }
            int take = Math.min(left, stack.getAmount());
            int newAmt = stack.getAmount() - take;
            if (newAmt <= 0) {
                inv.setItem(i, null);
            } else {
                stack.setAmount(newAmt);
                inv.setItem(i, stack);
            }
            left -= take;
        }
        return left == 0;
    }

    private static void refundMaterial(@NotNull Player p, @NotNull Material mat, int amount) {
        if (amount <= 0) {
            return;
        }
        HashMap<Integer, ItemStack> over = p.getInventory().addItem(new ItemStack(mat, amount));
        for (ItemStack o : over.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), o);
        }
    }
}
