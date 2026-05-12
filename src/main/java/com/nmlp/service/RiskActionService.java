package com.nmlp.service;

import com.nmlp.config.BuffsConfig;
import com.nmlp.config.BuffsConfig.PotionSpec;
import com.nmlp.config.BuffsConfig.RiskActionDefinition;
import com.nmlp.config.MessageService;
import com.nmlp.config.ReloadManager;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Config-driven risky actions: buffs, abuse window, max-health loss, possible death.
 */
public final class RiskActionService {

    private final JavaPlugin plugin;
    private final ReloadManager reload;
    private final MessageService messages;
    private final NamespacedKey cumulativeHealthKey;

    private final Map<String, Map<UUID, Long>> lastUse = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, Deque<Long>>> useWindows = new ConcurrentHashMap<>();
    private final Map<UUID, Double> cumulativeMaxHealthLoss = new ConcurrentHashMap<>();

    public RiskActionService(@NotNull JavaPlugin plugin, @NotNull ReloadManager reload, @NotNull MessageService messages) {
        this.plugin = plugin;
        this.reload = reload;
        this.messages = messages;
        this.cumulativeHealthKey = new NamespacedKey(plugin, "cumulative_risk_max_health");
    }

    public void tryUse(@NotNull Player player, @NotNull String actionIdRaw) {
        BuffsConfig cfg = reload.buffs();
        if (!cfg.enabled()) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    messages.send(player, "risk.disabled", "<gray>Отключено.</gray>"));
            return;
        }
        String id = actionIdRaw.toLowerCase(Locale.ROOT);
        RiskActionDefinition def = cfg.action(id);
        if (def == null) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    messages.send(player, "risk.unknown", "<red>Неизвестное действие.</red>", Map.of("id", actionIdRaw)));
            return;
        }
        if (!player.hasPermission("nmlp.risk." + id) && !player.hasPermission("nmlp.risk.*")) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    messages.send(player, "errors.no-permission", "<red>Нет прав.</red>"));
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> execute(player, def));
    }

    private void execute(@NotNull Player player, @NotNull RiskActionDefinition def) {
        if (!player.isOnline()) {
            return;
        }
        long now = System.currentTimeMillis();
        UUID u = player.getUniqueId();
        String id = def.id();
        Long last = lastUse.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).get(u);
        if (last != null && now - last < def.cooldownMs()) {
            messages.send(player, "risk.cooldown", "<red>Слишком часто.</red>");
            return;
        }
        Deque<Long> window = useWindows.computeIfAbsent(id, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(u, k -> new ArrayDeque<>());
        window.addLast(now);
        long cutoff = now - def.abuseWindowMs();
        while (!window.isEmpty() && window.peekFirst() < cutoff) {
            window.pollFirst();
        }
        int count = window.size();
        boolean overuse = count > def.maxUsesInWindow();
        if (overuse) {
            messages.sendRiskKind(player, id, "overuse", "<red>Перегрузка организма.</red>", Map.of("id", id));
        } else {
            messages.sendRiskKind(player, id, "use", "<green>Действие выполнено.</green>", Map.of("id", id));
        }
        boolean applyPrimary = !overuse || def.applyPrimaryOnOveruse();
        if (applyPrimary) {
            applyPotions(player, def.primary());
        }
        if (overuse) {
            applyPotions(player, def.penalty());
            applyMaxHealthLoss(player, def);
        }
        lastUse.computeIfAbsent(id, k -> new ConcurrentHashMap<>()).put(u, now);
    }

    private static void applyPotions(@NotNull Player player, @NotNull java.util.List<PotionSpec> specs) {
        for (PotionSpec s : specs) {
            player.addPotionEffect(new PotionEffect(s.type(), s.durationTicks(), s.amplifier(), false, true, true));
        }
    }

    private void applyMaxHealthLoss(@NotNull Player player, @NotNull RiskActionDefinition def) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        double add = def.maxHealthLossPerExcessUse();
        double total = cumulativeMaxHealthLoss.merge(player.getUniqueId(), add, Double::sum);
        attr.removeModifier(cumulativeHealthKey);
        if (total > 0) {
            attr.addModifier(new AttributeModifier(cumulativeHealthKey, -total, AttributeModifier.Operation.ADD_NUMBER));
        }
        double max = player.getMaxHealth();
        if (player.getHealth() > max) {
            player.setHealth(Math.max(0.5, max - 0.01));
        }
        if (def.killOnLethal() && max <= def.minMaxHealthLethal()) {
            messages.sendRiskKind(player, def.id(), "lethal", "<dark_red>Критическое состояние…</dark_red>", Map.of("id", def.id()));
            player.damage(player.getHealth() + 1.0);
        }
    }

    /** Removes cumulative max-health penalty (e.g. on death). */
    public void clearRiskHealthModifier(@NotNull Player player) {
        if (!reload.buffs().resetRiskHealthOnDeath()) {
            return;
        }
        cumulativeMaxHealthLoss.remove(player.getUniqueId());
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.removeModifier(cumulativeHealthKey);
        }
    }

    public void clearWindows(@NotNull UUID player) {
        for (Map<UUID, Deque<Long>> m : useWindows.values()) {
            m.remove(player);
        }
        for (Map<UUID, Long> m : lastUse.values()) {
            m.remove(player);
        }
    }
}
