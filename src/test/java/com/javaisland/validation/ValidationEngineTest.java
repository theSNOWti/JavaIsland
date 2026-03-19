package com.javaisland.validation;

import com.javaisland.run.JavaRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ValidationEngineTest {

  private final ValidationEngine engine = new ValidationEngine();

  @Test
  void validate_blankJson_fails() {
    var res = engine.validate("   ", run(0, "", ""), "class X{}");
    assertFalse(res.ok());
    assertTrue(res.message().toLowerCase().contains("no validation"), res.message());
  }

  @Test
  void runStdoutContains_passesAndFails() {
    String json = """
        {"type":"RUN_STDOUT_CONTAINS","value":"Hello"}
        """;

    assertTrue(engine.validate(json, run(0, "Hello World", ""), null).ok());
    assertFalse(engine.validate(json, run(0, "Nope", ""), null).ok());
  }

  @Test
  void runStdoutEquals_trimsAndNormalizesNewlines() {
    String json = """
        {"type":"RUN_STDOUT_EQUALS","value":"A\\nB"}
        """;

    assertTrue(engine.validate(json, run(0, "A\r\nB\r\n", ""), null).ok());
  }

  @Test
  void srcRegex_stripComments_true_ignoresCommentedCode() {
    String json = """
        {"type":"SRC_NOT_REGEX","pattern":"System\\\\.out\\\\.println","stripComments":true}
        """;

    String src = """
        public class X {
          // System.out.println("nope");
          /* System.out.println("nope2"); */
          void ok() {}
        }
        """;

    var res = engine.validate(json, run(0, "", ""), src);
    assertTrue(res.ok(), res.message());
  }

  @Test
  void arrayOfRules_shortCircuitsOnFirstFailure() {
    String json = """
        [
          {"type":"RUN_EXIT_CODE_IS","value":0},
          {"type":"RUN_STDOUT_CONTAINS","value":"OK"}
        ]
        """;

    var res = engine.validate(json, run(0, "no", ""), null);
    assertFalse(res.ok());
    assertTrue(res.message().contains("Expected stdout"), res.message());
  }

  @Test
  void invalidJson_returnsHelpfulMessage() {
    var res = engine.validate("{not-json}", run(0, "", ""), null);
    assertFalse(res.ok());
    assertTrue(res.message().startsWith("Invalid validation JSON:"), res.message());
  }

  private static JavaRunner.RunResult run(int exitCode, String stdout, String stderr) {
    return new JavaRunner.RunResult(true, exitCode, stdout, stderr, "test");
  }
}