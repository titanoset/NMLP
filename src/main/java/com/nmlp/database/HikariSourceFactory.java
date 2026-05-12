package com.nmlp.database;

import com.nmlp.config.MainConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Creates a SQLite {@link HikariDataSource} from plugin configuration.
 */
public final class HikariSourceFactory {

    private HikariSourceFactory() {
    }

    /**
     * @param dataFolder plugin data folder (e.g. {@code plugins/NMLP})
     */
    public static @NotNull HikariDataSource create(@NotNull MainConfig cfg, @NotNull File dataFolder) {
        File serverRoot = dataFolder.getParentFile().getParentFile();
        File dbFile = new File(cfg.databaseFile());
        if (!dbFile.isAbsolute()) {
            dbFile = new File(serverRoot, cfg.databaseFile().replace('/', File.separatorChar));
        }
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath().replace('\\', '/');
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(jdbcUrl);
        hc.setMaximumPoolSize(Math.max(1, cfg.poolSize()));
        hc.setMaxLifetime(cfg.maxLifetimeMs());
        hc.setPoolName("NMLP-SQLite");
        hc.setDriverClassName("org.sqlite.JDBC");
        hc.addDataSourceProperty("foreign_keys", "true");
        return new HikariDataSource(hc);
    }
}
