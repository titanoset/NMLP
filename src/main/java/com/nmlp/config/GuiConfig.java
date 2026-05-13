package com.nmlp.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * GUI-related strings from {@code gui.yml}.
 */
public final class GuiConfig {

    /** In case {@code labels.hub_*} are missing from an older {@code gui.yml} on disk (defaults merge quirks). */
    private static final Map<String, String> HUB_LABEL_DEFAULTS = Map.ofEntries(
            Map.entry("hub_profile", "<green>Профиль и статус</green>"),
            Map.entry("hub_profile_hint", "<dark_gray>Партнёр, ты, история. Команда: </dark_gray><yellow>/partner</yellow>"),
            Map.entry("hub_family", "<aqua>Семья</aqua>"),
            Map.entry("hub_family_hint", "<dark_gray>Связи и родственники. Команда: </dark_gray><yellow>/family</yellow>"),
            Map.entry("hub_tree", "<green>Родословная</green>"),
            Map.entry("hub_tree_hint", "<dark_gray>Древо отношений. Команда: </dark_gray><yellow>/tree</yellow>"),
            Map.entry("hub_history", "<gold>История</gold>"),
            Map.entry("hub_history_hint", "<dark_gray>Прошлые отношения (из профиля тоже).</dark_gray>"),
            Map.entry("hub_settings", "<yellow>Настройки</yellow>"),
            Map.entry("hub_settings_hint", "<dark_gray>Пол. Команда: </dark_gray><yellow>/gender male</yellow> <gray>|</gray> <yellow>/gender female</yellow>"),
            Map.entry("hub_close", "<red>Закрыть</red>"),
            Map.entry("hub_close_hint", "<dark_gray>Это меню: </dark_gray><yellow>/menu</yellow>"),
            Map.entry("nav_back_to_menu", "<gray>◀ </gray><yellow>В меню</yellow>"),
            Map.entry("nav_back_to_menu_hint", "<dark_gray>Вернуться в главное меню. </dark_gray><yellow>/menu</yellow>")
    );

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

    /**
     * Slot for «back to hub» button: bottom row of a chest with {@code rows} rows.
     * Uses {@code navigation.back-row-from-bottom} (1 = lowest row) and {@code navigation.back-column} (0–8).
     */
    public int toHubSlot(int rows) {
        int size = Math.max(9, Math.min(54, rows * 9));
        int rowFromBottom = Math.max(1, Math.min(rows, c.getInt("navigation.back-row-from-bottom", 1)));
        int col = Math.max(0, Math.min(8, c.getInt("navigation.back-column", 0)));
        int rowZeroBased = rows - rowFromBottom;
        int slot = rowZeroBased * 9 + col;
        return Math.max(0, Math.min(size - 1, slot));
    }

    public int nextSlot() {
        return c.getInt("navigation.next-slot", 53);
    }

    public int closeSlot() {
        return c.getInt("navigation.close-slot", 49);
    }

    /** Подпись из gui.yml → {@code labels.<key>}. */
    public @NotNull String label(@NotNull String key) {
        String path = "labels." + key;
        String fromFile = c.getString(path);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile;
        }
        return HUB_LABEL_DEFAULTS.getOrDefault(key, key);
    }

    /** Подпись типа связи семьи. */
    public @NotNull String linkTypeLabel(@NotNull String enumName) {
        return c.getString("link_type." + enumName, enumName);
    }

    /** Слот кнопки главного меню (хаб). Ключи: profile, family, tree, history, settings, close. */
    public int hubSlot(@NotNull String key) {
        int max = Math.max(0, rows("hub") * 9 - 1);
        int def = switch (key) {
            case "profile" -> 11;
            case "family" -> 13;
            case "tree" -> 15;
            case "history" -> 21;
            case "settings" -> 23;
            case "close" -> 31;
            default -> 4;
        };
        int v = c.getInt("hub.slots." + key, def);
        return Math.min(max, Math.max(0, v));
    }
}
