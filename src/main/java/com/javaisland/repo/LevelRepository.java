package com.javaisland.repo;

import com.javaisland.db.Sqlite;
import com.javaisland.model.LevelDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class LevelRepository {

  public LevelDto findFirstLevel() {
    String sql = """
        SELECT id, code, title, order_index, intro_text, outro_text
        FROM level
        ORDER BY order_index ASC, id ASC
        LIMIT 1
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      if (!rs.next()) return null;
      return mapRow(rs);
    } catch (SQLException e) {
      throw new RuntimeException("DB error in LevelRepository.findFirstLevel", e);
    }
  }

  public LevelDto findNextLevel(int currentOrderIndex, long currentLevelId) {
    String sql = """
        SELECT id, code, title, order_index, intro_text, outro_text
        FROM level
        WHERE (order_index > ? OR (order_index = ? AND id > ?))
        ORDER BY order_index ASC, id ASC
        LIMIT 1
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setInt(1, currentOrderIndex);
      ps.setInt(2, currentOrderIndex);
      ps.setLong(3, currentLevelId);

      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return null;
        return mapRow(rs);
      }
    } catch (SQLException e) {
      throw new RuntimeException("DB error in LevelRepository.findNextLevel", e);
    }
  }

  public Optional<LevelDto> findById(long levelId) {
    String sql = """
        SELECT id, code, title, order_index, intro_text, outro_text
        FROM level
        WHERE id = ?
        LIMIT 1
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setLong(1, levelId);

      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return Optional.empty();
        return Optional.of(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("DB error in LevelRepository.findById", e);
    }
  }

  public Optional<LevelDto> findLastNonPrologueLevel() {
    String sql = """
        SELECT id, code, title, order_index, intro_text, outro_text
        FROM level
        WHERE title <> 'Prolog'
        ORDER BY order_index DESC, id DESC
        LIMIT 1
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      if (!rs.next()) return Optional.empty();
      return Optional.of(mapRow(rs));
    } catch (SQLException e) {
      throw new RuntimeException("DB error in LevelRepository.findLastNonPrologueLevel", e);
    }
  }

  private static LevelDto mapRow(ResultSet rs) throws SQLException {
    return new LevelDto(
        rs.getLong("id"),
        rs.getString("code"),
        rs.getString("title"),
        rs.getInt("order_index"),
        rs.getString("intro_text"),
        rs.getString("outro_text")
    );
  }
}