package com.nmlp.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Raw message templates from {@code messages.yml}.
 */
public final class MessagesConfig {

    private final Map<String, String> strings;

    public MessagesConfig(@NotNull FileConfiguration c) {
        Map<String, String> out = new HashMap<>();
        flatten("", c.getValues(true), out);
        this.strings = Collections.unmodifiableMap(out);
    }

    private static void flatten(String prefix, Map<String, Object> map, Map<String, String> out) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            Object v = e.getValue();
            if (v instanceof Map<?, ?> nested) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sn = (Map<String, Object>) nested;
                flatten(key, sn, out);
            } else if (v != null) {
                out.put(key, String.valueOf(v));
            }
        }
    }

    public @NotNull String raw(@NotNull String path, @NotNull String def) {
        return strings.getOrDefault(path, def);
    }
}
