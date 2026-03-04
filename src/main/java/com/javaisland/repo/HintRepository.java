package com.javaisland.repo;

import com.javaisland.db.Sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class HintRepository {

  public List<String> findHintsForTask(long taskId) {
    String sql = """
        SELECT description
        FROM hint
        WHERE task_id = ?
        ORDER BY order_index ASC, id ASC
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setLong(1, taskId);

      try (ResultSet rs = ps.executeQuery()) {
        List<String> out = new ArrayList<>();
        while (rs.next()) {
          out.add(rs.getString("description"));
        }
        return out;
      }
    } catch (SQLException e) {
      throw new RuntimeException("DB error in HintRepository.findHintsForTask", e);
    }
  }
}