package com.nmlp.database;

import com.nmlp.config.MainConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HikariSourceFactoryTest {

    @Test
    void createsPoolAndOpensDatabase(@TempDir Path tempDir) throws Exception {
        File serverRoot = tempDir.resolve("server").toFile();
        File dataFolder = new File(new File(serverRoot, "plugins"), "NMLP");
        assertTrue(dataFolder.mkdirs());

        YamlConfiguration yaml = new YamlConfiguration();
        File db = new File(dataFolder, "unit.db");
        yaml.set("database.file", db.getAbsolutePath());
        yaml.set("database.pool-size", 2);
        yaml.set("database.max-lifetime-ms", 600_000L);

        MainConfig cfg = new MainConfig(yaml);
        try (HikariDataSource ds = HikariSourceFactory.create(cfg, dataFolder)) {
            try (Connection c = ds.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1")) {
                assertTrue(rs.next());
            }
        }
        assertTrue(db.exists());
    }
}
