package com.javaisland.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

final class SqliteConnectionTest {

  @AfterEach
  void cleanup() {
    System.clearProperty("javaisland.jdbcUrl");
  }

  @Test
  void opensConnectionAndCanSelectOne() throws Exception {
    String url = TestDb.createTempSqliteUrl();
    System.setProperty("javaisland.jdbcUrl", url);

    try (Connection c = Sqlite.open()) {
      assertNotNull(c);
      assertTrue(c.isValid(2));

      try (var st = c.createStatement();
           var rs = st.executeQuery("SELECT 1")) {
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
      }
    }
  }
}