package com.javaisland.repo;

import com.javaisland.db.Sqlite;

import java.sql.*;
import java.util.OptionalLong;

public final class PlayerRepository {

    public long createPlayer() {
        return createPlayer("Player-" + System.currentTimeMillis());
      }
    
      public long createPlayer(String name) {
        String sql = """
            INSERT INTO player(name, created_at, last_played_at)
            VALUES(?, datetime('now'), datetime('now'))
            """;
    
        try (var c = Sqlite.open();
             var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
    
          ps.setString(1, name);
          ps.executeUpdate();
    
          try (var keys = ps.getGeneratedKeys()) {
            if (keys.next()) return keys.getLong(1);
          }
          throw new java.sql.SQLException("No generated key returned for player insert.");
    
        } catch (Exception e) {
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

  public void touchLastPlayed(long playerId) {
    String sql = "UPDATE player SET last_played_at = datetime('now') WHERE id = ?";
    try (var c = Sqlite.open(); var ps = c.prepareStatement(sql)) {
      ps.setLong(1, playerId);
      ps.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException("DB error in PlayerRepository.touchLastPlayed", e);
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