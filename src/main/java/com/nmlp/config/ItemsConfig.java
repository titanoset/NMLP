package com.nmlp.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Item display configuration from {@code items.yml}.
 */
public final class ItemsConfig {

    private final FileConfiguration c;

    public ItemsConfig(@NotNull FileConfiguration c) {
        this.c = c;
    }

    public @NotNull String docName(@NotNull String key) {
        return c.getString("documents." + key + ".name", "<gray>Document</gray>");
    }

    public int docCmd(@NotNull String key) {
        return c.getInt("documents." + key + ".custom-model-data", 0);
    }

    public @NotNull String docMaterial(@NotNull String key) {
        return c.getString("documents." + key + ".material", "PAPER");
    }

    public @NotNull String ringName() {
        return c.getString("ring.name", "<white>Ring</white>");
    }

    public java.util.@NotNull List<String> ringLore() {
        return c.getStringList("ring.lore");
    }
}
