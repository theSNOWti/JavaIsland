package com.javaisland.run;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

final class JavaRunnerTest {

  @Test
  void compileAndRunMain_success_returnsExitCode0_andStdout() {
    String code = """
        public class Main {
          public static void main(String[] args) {
            System.out.println("Hello");
          }
        }
        """;

    JavaRunner.RunResult r = JavaRunner.compileAndRunMain(code, Duration.ofSeconds(2));

    assertTrue(r.compiled(), "expected compiled=true but got: " + r);
    assertEquals(0, r.exitCode(), "exitCode: " + r.exitCode() + " stderr:\n" + r.stderr());
    assertNotNull(r.message());
    assertFalse(r.message().isBlank(), "message should be non-blank");
    assertTrue(r.stdout().contains("Hello"), "stdout was:\n" + r.stdout());
  }

  @Test
  void compileAndRunMain_compileError_returnsCompiledFalse_andHasDiagnostics() {
    // Missing semicolon -> compile error
    String code = """
        public class Main {
          public static void main(String[] args) {
            System.out.println("Hello")
          }
        }
        """;

    JavaRunner.RunResult r = JavaRunner.compileAndRunMain(code, Duration.ofSeconds(2));

    assertFalse(r.compiled(), "expected compiled=false but got: " + r);
    assertEquals(-1, r.exitCode());
    assertNotNull(r.message());
    assertFalse(r.message().isBlank());
    assertTrue(r.message().toLowerCase().contains("compilation"), "message was: " + r.message());
    assertNotNull(r.stderr());
    assertTrue(r.stderr().contains("ERROR"), "stderr was:\n" + r.stderr());
    assertTrue(r.stderr().contains("Main.java"), "stderr was:\n" + r.stderr());
  }

  @Test
  void compileAndRunMain_timeout_returnsTimedOutMessage_andExitCodeMinus1() {
    String code = """
        public class Main {
          public static void main(String[] args) {
            while (true) { }
          }
        }
        """;

    JavaRunner.RunResult r = JavaRunner.compileAndRunMain(code, Duration.ofMillis(150));

    assertTrue(r.compiled(), "expected compiled=true but got: " + r);
    assertEquals(-1, r.exitCode());
    assertNotNull(r.message());
    assertTrue(r.message().toLowerCase().contains("timed out"), "message was: " + r.message());
  }
}