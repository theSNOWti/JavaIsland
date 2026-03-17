package com.javaisland.repo;

import com.javaisland.db.Sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class PlayerTaskResultRepository {

    public void ensureRowExists(long playerId, long taskId) {
        // create row if not exists; keep counters defaulting to 0
        String sql = """
            INSERT INTO player_task_result(player_id, task_id, completed, attempts, hints_used)
            VALUES(?, ?, 0, 0, 0)
            ON CONFLICT(player_id, task_id) DO NOTHING
            """;
        try (Connection c = Sqlite.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
          ps.setLong(1, playerId);
          ps.setLong(2, taskId);
          ps.executeUpdate();
        } catch (SQLException e) {
          throw new RuntimeException("DB error in PlayerTaskResultRepository.ensureRowExists", e);
        }
    }
    
    public void incrementAttempts(long playerId, long taskId) {
        String sql = """
            INSERT INTO player_task_result(player_id, task_id, completed, attempts, hints_used)
            VALUES(?, ?, 0, 1, 0)
            ON CONFLICT(player_id, task_id)
            DO UPDATE SET attempts = attempts + 1
            """;
        try (Connection c = Sqlite.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
          ps.setLong(1, playerId);
          ps.setLong(2, taskId);
          ps.executeUpdate();
        } catch (SQLException e) {
          throw new RuntimeException("DB error in PlayerTaskResultRepository.incrementAttempts", e);
        }
    }
    
    public void setHintsUsed(long playerId, long taskId, int hintsUsed) {
        String sql = """
            INSERT INTO player_task_result(player_id, task_id, completed, attempts, hints_used)
            VALUES(?, ?, 0, 0, ?)
            ON CONFLICT(player_id, task_id)
            DO UPDATE SET hints_used = CASE
              WHEN excluded.hints_used > hints_used THEN excluded.hints_used
              ELSE hints_used
            END
            """;
        try (Connection c = Sqlite.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
          ps.setLong(1, playerId);
          ps.setLong(2, taskId);
          ps.setInt(3, Math.max(0, hintsUsed));
          ps.executeUpdate();
        } catch (SQLException e) {
          throw new RuntimeException("DB error in PlayerTaskResultRepository.setHintsUsed", e);
        }
    }
    
    public void markCompleted(long playerId, long taskId) {
        String sql = """
            INSERT INTO player_task_result(player_id, task_id, completed, attempts, hints_used)
            VALUES(?, ?, 1, 0, 0)
            ON CONFLICT(player_id, task_id)
            DO UPDATE SET completed = 1
            """;
        try (Connection c = Sqlite.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
          ps.setLong(1, playerId);
          ps.setLong(2, taskId);
          ps.executeUpdate();
        } catch (SQLException e) {
          throw new RuntimeException("DB error in PlayerTaskResultRepository.markCompleted", e);
        }
    }

    private long prologueLevelId() {
        String sql = """
            SELECT id
            FROM level
            WHERE title = ?
            ORDER BY id ASC
            LIMIT 1
            """;

        try (Connection c = Sqlite.open();
            PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, "Prolog");
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
            // If no prologue level exists, treat "no prologue" as "exclude nothing".
            return -1L;
            }
            return rs.getLong("id");
        }
        } catch (SQLException e) {
        throw new RuntimeException("DB error in PlayerTaskResultRepository.prologueLevelId", e);
        }
    }

    public int countCompletedNonPrologue(long playerId) {
        long prologueId = prologueLevelId();

        String sql = """
            SELECT COUNT(*) AS cnt
            FROM player_task_result ptr
            JOIN task t ON t.id = ptr.task_id
            WHERE ptr.player_id = ?
            AND ptr.completed = 1
            AND (? < 0 OR t.level_id <> ?)
            """;

        try (Connection c = Sqlite.open();
            PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setLong(1, playerId);
        ps.setLong(2, prologueId);
        ps.setLong(3, prologueId);

        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("cnt") : 0;
        }
        } catch (SQLException e) {
        throw new RuntimeException("DB error in PlayerTaskResultRepository.countCompletedNonPrologue", e);
        }
    }

    public int countAllNonPrologueTasks() {
        long prologueId = prologueLevelId();

        String sql = """
            SELECT COUNT(*) AS cnt
            FROM task
            WHERE (? < 0 OR level_id <> ?)
            """;

        try (Connection c = Sqlite.open();
            PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setLong(1, prologueId);
        ps.setLong(2, prologueId);

        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("cnt") : 0;
        }
        } catch (SQLException e) {
        throw new RuntimeException("DB error in PlayerTaskResultRepository.countAllNonPrologueTasks", e);
        }
    }

    public int totalScoreNonPrologue(long playerId) {
        String sql = """
            SELECT
              COALESCE(SUM(
                MAX(100,
                  l.max_score
                  - (ptr.hints_used * 100)
                  - (CASE WHEN ptr.attempts > 1 THEN (ptr.attempts - 1) * 50 ELSE 0 END)
                )
              ), 0) AS total
            FROM player_task_result ptr
            JOIN task t ON t.id = ptr.task_id
            JOIN level l ON l.id = t.level_id
            WHERE ptr.player_id = ?
              AND ptr.completed = 1
              AND l.title <> 'Prolog'
            """;
    
        try (Connection c = Sqlite.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
          ps.setLong(1, playerId);
          try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
          }
        } catch (SQLException e) {
          throw new RuntimeException("DB error in PlayerTaskResultRepository.totalScoreNonPrologue", e);
        }
      }
}