package com.javaisland.repo;

import com.javaisland.db.Sqlite;
import com.javaisland.db.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

final class PlayerTaskResultRepositoryTest {

  @AfterEach
  void cleanup() {
    System.clearProperty("javaisland.jdbcUrl");
  }

  @Test
  void ensureRowExists_createsRowWithDefaults() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchema();

    PlayerTaskResultRepository repo = new PlayerTaskResultRepository();
    repo.ensureRowExists(1L, 10L);

    try (Connection c = Sqlite.open()) {
      var rs = c.createStatement().executeQuery("""
          SELECT completed, attempts, hints_used
          FROM player_task_result
          WHERE player_id = 1 AND task_id = 10
          """);
      assertTrue(rs.next());
      assertEquals(0, rs.getInt("completed"));
      assertEquals(0, rs.getInt("attempts"));
      assertEquals(0, rs.getInt("hints_used"));
    }
  }

  @Test
  void incrementAttempts_insertsThenIncrements() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchema();

    PlayerTaskResultRepository repo = new PlayerTaskResultRepository();

    repo.incrementAttempts(1L, 10L);
    repo.incrementAttempts(1L, 10L);

    try (Connection c = Sqlite.open()) {
      var rs = c.createStatement().executeQuery("""
          SELECT attempts
          FROM player_task_result
          WHERE player_id = 1 AND task_id = 10
          """);
      assertTrue(rs.next());
      assertEquals(2, rs.getInt("attempts"));
    }
  }

  @Test
  void setHintsUsed_neverDecreases_andClampsAtZero() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchema();

    PlayerTaskResultRepository repo = new PlayerTaskResultRepository();

    repo.setHintsUsed(1L, 10L, 2);
    repo.setHintsUsed(1L, 10L, 1);   // should not decrease
    repo.setHintsUsed(1L, 10L, -5);  // clamp to 0, but should not decrease from 2

    try (Connection c = Sqlite.open()) {
      var rs = c.createStatement().executeQuery("""
          SELECT hints_used
          FROM player_task_result
          WHERE player_id = 1 AND task_id = 10
          """);
      assertTrue(rs.next());
      assertEquals(2, rs.getInt("hints_used"));
    }
  }

  @Test
  void markCompleted_setsCompletedTo1() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchema();

    PlayerTaskResultRepository repo = new PlayerTaskResultRepository();

    repo.ensureRowExists(1L, 10L);
    repo.markCompleted(1L, 10L);

    try (Connection c = Sqlite.open()) {
      var rs = c.createStatement().executeQuery("""
          SELECT completed
          FROM player_task_result
          WHERE player_id = 1 AND task_id = 10
          """);
      assertTrue(rs.next());
      assertEquals(1, rs.getInt("completed"));
    }
  }

  @Test
  void countCompletedNonPrologue_ignoresTasksFromLevelTitledProlog() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchemaWithLevelsAndTasks();

    PlayerTaskResultRepository repo = new PlayerTaskResultRepository();

    // player completed both tasks, but one is prologue -> count should be 1
    repo.markCompleted(7L, 100L); // prologue task
    repo.markCompleted(7L, 200L); // non-prologue task

    assertEquals(1, repo.countCompletedNonPrologue(7L));
  }

  @Test
  void countAllNonPrologueTasks_ignoresTasksFromLevelTitledProlog() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchemaWithLevelsAndTasks();

    PlayerTaskResultRepository repo = new PlayerTaskResultRepository();
    assertEquals(1, repo.countAllNonPrologueTasks());
  }

  @Test
  void totalScoreNonPrologue_appliesHintsAndAttemptsPenalty_andMin100() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());
    initSchemaWithLevelsAndTasksAndMaxScore();

    PlayerTaskResultRepository repo = new PlayerTaskResultRepository();

    // For non-prologue task 200: max_score=500
    // hints_used=2 => -200
    // attempts=3 => -(3-1)*50 = -100
    // total = max(100, 500-200-100)=200
    repo.incrementAttempts(7L, 200L); // attempts 1
    repo.incrementAttempts(7L, 200L); // attempts 2
    repo.incrementAttempts(7L, 200L); // attempts 3
    repo.setHintsUsed(7L, 200L, 2);
    repo.markCompleted(7L, 200L);

    // prologue completion should not count
    repo.markCompleted(7L, 100L);

    assertEquals(200, repo.totalScoreNonPrologue(7L));
  }

  private static void initSchema() throws Exception {
    try (Connection c = Sqlite.open()) {
      c.createStatement().execute("""
          CREATE TABLE level (
            id INTEGER PRIMARY KEY,
            title TEXT NOT NULL,
            max_score INTEGER NOT NULL DEFAULT 500
          )
          """);

      c.createStatement().execute("""
          CREATE TABLE task (
            id INTEGER PRIMARY KEY,
            level_id INTEGER NOT NULL,
            FOREIGN KEY(level_id) REFERENCES level(id)
          )
          """);

      c.createStatement().execute("""
          CREATE TABLE player_task_result (
            player_id INTEGER NOT NULL,
            task_id INTEGER NOT NULL,
            completed INTEGER NOT NULL DEFAULT 0,
            attempts INTEGER NOT NULL DEFAULT 0,
            hints_used INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(player_id, task_id)
          )
          """);
    }
  }

  private static void initSchemaWithLevelsAndTasks() throws Exception {
    initSchema();
    try (Connection c = Sqlite.open()) {
      c.createStatement().executeUpdate("""
          INSERT INTO level(id, title, max_score) VALUES (1, 'Prolog', 500)
          """);
      c.createStatement().executeUpdate("""
          INSERT INTO level(id, title, max_score) VALUES (2, 'Level 2', 500)
          """);

      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id) VALUES (100, 1)
          """);
      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id) VALUES (200, 2)
          """);
    }
  }

  private static void initSchemaWithLevelsAndTasksAndMaxScore() throws Exception {
    initSchemaWithLevelsAndTasks();
    try (Connection c = Sqlite.open()) {
      // override max_score for level 2 for the score test
      c.createStatement().executeUpdate("""
          UPDATE level SET max_score = 500 WHERE id = 2
          """);
    }
  }
}