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

  public int countCompleted(long playerId) {
    String sql = "SELECT COUNT(*) AS cnt FROM player_task_result WHERE player_id = ? AND completed = 1";
    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, playerId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt("cnt") : 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("DB error in PlayerTaskResultRepository.countCompleted", e);
    }
  }

  public int countAllTasks() {
    String sql = "SELECT COUNT(*) AS cnt FROM task";
    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      return rs.next() ? rs.getInt("cnt") : 0;
    } catch (SQLException e) {
      throw new RuntimeException("DB error in PlayerTaskResultRepository.countAllTasks", e);
    }
  }
}