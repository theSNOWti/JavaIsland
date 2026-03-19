package com.javaisland.capture;

import com.javaisland.db.Sqlite;
import com.javaisland.db.TestDb;
import com.javaisland.repo.PlayerVarRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

final class CaptureModeEvaluatorTest {

  @AfterEach
  void cleanup() {
    System.clearProperty("javaisland.jdbcUrl");
  }

  @Test
  void deltaAdd_correctValue_persists() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchema();

    // base value = 1.5
    new PlayerVarRepository().upsert(1L, "energy", "NUMBER", "1.5");

    var decision = CaptureModeEvaluator.evaluate(
        "DELTA_ADD",
        "{\"delta\":0.2,\"baseKey\":\"energy\"}",
        new TaskCapture("energy", "NUMBER", "energy=(\\d+(?:\\.\\d+)?)"),
        "1.7",
        1L,
        new PlayerVarRepository()
    );

    assertTrue(decision.shouldPersist(), decision.errorMessage());
    assertNull(decision.errorMessage());
    assertEquals("1.7", decision.finalValueText());
  }

  @Test
  void deltaAdd_missingBaseValue_returnsError() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchema();

    var decision = CaptureModeEvaluator.evaluate(
        "DELTA_ADD",
        "{\"delta\":2,\"baseKey\":\"coins\"}",
        new TaskCapture("coins", "NUMBER", "coins=(\\d+)"),
        "7",
        1L,
        new PlayerVarRepository()
    );

    assertFalse(decision.shouldPersist());
    assertEquals("No stored base value for coins", decision.errorMessage());
    assertNull(decision.finalValueText());
  }

  @Test
  void deltaAdd_wrongExpectedValue_returnsError() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchema();

    // base value = 5
    new PlayerVarRepository().upsert(1L, "coins", "NUMBER", "5");

    // expected 7 but got 8
    var decision = CaptureModeEvaluator.evaluate(
        "DELTA_ADD",
        "{\"delta\":2,\"baseKey\":\"coins\"}",
        new TaskCapture("coins", "NUMBER", "coins=(\\d+)"),
        "8",
        1L,
        new PlayerVarRepository()
    );

    assertFalse(decision.shouldPersist());
    assertNotNull(decision.errorMessage());
    assertTrue(decision.errorMessage().contains("Expected"), decision.errorMessage());
    assertNull(decision.finalValueText());
  }

  private static void initSchema() throws Exception {
    try (Connection c = Sqlite.open()) {
      TestDb.initSchema(c);
    }
  }
}