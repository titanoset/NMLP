package com.nmlp.repository;

import com.nmlp.model.PlayerRow;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class SqlPlayerRepository implements PlayerRepository {

    private final DataSource dataSource;
    private final Executor executor;

    public SqlPlayerRepository(@NotNull DataSource dataSource, @NotNull Executor executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }

    @Override
    public @NotNull CompletableFuture<Void> upsert(@NotNull UUID uuid, @NotNull String usernameLast, long now) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO players(uuid, username_last, gender_id, pronouns_id, setup_wizard_complete, first_seen, updated_at)
                        VALUES(?,?,NULL,NULL,0,?,?)
                        ON CONFLICT(uuid) DO UPDATE SET
                          username_last = excluded.username_last,
                          updated_at = excluded.updated_at
                        """)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, usernameLast);
                    ps.setLong(3, now);
                    ps.setLong(4, now);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Optional<PlayerRow>> find(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         SELECT p.uuid, p.username_last, g.code, pr.code, p.setup_wizard_complete, p.first_seen, p.updated_at
                         FROM players p
                         LEFT JOIN genders g ON g.id = p.gender_id
                         LEFT JOIN pronouns pr ON pr.id = p.pronouns_id
                         WHERE p.uuid = ?
                         """)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new PlayerRow(
                            UUID.fromString(rs.getString(1)),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getInt(5) != 0,
                            rs.getLong(6),
                            rs.getLong(7)
                    ));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> setGender(@NotNull UUID uuid, int genderId, long now) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE players SET gender_id = ?, updated_at = ? WHERE uuid = ?")) {
                ps.setInt(1, genderId);
                ps.setLong(2, now);
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> setPronouns(@NotNull UUID uuid, int pronounsId, long now) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE players SET pronouns_id = ?, updated_at = ? WHERE uuid = ?")) {
                ps.setInt(1, pronounsId);
                ps.setLong(2, now);
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> setSetupComplete(@NotNull UUID uuid, boolean done, long now) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE players SET setup_wizard_complete = ?, updated_at = ? WHERE uuid = ?")) {
                ps.setInt(1, done ? 1 : 0);
                ps.setLong(2, now);
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<String>> searchUsernames(@NotNull String prefix, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT username_last FROM players WHERE username_last LIKE ? ESCAPE '!' ORDER BY username_last LIMIT ?")) {
                String like = prefix.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                ps.setString(1, like);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(rs.getString(1));
                    }
                    return out;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
}
