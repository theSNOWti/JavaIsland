package com.javaisland.repo;

import com.javaisland.db.Sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class PlayerVarRepository {

  /**
   * Upsert into player_var with composite primary key (player_id, var_name).
   *
   * Schema:
   *   player_var(player_id PK, var_name PK, type, value)
   */
  public void upsert(long playerId, String varName, String type, String value) {
    String sql = """
        INSERT INTO player_var(player_id, var_name, type, value)
        VALUES(?, ?, ?, ?)
        ON CONFLICT(player_id, var_name)
        DO UPDATE SET
          type = excluded.type,
          value = excluded.value
        """;

    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, playerId);
      ps.setString(2, varName);
      ps.setString(3, type);
      ps.setString(4, value);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("DB error in PlayerVarRepository.upsert", e);
    }
  }

  public String findValue(long playerId, String varName) {
    String sql = "SELECT value FROM player_var WHERE player_id = ? AND var_name = ? LIMIT 1";
    try (Connection c = Sqlite.open();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, playerId);
      ps.setString(2, varName);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return null;
        return rs.getString("value");
      }
    } catch (SQLException e) {
      throw new RuntimeException("DB error in PlayerVarRepository.findValue", e);
    }
  }
}