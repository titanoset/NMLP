package com.nmlp.config;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Risk / buff actions from {@code buffs.yml} (action keys and semantics are server-defined).
 */
public final class BuffsConfig {

    private final boolean enabled;
    private final boolean resetRiskHealthOnDeath;
    private final Map<String, RiskActionDefinition> actions;

    public BuffsConfig(@NotNull FileConfiguration c) {
        this.enabled = c.getBoolean("enabled", true);
        this.resetRiskHealthOnDeath = c.getBoolean("reset-risk-health-on-death", true);
        ConfigurationSection root = c.getConfigurationSection("actions");
        if (root == null) {
            this.actions = Map.of();
            return;
        }
        Map<String, RiskActionDefinition> out = new LinkedHashMap<>();
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            RiskActionDefinition def = parseAction(key, sec);
            if (def != null) {
                out.put(key.toLowerCase(Locale.ROOT), def);
            }
        }
        this.actions = Collections.unmodifiableMap(out);
    }

    private static @Nullable RiskActionDefinition parseAction(@NotNull String id, @NotNull ConfigurationSection sec) {
        long cooldown = sec.getLong("cooldown-ms", 30_000L);
        long window = sec.getLong("abuse-window-ms", 180_000L);
        int maxUses = sec.getInt("max-uses-in-window", 5);
        boolean primaryOnOveruse = sec.getBoolean("apply-primary-on-overuse", true);
        List<PotionSpec> primary = parsePotions(sec, "primary");
        List<PotionSpec> penalty = parsePotions(sec, "penalty");
        double loss = sec.getDouble("max-health-loss-per-excess-use", 2.0);
        double lethal = sec.getDouble("min-max-health-lethal", 6.0);
        boolean kill = sec.getBoolean("kill-on-lethal", true);
        return new RiskActionDefinition(id.toLowerCase(Locale.ROOT), cooldown, window, maxUses, primaryOnOveruse, primary, penalty, loss, lethal, kill);
    }

    private static @NotNull List<PotionSpec> parsePotions(@NotNull ConfigurationSection sec, @NotNull String listKey) {
        List<Map<?, ?>> raw = sec.getMapList(listKey);
        if (raw.isEmpty()) {
            return List.of();
        }
        List<PotionSpec> out = new ArrayList<>();
        for (Map<?, ?> m : raw) {
            Object t = m.get("type");
            if (t == null) {
                continue;
            }
            PotionEffectType pet = resolvePotionType(String.valueOf(t));
            if (pet == null) {
                continue;
            }
            Object durObj = m.get("duration-ticks");
            Object ampObj = m.get("amplifier");
            int dur = durObj instanceof Number n ? n.intValue() : 100;
            int amp = ampObj instanceof Number n ? n.intValue() : 0;
            out.add(new PotionSpec(pet, dur, amp));
        }
        return List.copyOf(out);
    }

    private static @Nullable PotionEffectType resolvePotionType(@NotNull String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (!s.contains(":")) {
            s = "minecraft:" + s.replace('_', '.');
        }
        NamespacedKey key = NamespacedKey.fromString(s);
        if (key == null) {
            return null;
        }
        return Registry.POTION_EFFECT_TYPE.get(key);
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean resetRiskHealthOnDeath() {
        return resetRiskHealthOnDeath;
    }

    public @NotNull Map<String, RiskActionDefinition> actions() {
        return actions;
    }

    public @Nullable RiskActionDefinition action(@NotNull String id) {
        return actions.get(id.toLowerCase(Locale.ROOT));
    }

    public record RiskActionDefinition(
            @NotNull String id,
            long cooldownMs,
            long abuseWindowMs,
            int maxUsesInWindow,
            boolean applyPrimaryOnOveruse,
            @NotNull List<PotionSpec> primary,
            @NotNull List<PotionSpec> penalty,
            double maxHealthLossPerExcessUse,
            double minMaxHealthLethal,
            boolean killOnLethal
    ) {
    }

    public record PotionSpec(@NotNull PotionEffectType type, int durationTicks, int amplifier) {
    }
}
