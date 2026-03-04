package com.javaisland.repo;

import com.javaisland.db.Sqlite;
import com.javaisland.model.TaskDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class TaskRepository {

  public TaskDto findFirstTaskOfLevel(long levelId) {
    String sql = """
        SELECT id, level_id, title, description, order_index, validation, code
        FROM task
        WHERE level_id = ?
        ORDER BY order_index ASC, id ASC
        LIMIT 1
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setLong(1, levelId);

      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return null;

        return mapRow(rs);
      }
    } catch (SQLException e) {
      throw new RuntimeException("DB error in TaskRepository.findFirstTaskOfLevel", e);
    }
  }

  public TaskDto findNextTaskInLevel(long levelId, int currentOrderIndex, long currentTaskId) {
    String sql = """
        SELECT id, level_id, title, description, order_index, validation, code
        FROM task
        WHERE level_id = ?
          AND (order_index > ? OR (order_index = ? AND id > ?))
        ORDER BY order_index ASC, id ASC
        LIMIT 1
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setLong(1, levelId);
      ps.setInt(2, currentOrderIndex);
      ps.setInt(3, currentOrderIndex);
      ps.setLong(4, currentTaskId);

      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return null;

        return mapRow(rs);
      }
    } catch (SQLException e) {
      throw new RuntimeException("DB error in TaskRepository.findNextTaskInLevel", e);
    }
  }

  private static TaskDto mapRow(ResultSet rs) throws SQLException {
    return new TaskDto(
        rs.getLong("id"),
        rs.getLong("level_id"),
        rs.getString("title"),
        rs.getString("description"),
        rs.getInt("order_index"),
        rs.getString("validation"),
        rs.getString("code") // <-- comes from DB column "code"
    );
  }
}