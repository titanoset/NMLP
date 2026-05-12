package com.nmlp.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * GUI-related strings from {@code gui.yml}.
 */
public final class GuiConfig {

    private final FileConfiguration c;

    public GuiConfig(@NotNull FileConfiguration c) {
        this.c = c;
    }

    public @NotNull String title(@NotNull String path) {
        return c.getString(path + ".title", "<gray>Menu</gray>");
    }

    public int rows(@NotNull String path) {
        return Math.min(6, Math.max(1, c.getInt(path + ".rows", 4)));
    }

    public @NotNull String fillerMaterial() {
        return c.getString("filler.material", "GRAY_STAINED_GLASS_PANE");
    }

    public int backSlot() {
        return c.getInt("navigation.back-slot", 45);
    }

    public int nextSlot() {
        return c.getInt("navigation.next-slot", 53);
    }

    public int closeSlot() {
        return c.getInt("navigation.close-slot", 49);
    }

    /** Подпись из gui.yml → {@code labels.<key>}. */
    public @NotNull String label(@NotNull String key) {
        return c.getString("labels." + key, key);
    }

    /** Подпись типа связи семьи. */
    public @NotNull String linkTypeLabel(@NotNull String enumName) {
        return c.getString("link_type." + enumName, enumName);
    }
}
