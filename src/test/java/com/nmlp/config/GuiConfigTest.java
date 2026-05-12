package com.nmlp.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiConfigTest {

    @Test
    void rowsClampedBetweenOneAndSix() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("menu.rows", 0);
        GuiConfig cfg = new GuiConfig(yaml);
        assertEquals(1, cfg.rows("menu"));

        yaml.set("menu.rows", 99);
        cfg = new GuiConfig(yaml);
        assertEquals(6, cfg.rows("menu"));
    }

    @Test
    void labelFallsBackToKey() {
        YamlConfiguration yaml = new YamlConfiguration();
        GuiConfig cfg = new GuiConfig(yaml);
        assertEquals("unknown", cfg.label("unknown"));
    }

    @Test
    void fillerMaterialDefault() {
        assertTrue(new GuiConfig(new YamlConfiguration()).fillerMaterial().length() > 0);
    }
}
