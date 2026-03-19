package com.javaisland.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class StatusTextFormatterTest {

  @Test
  void compileError_extractsLineNumberAndMessage() {
    String stderr = """
        Main.java:3: error: incompatible types: String cannot be converted to int
          int trees = "34";
                      ^
        1 error
        """;

    String msg = StatusTextFormatter.formatCompileError(stderr);

    assertTrue(msg.startsWith("Zeile 3:"), msg);
    assertTrue(msg.contains("incompatible types"), msg);
    assertTrue(msg.contains("int trees = \"34\";"), msg);
    assertTrue(msg.contains("^"), msg);
  }

  @Test
  void compileError_supportsErrorPrefixWithoutFilename() {
    String stderr = "error: cannot find symbol\n";
    String msg = StatusTextFormatter.formatCompileError(stderr);
    assertEquals("cannot find symbol", msg);
  }

  @Test
  void compileError_fallbackToFirstNonBlankLines() {
    String stderr = "\n\nsomething odd\n\nsecond line\nthird line\nfourth line\n";
    String msg = StatusTextFormatter.formatCompileError(stderr);
    assertEquals("something odd\nsecond line\nthird line", msg);
  }

  @Test
  void validationMessage_regexExpectedIsUserFriendly() {
    String raw = "regex expected: ^Hello$";
    String msg = StatusTextFormatter.formatValidationMessage(raw);
    assertTrue(msg.toLowerCase().contains("ausgabe"), msg);
  }
}