package com.nmlp.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationRunnerTest {

    @Test
    void appliesBundledMigrations(@TempDir Path tempDir) throws Exception {
        Path dbFile = tempDir.resolve("migrate.db");
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:sqlite:" + dbFile.toAbsolutePath().toString().replace('\\', '/'));
        hc.setDriverClassName("org.sqlite.JDBC");
        hc.setMaximumPoolSize(1);
        hc.setPoolName("nmlp-test-migrate");

        try (HikariDataSource ds = new HikariDataSource(hc)) {
            MigrationRunner runner = new MigrationRunner(ds, MigrationRunnerTest.class.getClassLoader(), Logger.getLogger("test"));
            runner.migrate();

            try (Connection c = ds.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) AS n FROM schema_migrations")) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt("n"));
            }
        }
    }
}
