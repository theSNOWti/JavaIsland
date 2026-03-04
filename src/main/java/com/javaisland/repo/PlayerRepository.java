package com.javaisland.repo;

import com.javaisland.db.Sqlite;

import java.sql.*;

public final class PlayerRepository {

  /**
   * Returns the "current" player:
   * - if a player exists: the most recently played (fallback: newest created)
   * - else: creates a new player row and returns its id
   */
  public long ensureCurrentPlayerId() {
    Long id = findMostRecentPlayerId();
    if (id != null) return id;
    return createPlayer();
  }

  public void updatePlayerName(long playerId, String name) {
    String sql = """
        UPDATE player
        SET name = ?,
            last_played_at = CURRENT_TIMESTAMP
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

  private Long findMostRecentPlayerId() {
    String sql = """
        SELECT id
        FROM player
        ORDER BY
          COALESCE(last_played_at, created_at) DESC,
          id DESC
        LIMIT 1
        """;
    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      if (!rs.next()) return null;
      return rs.getLong("id");
    } catch (SQLException e) {
      throw new RuntimeException("DB error in PlayerRepository.findMostRecentPlayerId", e);
    }
  }

  private long createPlayer() {
    String sql = """
        INSERT INTO player(name, created_at, last_played_at)
        VALUES(?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      ps.setString(1, ""); // start empty
      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No generated key returned for player insert");
        return keys.getLong(1);
      }
    } catch (SQLException e) {
      throw new RuntimeException("DB error in PlayerRepository.createPlayer", e);
    }
  }
}