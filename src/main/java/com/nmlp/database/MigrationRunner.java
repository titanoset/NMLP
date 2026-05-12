package com.nmlp.database;

import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
/**
 * Applies versioned SQL resources from {@code /migrations}.
 */
public final class MigrationRunner {

    private final DataSource dataSource;
    private final ClassLoader resourceLoader;
    private final Logger log;

    public MigrationRunner(@NotNull DataSource dataSource, @NotNull ClassLoader resourceLoader, @NotNull Logger log) {
        this.dataSource = dataSource;
        this.resourceLoader = resourceLoader;
        this.log = log;
    }

    public void migrate() throws Exception {
        List<Integer> versions = listMigrationVersions();
        Collections.sort(versions);
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            ensureMigrationsTable(c);
            for (int v : versions) {
                if (alreadyApplied(c, v)) {
                    continue;
                }
                String path = findPathForVersion(v);
                if (path == null) {
                    throw new IllegalStateException("Missing migration file for version " + v);
                }
                applyFile(c, path);
                record(c, v);
                c.commit();
                log.info("Applied migration V" + v);
            }
        }
    }

    private void ensureMigrationsTable(Connection c) throws Exception {
        try (Statement st = c.createStatement()) {
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS schema_migrations (version INTEGER PRIMARY KEY, applied_at INTEGER NOT NULL)"
            );
        }
    }

    private boolean alreadyApplied(Connection c, int v) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM schema_migrations WHERE version = ?")) {
            ps.setInt(1, v);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void record(Connection c, int v) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO schema_migrations(version, applied_at) VALUES(?, ?)")) {
            ps.setInt(1, v);
            ps.setLong(2, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    private void applyFile(Connection c, String resourcePath) throws Exception {
        String sql = stripLineCommentsOutsideQuotes(readResource(resourcePath));
        for (String stmt : splitStatements(sql)) {
            if (stmt.isBlank()) {
                continue;
            }
            try (Statement st = c.createStatement()) {
                st.executeUpdate(stmt);
            }
        }
    }

    private String readResource(String path) throws Exception {
        try (InputStream in = resourceLoader.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Resource not found: " + path);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        }
    }

    private List<Integer> listMigrationVersions() {
        List<Integer> out = new ArrayList<>();
        try {
            java.net.URL url = resourceLoader.getResource("migrations");
            if (url == null) {
                return out;
            }
            // JAR: use walk if possible; fallback scan known V001
            for (int i = 1; i < 1000; i++) {
                String p = migrationPath(i);
                if (resourceLoader.getResourceAsStream(p) != null) {
                    out.add(i);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private String findPathForVersion(int v) {
        String p = migrationPath(v);
        if (resourceLoader.getResourceAsStream(p) != null) {
            return p;
        }
        return null;
    }

    private static String migrationPath(int v) {
        return "migrations/V" + String.format("%03d", v) + "__init.sql";
    }

    /**
     * Removes {@code -- ...} line comments except inside single-quoted strings (simplified for our migrations).
     */
    static @NotNull String stripLineCommentsOutsideQuotes(@NotNull String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        boolean inSingle = false;
        int i = 0;
        while (i < sql.length()) {
            char ch = sql.charAt(i);
            if (ch == '\'') {
                inSingle = !inSingle;
                out.append(ch);
                i++;
                continue;
            }
            if (!inSingle && ch == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                while (i < sql.length() && sql.charAt(i) != '\n') {
                    i++;
                }
                if (i < sql.length()) {
                    out.append('\n');
                    i++;
                }
                continue;
            }
            out.append(ch);
            i++;
        }
        return out.toString();
    }

    /**
     * Split on semicolons outside of quotes — simplified for our migrations.
     */
    private List<String> splitStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSingle = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'') {
                inSingle = !inSingle;
            }
            if (ch == ';' && !inSingle) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        String tail = cur.toString().trim();
        if (!tail.isEmpty()) {
            out.add(tail);
        }
        return out;
    }
}
