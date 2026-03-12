package com.javaisland.repo;

import com.javaisland.db.Sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class PlayerTaskResultRepository {

  public void markCompleted(long playerId, long taskId) {
    String sql = """
        INSERT INTO player_task_result(player_id, task_id, completed)
        VALUES(?, ?, 1)
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
}