package com.javaisland.repo;

import com.javaisland.db.Sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class TutorialRepository {

  public record TutorialRow(long id, long levelId, int levelOrderIndex, String levelTitle, String text) {}

  /**
   * Returns tutorials for all levels with order_index <= currentLevelOrderIndex,
   * excluding the prologue (title='Prolog'), ordered by level order.
   */
  public List<TutorialRow> findTutorialsUpToLevel(int currentLevelOrderIndex) {
    String sql = """
        SELECT
          tu.id AS tutorial_id,
          tu.level_id AS level_id,
          l.order_index AS level_order_index,
          l.title AS level_title,
          tu.text AS tutorial_text
        FROM tutorial tu
        JOIN level l ON l.id = tu.level_id
        WHERE l.order_index <= ?
          AND l.title <> 'Prolog'
        ORDER BY l.order_index ASC, l.id ASC, tu.id ASC
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setInt(1, currentLevelOrderIndex);

      try (ResultSet rs = ps.executeQuery()) {
        List<TutorialRow> out = new ArrayList<>();
        while (rs.next()) {
          out.add(new TutorialRow(
              rs.getLong("tutorial_id"),
              rs.getLong("level_id"),
              rs.getInt("level_order_index"),
              rs.getString("level_title"),
              rs.getString("tutorial_text")
          ));
        }
        return out;
      }
    } catch (SQLException e) {
      throw new RuntimeException("DB error in TutorialRepository.findTutorialsUpToLevel", e);
    }
  }
}