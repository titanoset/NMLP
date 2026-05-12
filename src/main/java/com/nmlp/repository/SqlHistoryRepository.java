package com.nmlp.repository;

import com.nmlp.domain.HistoryEventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class SqlHistoryRepository implements HistoryRepository {

    private final DataSource dataSource;
    private final Executor executor;

    public SqlHistoryRepository(@NotNull DataSource dataSource, @NotNull Executor executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }

    @Override
    public @NotNull CompletableFuture<Void> append(
            @NotNull HistoryEventType type,
            @Nullable UUID actor,
            @Nullable UUID target,
            @Nullable UUID related,
            @Nullable String payload,
            long createdAt
    ) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("""
                         INSERT INTO history(event_type, actor_uuid, target_uuid, related_uuid, payload, created_at)
                         VALUES(?,?,?,?,?,?)
                         """)) {
                ps.setString(1, type.toDb());
                ps.setString(2, actor == null ? null : actor.toString());
                ps.setString(3, target == null ? null : target.toString());
                ps.setString(4, related == null ? null : related.toString());
                ps.setString(5, payload);
                ps.setLong(6, createdAt);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
}
