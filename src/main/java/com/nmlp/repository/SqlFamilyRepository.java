package com.nmlp.repository;

import com.nmlp.domain.FamilyLinkType;
import com.nmlp.model.FamilyLinkRow;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class SqlFamilyRepository implements FamilyRepository {

    private final DataSource dataSource;
    private final Executor executor;

    public SqlFamilyRepository(@NotNull DataSource dataSource, @NotNull Executor executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }

    @Override
    public @NotNull CompletableFuture<Void> addLink(
            @NotNull UUID from,
            @NotNull UUID to,
            @NotNull FamilyLinkType type,
            long createdAt
    ) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         INSERT INTO family_links(from_uuid, to_uuid, link_type, created_at, ended_at)
                         VALUES(?,?,?,?,NULL)
                         """)) {
                ps.setString(1, from.toString());
                ps.setString(2, to.toString());
                ps.setString(3, type.toDb());
                ps.setLong(4, createdAt);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<FamilyLinkRow>> activeLinksFor(@NotNull UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            String s = player.toString();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         SELECT id, from_uuid, to_uuid, link_type, created_at, ended_at
                         FROM family_links
                         WHERE ended_at IS NULL AND (from_uuid = ? OR to_uuid = ?)
                         """)) {
                ps.setString(1, s);
                ps.setString(2, s);
                try (ResultSet rs = ps.executeQuery()) {
                    List<FamilyLinkRow> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(new FamilyLinkRow(
                                rs.getLong(1),
                                UUID.fromString(rs.getString(2)),
                                UUID.fromString(rs.getString(3)),
                                FamilyLinkType.fromDb(rs.getString(4)),
                                rs.getLong(5),
                                rs.getObject(6) == null ? null : rs.getLong(6)
                        ));
                    }
                    return out;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<Void> addChildParent(
            @NotNull UUID child,
            @NotNull UUID parent,
            boolean biological,
            long createdAt
    ) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         INSERT INTO children(child_uuid, parent_uuid, biological, created_at, ended_at)
                         VALUES(?,?,?,?,NULL)
                         """)) {
                ps.setString(1, child.toString());
                ps.setString(2, parent.toString());
                ps.setInt(3, biological ? 1 : 0);
                ps.setLong(4, createdAt);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<UUID>> parentsOf(@NotNull UUID child) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         SELECT parent_uuid FROM children
                         WHERE child_uuid = ? AND ended_at IS NULL
                         """)) {
                ps.setString(1, child.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    List<UUID> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(UUID.fromString(rs.getString(1)));
                    }
                    return out;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    @Override
    public @NotNull CompletableFuture<List<UUID>> childrenOf(@NotNull UUID parent) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         SELECT child_uuid FROM children
                         WHERE parent_uuid = ? AND ended_at IS NULL
                         """)) {
                ps.setString(1, parent.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    List<UUID> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(UUID.fromString(rs.getString(1)));
                    }
                    return out;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
}
