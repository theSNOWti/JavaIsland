package com.javaisland.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Sqlite {

  // If your DB file sits in the project root: JavaIsland.db
  // Otherwise use absolute path: jdbc:sqlite:/Users/marco/JavaIsland/path/to/JavaIsland.db
  private static final String JDBC_URL = "jdbc:sqlite:src/main/resources/com/javaisland/JavaIsland.db";

  private Sqlite() {}

  public static Connection open() throws SQLException {
    return DriverManager.getConnection(JDBC_URL);
  }
}