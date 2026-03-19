package com.javaisland.repo;

import com.javaisland.db.Sqlite;
import com.javaisland.db.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

final class LevelRepositoryTest {

  @AfterEach
  void cleanup() {
    System.clearProperty("javaisland.jdbcUrl");
  }

  @Test
  void findFirstLevel_returnsSmallestOrderIndex() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());

    try (Connection c = Sqlite.open()) {
      TestDb.initSchema(c);

      try (var ps = c.prepareStatement("""
          INSERT INTO level(id, code, title, order_index, intro_text, outro_text)
          VALUES (?, ?, ?, ?, ?, ?)
          """)) {
        ps.setLong(1, 10);
        ps.setString(2, "L10");
        ps.setString(3, "Later");
        ps.setInt(4, 2);
        ps.setString(5, null);
        ps.setString(6, null);
        ps.executeUpdate();

        ps.setLong(1, 1);
        ps.setString(2, "L01");
        ps.setString(3, "First");
        ps.setInt(4, 1);
        ps.setString(5, "intro");
        ps.setString(6, "outro");
        ps.executeUpdate();
      }
    }

    LevelRepository repo = new LevelRepository();
    var lvl = repo.findFirstLevel();

    assertNotNull(lvl);
    assertEquals(1L, lvl.id());
    assertEquals("First", lvl.title());
    assertEquals(1, lvl.orderIndex());
  }
}