package com.javaisland.repo;

import com.javaisland.db.Sqlite;
import com.javaisland.db.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

final class PlayerVarRepositoryTest {

  @AfterEach
  void cleanup() {
    System.clearProperty("javaisland.jdbcUrl");
  }

  @Test
  void findValue_returnsNullWhenMissing() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchema();

    PlayerVarRepository repo = new PlayerVarRepository();
    assertNull(repo.findValue(1L, "coins"));
  }

  @Test
  void upsert_insertsNewRow() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchema();

    PlayerVarRepository repo = new PlayerVarRepository();
    repo.upsert(1L, "coins", "NUMBER", "10");

    assertEquals("10", repo.findValue(1L, "coins"));
  }

  @Test
  void upsert_updatesExistingRow() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchema();

    try (Connection c = Sqlite.open()) {
      c.createStatement().execute("""
          INSERT INTO player_var(player_id, var_name, type, value)
          VALUES (1, 'coins', 'NUMBER', '10')
          """);
    }

    PlayerVarRepository repo = new PlayerVarRepository();
    repo.upsert(1L, "coins", "NUMBER", "11");

    assertEquals("11", repo.findValue(1L, "coins"));
  }

  private static void initSchema() throws Exception {
    try (Connection c = Sqlite.open()) {
      TestDb.initSchema(c);
    }
  }
}