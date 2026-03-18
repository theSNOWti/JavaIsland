package com.javaisland.repo;

import com.javaisland.db.Sqlite;

import java.sql.*;
import java.util.OptionalLong;

public final class PlayerRepository {

  /**
   * Create a new player row and return its id.
   * Player gets its name later (Prolog Task 2).
   */
  public long createPlayer() {
    String sql = """
        INSERT INTO player(name, created_at, last_played_at)
        VALUES(?, strftime('%s','now'), strftime('%s','now'))
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      ps.setString(1, "");
      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No generated key returned for player insert");
        return keys.getLong(1);
      }
    } catch (SQLException e) {
      throw new RuntimeException("DB error in PlayerRepository.createPlayer", e);
    }
  }

  public void updatePlayerName(long playerId, String name) {
    String sql = """
        UPDATE player
        SET name = ?,
            last_played_at = strftime('%s','now')
        WHERE id = ?
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setString(1, name);
      ps.setLong(2, playerId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("DB error in PlayerRepository.updatePlayerName", e);
    }
  }

  public OptionalLong findLastPlayerId() {
    String sql = "SELECT id FROM player ORDER BY id DESC LIMIT 1";
    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      if (rs.next()) return OptionalLong.of(rs.getLong(1));
      return OptionalLong.empty();

    } catch (SQLException e) {
      throw new RuntimeException("DB error in PlayerRepository.findLastPlayerId", e);
    }
  }
}