package com.javaisland.repo;

import com.javaisland.db.Sqlite;
import com.javaisland.model.LevelDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class LevelRepository {

  public LevelDto findFirstLevel() {
    String sql = """
        SELECT id, code, title, order_index
        FROM level
        ORDER BY order_index ASC, id ASC
        LIMIT 1
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      if (!rs.next()) return null;

      return new LevelDto(
          rs.getLong("id"),
          rs.getString("code"),
          rs.getString("title"),
          rs.getInt("order_index")
      );
    } catch (SQLException e) {
      throw new RuntimeException("DB error in LevelRepository.findFirstLevel", e);
    }
  }
}