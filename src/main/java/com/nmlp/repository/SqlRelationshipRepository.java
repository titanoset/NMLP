package com.nmlp.repository;

import com.nmlp.domain.RelationshipStatus;
import com.nmlp.model.RelationshipRow;
import com.nmlp.util.UuidPair;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class SqlRelationshipRepository implements RelationshipRepository {

    private final DataSource dataSource;
    private final Executor executor;

    public SqlRelationshipRepository(@NotNull DataSource dataSource, @NotNull Executor executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }

    private static @NotNull RelationshipRow mapRow(@NotNull ResultSet rs) throws Exception {
        return new RelationshipRow(
                rs.getLong("id"),
                UUID.fromString(rs.getString("player_low")),
                UUID.fromString(rs.getString("player_high")),
                RelationshipStatus.fromDb(rs.getString("status")),
                rs.getLong("started_at"),
                rs.getObject("ended_at") == null ? null : rs.getLong("ended_at")
        );
    }

    @Override
    public @NotNull CompletableFuture<Optional<RelationshipRow>> findActiveFor(@NotNull UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            String s = player.toString();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         SELECT id, player_low, player_high, status, started_at, ended_at
                         FROM relationships
                         WHERE ended_at IS NULL AND (player_low = ? OR player_high = ?)
                         LIMIT 1
                         """)) {
                ps.setString(1, s);
                ps.setString(2, s);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(mapRow(rs));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> hasActiveMarriage(@NotNull UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            String s = player.toString();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         SELECT 1 FROM relationships
                         WHERE ended_at IS NULL AND status = 'MARRIED' AND (player_low = ? OR player_high = ?)
                         LIMIT 1
                         """)) {
                ps.setString(1, s);
                ps.setString(2, s);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Long> insertActive(
            @NotNull UuidPair pair,
            @NotNull RelationshipStatus status,
            long startedAt
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         INSERT INTO relationships(player_low, player_high, status, started_at, ended_at)
                         VALUES(?,?,?,?,NULL)
                         """, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, pair.low().toString());
                ps.setString(2, pair.high().toString());
                ps.setString(3, status.toDb());
                ps.setLong(4, startedAt);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new IllegalStateException("No generated id");
                    }
                    return keys.getLong(1);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> updateStatus(long relationshipId, @NotNull RelationshipStatus status) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("UPDATE relationships SET status = ? WHERE id = ?")) {
                ps.setString(1, status.toDb());
                ps.setLong(2, relationshipId);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> endRelationship(long relationshipId, long endedAt) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("UPDATE relationships SET ended_at = ? WHERE id = ?")) {
                ps.setLong(1, endedAt);
                ps.setLong(2, relationshipId);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> insertEngagement(long relationshipId, long proposedAt, long acceptedAt) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         INSERT INTO engagements(relationship_id, proposed_at, accepted_at, ended_at)
                         VALUES(?,?,?,NULL)
                         """)) {
                ps.setLong(1, relationshipId);
                ps.setLong(2, proposedAt);
                ps.setLong(3, acceptedAt);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> insertMarriage(long relationshipId, long marriedAt) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         INSERT INTO marriages(relationship_id, married_at, divorced_at)
                         VALUES(?,?,NULL)
                         """)) {
                ps.setLong(1, relationshipId);
                ps.setLong(2, marriedAt);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> divorceMarriage(long relationshipId, long divorcedAt) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         UPDATE marriages SET divorced_at = ? WHERE relationship_id = ? AND divorced_at IS NULL
                         """)) {
                ps.setLong(1, divorcedAt);
                ps.setLong(2, relationshipId);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> finalizeDivorce(long relationshipId, long endedAt) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection()) {
                c.setAutoCommit(false);
                try (PreparedStatement ps1 = c.prepareStatement(
                        "UPDATE engagements SET ended_at = ? WHERE relationship_id = ? AND ended_at IS NULL")) {
                    ps1.setLong(1, endedAt);
                    ps1.setLong(2, relationshipId);
                    ps1.executeUpdate();
                }
                try (PreparedStatement ps2 = c.prepareStatement(
                        "UPDATE marriages SET divorced_at = ? WHERE relationship_id = ? AND divorced_at IS NULL")) {
                    ps2.setLong(1, endedAt);
                    ps2.setLong(2, relationshipId);
                    ps2.executeUpdate();
                }
                try (PreparedStatement ps3 = c.prepareStatement(
                        "UPDATE relationships SET ended_at = ?, status = 'DIVORCED' WHERE id = ?")) {
                    ps3.setLong(1, endedAt);
                    ps3.setLong(2, relationshipId);
                    ps3.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<RelationshipRow>> historyFor(@NotNull UUID player, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String s = player.toString();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         SELECT id, player_low, player_high, status, started_at, ended_at
                         FROM relationships
                         WHERE player_low = ? OR player_high = ?
                         ORDER BY COALESCE(ended_at, started_at) DESC
                         LIMIT ?
                         """)) {
                ps.setString(1, s);
                ps.setString(2, s);
                ps.setInt(3, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<RelationshipRow> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(mapRow(rs));
                    }
                    return out;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<UUID>> exPartners(@NotNull UUID player, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String s = player.toString();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         SELECT player_low, player_high FROM relationships
                         WHERE ended_at IS NOT NULL AND (player_low = ? OR player_high = ?)
                         ORDER BY ended_at DESC
                         LIMIT ?
                         """)) {
                ps.setString(1, s);
                ps.setString(2, s);
                ps.setInt(3, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<UUID> out = new ArrayList<>();
                    while (rs.next()) {
                        UUID a = UUID.fromString(rs.getString(1));
                        UUID b = UUID.fromString(rs.getString(2));
                        out.add(a.equals(player) ? b : a);
                    }
                    return out;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
}
