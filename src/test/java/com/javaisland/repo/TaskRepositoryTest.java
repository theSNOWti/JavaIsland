package com.javaisland.repo;

import com.javaisland.db.Sqlite;
import com.javaisland.db.TestDb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

final class TaskRepositoryTest {

  @AfterEach
  void cleanup() {
    System.clearProperty("javaisland.jdbcUrl");
  }

  @Test
  void findFirstTaskOfLevel_mapsBackgroundImage() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());

    try (Connection c = Sqlite.open()) {
      TestDb.initSchema(c);

      c.createStatement().executeUpdate("""
          INSERT INTO level(id, code, title, order_index, intro_text, outro_text)
          VALUES (1, 'L01', 'Prolog', 1, NULL, NULL)
          """);

      c.createStatement().executeUpdate("""
          INSERT INTO task(
            id, level_id, title, description, order_index, validation, code, story, success_text,
            capture_json, capture_mode, capture_params, background_image
          ) VALUES (
            101, 1, 'T1', 'desc', 1, 'v', 'code', 'story', 'ok',
            NULL, NULL, NULL, 'JavaIsland1.png'
          )
          """);
    }

    TaskRepository repo = new TaskRepository();
    var task = repo.findFirstTaskOfLevel(1L);

    assertNotNull(task);
    assertEquals(101L, task.id());
    assertEquals("JavaIsland1.png", task.backgroundImage());
  }

  @Test
  void findFirstTaskOfLevel_ordersByOrderIndexThenId() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());

    try (Connection c = Sqlite.open()) {
      TestDb.initSchema(c);

      c.createStatement().executeUpdate("""
          INSERT INTO level(id, code, title, order_index, intro_text, outro_text)
          VALUES (1, 'L01', 'Level 1', 1, NULL, NULL)
          """);

      // order_index 2 (not first)
      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (200, 1, 'T200', NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);

      // order_index 1, higher id (tie)
      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (101, 1, 'T101', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);

      // order_index 1, lower id (should win)
      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (100, 1, 'T100', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);
    }

    TaskRepository repo = new TaskRepository();
    var task = repo.findFirstTaskOfLevel(1L);

    assertNotNull(task);
    assertEquals(100L, task.id());
    assertEquals(1, task.orderIndex());
  }

  @Test
  void findNextTaskInLevel_movesToNextByOrderIndex() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());

    try (Connection c = Sqlite.open()) {
      TestDb.initSchema(c);

      c.createStatement().executeUpdate("""
          INSERT INTO level(id, code, title, order_index, intro_text, outro_text)
          VALUES (1, 'L01', 'Level 1', 1, NULL, NULL)
          """);

      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (10, 1, 'T10', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);

      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (20, 1, 'T20', NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);
    }

    TaskRepository repo = new TaskRepository();

    // if your method signature differs, tell me and I'll adjust:
    var next = repo.findNextTaskInLevel(1L, 1, 10L);

    assertNotNull(next);
    assertEquals(20L, next.id());
  }

  @Test
  void findNextTaskInLevel_usesIdTieBreakerWithinSameOrderIndex() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());

    try (Connection c = Sqlite.open()) {
      TestDb.initSchema(c);

      c.createStatement().executeUpdate("""
          INSERT INTO level(id, code, title, order_index, intro_text, outro_text)
          VALUES (1, 'L01', 'Level 1', 1, NULL, NULL)
          """);

      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (10, 1, 'T10', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);

      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (11, 1, 'T11', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);
    }

    TaskRepository repo = new TaskRepository();
    var next = repo.findNextTaskInLevel(1L, 1, 10L);

    assertNotNull(next);
    assertEquals(11L, next.id());
  }

  @Test
  void findFirstUncompletedNonPrologue_skipsPrologueAndCompletedTasks() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());

    try (Connection c = Sqlite.open()) {
      TestDb.initSchema(c);

      // Prologue level
      c.createStatement().executeUpdate("""
          INSERT INTO level(id, code, title, order_index, intro_text, outro_text)
          VALUES (1, 'P', 'Prolog', 1, NULL, NULL)
          """);

      // Non-prologue level
      c.createStatement().executeUpdate("""
          INSERT INTO level(id, code, title, order_index, intro_text, outro_text)
          VALUES (2, 'L02', 'Level 2', 2, NULL, NULL)
          """);

      // Prologue task (ignored)
      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (10, 1, 'PT', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);

      // Level 2 tasks
      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (20, 2, 'T20', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);
      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (21, 2, 'T21', NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);

      // Mark T20 completed for player 99
      c.createStatement().executeUpdate("""
          INSERT INTO player_task_result(player_id, task_id, completed)
          VALUES (99, 20, 1)
          """);
    }

    TaskRepository repo = new TaskRepository();
    var opt = repo.findFirstUncompletedNonPrologue(99L);

    assertTrue(opt.isPresent());
    assertEquals(21L, opt.get().id());
  }

  @Test
  void happyPath_findFirstUncompletedNonPrologue_thenComplete_thenNextTaskThenNextLevel() throws Exception {
    System.setProperty("javaisland.jdbcUrl", TestDb.createTempSqliteUrl());

    long playerId = 42L;

    try (Connection c = Sqlite.open()) {
      TestDb.initSchema(c);

      // Levels
      c.createStatement().executeUpdate("""
          INSERT INTO level(id, code, title, order_index, intro_text, outro_text)
          VALUES (1, 'P', 'Prolog', 1, NULL, NULL)
          """);
      c.createStatement().executeUpdate("""
          INSERT INTO level(id, code, title, order_index, intro_text, outro_text)
          VALUES (2, 'L02', 'Level 2', 2, NULL, NULL)
          """);
      c.createStatement().executeUpdate("""
          INSERT INTO level(id, code, title, order_index, intro_text, outro_text)
          VALUES (3, 'L03', 'Level 3', 3, NULL, NULL)
          """);

      // Prolog task (must be ignored)
      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (10, 1, 'PT', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);

      // Level 2 tasks
      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (20, 2, 'L2-T1', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);
      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (21, 2, 'L2-T2', NULL, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);

      // Level 3 first task
      c.createStatement().executeUpdate("""
          INSERT INTO task(id, level_id, title, description, order_index, validation, code, story, success_text,
                           capture_json, capture_mode, capture_params, background_image)
          VALUES (30, 3, 'L3-T1', NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
          """);
    }

    TaskRepository repo = new TaskRepository();

    // 1) first uncompleted is Level2 Task20
    var first = repo.findFirstUncompletedNonPrologue(playerId);
    assertTrue(first.isPresent());
    assertEquals(20L, first.get().id());

    // 2) complete Task20
    try (Connection c = Sqlite.open()) {
      c.createStatement().executeUpdate("""
          INSERT INTO player_task_result(player_id, task_id, completed)
          VALUES (42, 20, 1)
          """);
    }

    var second = repo.findFirstUncompletedNonPrologue(playerId);
    assertTrue(second.isPresent());
    assertEquals(21L, second.get().id());

    // 3) complete Task21 => should move to Level3 Task30
    try (Connection c = Sqlite.open()) {
      c.createStatement().executeUpdate("""
          INSERT INTO player_task_result(player_id, task_id, completed)
          VALUES (42, 21, 1)
          """);
    }

    var third = repo.findFirstUncompletedNonPrologue(playerId);
    assertTrue(third.isPresent());
    assertEquals(30L, third.get().id());
  }
}