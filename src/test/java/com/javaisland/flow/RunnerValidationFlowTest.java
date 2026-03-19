package com.javaisland.flow;

import com.javaisland.run.JavaRunner;
import com.javaisland.validation.ValidationEngine;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

final class RunnerValidationFlowTest {

  @Test
  void compileRunThenValidate_stdoutContains_ok() {
    String userCode = """
        public class Main {
          public static void main(String[] args) {
            System.out.println("OK");
          }
        }
        """;

    JavaRunner.RunResult run = JavaRunner.compileAndRunMain(userCode, Duration.ofSeconds(2));
    assertTrue(run.compiled(), "expected compiled=true, got: " + run);
    assertEquals(0, run.exitCode(), "stderr:\n" + run.stderr());

    String validationJson = """
        {"type":"RUN_STDOUT_CONTAINS","value":"OK"}
        """;

    var res = new ValidationEngine().validate(validationJson, run, userCode);

    assertTrue(res.ok(), res.message());
  }

  @Test
  void compileRunThenValidate_stdoutContains_fails() {
    String userCode = """
        public class Main {
          public static void main(String[] args) {
            System.out.println("NO");
          }
        }
        """;

    JavaRunner.RunResult run = JavaRunner.compileAndRunMain(userCode, Duration.ofSeconds(2));
    assertTrue(run.compiled(), "expected compiled=true, got: " + run);
    assertEquals(0, run.exitCode(), "stderr:\n" + run.stderr());

    String validationJson = """
        {"type":"RUN_STDOUT_CONTAINS","value":"OK"}
        """;

    var res = new ValidationEngine().validate(validationJson, run, userCode);

    assertFalse(res.ok());
  }
}