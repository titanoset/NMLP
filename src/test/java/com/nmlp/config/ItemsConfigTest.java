package com.nmlp.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemsConfigTest {

    @Test
    void docDefaults() {
        YamlConfiguration yaml = new YamlConfiguration();
        ItemsConfig cfg = new ItemsConfig(yaml);
        assertTrue(cfg.docName("any").contains("Document") || cfg.docName("any").contains("gray"));
        assertEquals(0, cfg.docCmd("any"));
        assertEquals("PAPER", cfg.docMaterial("any"));
    }

    @Test
    void ringDefaults() {
        ItemsConfig cfg = new ItemsConfig(new YamlConfiguration());
        assertTrue(cfg.ringName().length() > 0);
        assertTrue(cfg.ringLore().isEmpty());
    }
}
