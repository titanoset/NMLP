package com.nmlp.integration;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * TAB plugin has no stable public Maven artifact; use PlaceholderAPI in TAB config.
 * Example TAB group line: {@code prefix: "%nmlp_status% %heart_symbol%"} with heart from resource pack.
 */
public final class TabPlaceholderNote {

    public static void logOnce(@NotNull JavaPlugin plugin) {
        Logger log = plugin.getLogger();
        if (plugin.getServer().getPluginManager().getPlugin("TAB") != null) {
            log.info("TAB detected: configure placeholders via PlaceholderAPI (e.g. %nmlp_partner%, %nmlp_status%).");
        }
    }
}
