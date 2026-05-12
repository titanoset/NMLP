package com.nmlp.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Buffs parsing without potion entries (avoids {@link org.bukkit.Registry} when uninitialized in tests).
 */
class BuffsConfigTest {

    @Test
    void parsesFlagsAndEmptyActions() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("enabled", false);
        yaml.set("reset-risk-health-on-death", false);
        yaml.set("actions", null);

        BuffsConfig cfg = new BuffsConfig(yaml);
        assertFalse(cfg.enabled());
        assertFalse(cfg.resetRiskHealthOnDeath());
        assertTrue(cfg.actions().isEmpty());
    }

    @Test
    void parsesActionWithoutPotions() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("enabled", true);
        yaml.set("actions.testkey.cooldown-ms", 5000L);
        yaml.set("actions.testkey.abuse-window-ms", 120_000L);
        yaml.set("actions.testkey.max-uses-in-window", 3);
        yaml.set("actions.testkey.apply-primary-on-overuse", false);
        yaml.set("actions.testkey.max-health-loss-per-excess-use", 1.5);
        yaml.set("actions.testkey.min-max-health-lethal", 8.0);
        yaml.set("actions.testkey.kill-on-lethal", false);
        yaml.set("actions.testkey.primary", java.util.List.of());
        yaml.set("actions.testkey.penalty", java.util.List.of());

        BuffsConfig cfg = new BuffsConfig(yaml);
        BuffsConfig.RiskActionDefinition def = cfg.action("TESTKEY");
        assertNotNull(def);
        assertEquals("testkey", def.id());
        assertEquals(5000L, def.cooldownMs());
        assertEquals(120_000L, def.abuseWindowMs());
        assertEquals(3, def.maxUsesInWindow());
        assertFalse(def.applyPrimaryOnOveruse());
        assertEquals(1.5, def.maxHealthLossPerExcessUse(), 0.001);
        assertEquals(8.0, def.minMaxHealthLethal(), 0.001);
        assertFalse(def.killOnLethal());
        assertTrue(def.primary().isEmpty());
        assertTrue(def.penalty().isEmpty());
    }

    @Test
    void actionLookupIsCaseInsensitive() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("actions.MyAct.primary", java.util.List.of());
        yaml.set("actions.MyAct.penalty", java.util.List.of());

        BuffsConfig cfg = new BuffsConfig(yaml);
        assertNotNull(cfg.action("myact"));
        assertNull(cfg.action("missing"));
    }
}
