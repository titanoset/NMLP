package com.nmlp.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainConfigTest {

    @Test
    void defaultsWhenEmpty() {
        MainConfig cfg = new MainConfig(new YamlConfiguration());
        assertTrue(cfg.databaseFile().contains("data.db"));
        assertEquals(4, cfg.poolSize());
        assertFalse(cfg.economyEnabled());
        assertTrue(cfg.affectionEnabled());
        assertEquals(4.0, cfg.affectionMaxDistance(), 0.001);
        assertEquals(0L, cfg.ringGrantCooldownMs());
        assertEquals(20, cfg.ringPurchaseDiamonds());
    }

    @Test
    void readsOverrides() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("database.file", "/tmp/custom.db");
        yaml.set("database.pool-size", 8);
        yaml.set("economy.enabled", true);
        yaml.set("economy.engage-cost", 100.5);
        yaml.set("affection.enabled", false);
        yaml.set("affection.max-distance", 12.5);
        yaml.set("cooldowns.ring-grant-ms", 12_000L);
        yaml.set("ring.purchase-diamonds", 3);
        yaml.set("debug", true);

        MainConfig cfg = new MainConfig(yaml);
        assertEquals("/tmp/custom.db", cfg.databaseFile());
        assertEquals(8, cfg.poolSize());
        assertTrue(cfg.economyEnabled());
        assertEquals(100.5, cfg.engageCost(), 0.001);
        assertFalse(cfg.affectionEnabled());
        assertEquals(12.5, cfg.affectionMaxDistance(), 0.001);
        assertTrue(cfg.debug());
        assertEquals(12_000L, cfg.ringGrantCooldownMs());
        assertEquals(3, cfg.ringPurchaseDiamonds());
    }
}
