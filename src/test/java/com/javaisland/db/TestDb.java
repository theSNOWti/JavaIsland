package com.javaisland.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

public final class TestDb {
  private TestDb() {}

  public static String createTempSqliteUrl() throws Exception {
    Path file = Files.createTempFile("javaisland-test-", ".db");
    file.toFile().deleteOnExit();
    return "jdbc:sqlite:" + file.toAbsolutePath();
  }

  public static void initSchema(Connection c) throws Exception {
    try (Statement st = c.createStatement()) {
      st.execute("""
          CREATE TABLE level (
            id INTEGER PRIMARY KEY,
            code TEXT NOT NULL,
            title TEXT NOT NULL,
            order_index INTEGER NOT NULL,
            intro_text TEXT,
            outro_text TEXT
          )
          """);

      st.execute("""
          CREATE TABLE task (
            id INTEGER PRIMARY KEY,
            level_id INTEGER NOT NULL,
            title TEXT NOT NULL,
            description TEXT,
            order_index INTEGER NOT NULL,
            validation TEXT,
            code TEXT,
            story TEXT,
            success_text TEXT,
            capture_json TEXT,
            capture_mode TEXT,
            capture_params TEXT,
            background_image TEXT,
            FOREIGN KEY(level_id) REFERENCES level(id)
          )
          """);

      st.execute("""
          CREATE TABLE player_task_result (
            player_id INTEGER NOT NULL,
            task_id INTEGER NOT NULL,
            completed INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(player_id, task_id)
          )
          """);

      // Used by PlayerVarRepository + CaptureModeEvaluator tests
      st.execute("""
          CREATE TABLE player_var (
            player_id INTEGER NOT NULL,
            var_name TEXT NOT NULL,
            type TEXT NOT NULL,
            value TEXT,
            PRIMARY KEY(player_id, var_name)
          )
          """);
    }
  }
}