package com.javaisland.repo;

import com.javaisland.db.Sqlite;
import com.javaisland.model.TaskDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class TaskRepository {

  public TaskDto findFirstTaskOfLevel(long levelId) {
    String sql = """
        SELECT id, level_id, title, description, order_index, validation, code, story, success_text, capture_json, capture_mode, capture_params
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
        SELECT id, level_id, title, description, order_index, validation, code, story, success_text, capture_json, capture_mode, capture_params
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

  public Optional<TaskDto> findFirstUncompletedNonPrologue(long playerId) {
    String sql = """
        SELECT t.*
        FROM task t
        JOIN level l ON l.id = t.level_id
        LEFT JOIN player_task_result ptr
          ON ptr.task_id = t.id AND ptr.player_id = ?
        WHERE l.title <> 'Prolog'
          AND COALESCE(ptr.completed, 0) <> 1
        ORDER BY l.order_index ASC, t.order_index ASC, t.id ASC
        LIMIT 1
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, playerId);

      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return Optional.empty();
        return Optional.of(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("DB error in TaskRepository.findFirstUncompletedNonPrologue", e);
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
      rs.getString("code"),
      rs.getString("story"),
      rs.getString("success_text"),
      rs.getString("capture_json"),
      rs.getString("capture_mode"),
      rs.getString("capture_params")
    );
  }
}