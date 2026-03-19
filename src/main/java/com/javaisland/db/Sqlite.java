package com.javaisland.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Sqlite {

  private static final String DEFAULT_JDBC_URL =
      "jdbc:sqlite:src/main/resources/com/javaisland/JavaIsland.db";

  private Sqlite() {}

  public static Connection open() throws SQLException {
    String url = System.getProperty("javaisland.jdbcUrl", DEFAULT_JDBC_URL);
    return DriverManager.getConnection(url);
  }
}