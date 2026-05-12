package com.nmlp.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Loads bundled defaults and merges user files on disk.
 */
public final class YamlFiles {

    private YamlFiles() {
    }

    public static @NotNull FileConfiguration loadMerged(
            @NotNull File dataFolder,
            @NotNull String name,
            @NotNull JavaPlugin plugin
    ) {
        File out = new File(dataFolder, name);
        if (!out.exists()) {
            dataFolder.mkdirs();
            try (InputStream in = plugin.getResource(name)) {
                if (in != null) {
                    java.nio.file.Files.copy(in, out.toPath());
                } else {
                    if (!out.createNewFile()) {
                        plugin.getLogger().warning("Could not create " + name);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create " + name, e);
            }
        }
        YamlConfiguration user = YamlConfiguration.loadConfiguration(out);
        YamlConfiguration def = new YamlConfiguration();
        try (InputStream in = plugin.getResource(name)) {
            if (in != null) {
                def.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "No default resource for " + name, e);
        }
        user.setDefaults(def);
        user.options().copyDefaults(true);
        try {
            user.save(out);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not save defaults for " + name, e);
        }
        return user;
    }
}
