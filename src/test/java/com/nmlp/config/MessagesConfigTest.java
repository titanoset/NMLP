package com.nmlp.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesConfigTest {

    @Test
    void flattensNestedKeys() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("prefix", "<gray>[N]</gray> ");
        yaml.set("risk.use", "<green>ok</green>");
        yaml.set("risk.by_action.sex.use", "<green>custom</green>");

        MessagesConfig cfg = new MessagesConfig(yaml);
        assertEquals("<gray>[N]</gray> ", cfg.raw("prefix", ""));
        assertEquals("<green>ok</green>", cfg.raw("risk.use", ""));
        assertEquals("<green>custom</green>", cfg.raw("risk.by_action.sex.use", ""));
    }

    @Test
    void rawFallsBackToDefault() {
        MessagesConfig cfg = new MessagesConfig(new YamlConfiguration());
        assertEquals("fallback", cfg.raw("missing.path", "fallback"));
    }
}
