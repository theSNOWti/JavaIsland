package com.javaisland.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaisland.run.JavaRunner;

import java.util.regex.Pattern;

public final class ValidationEngine {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public record ValidationResult(boolean ok, String message) {}

  public ValidationResult validate(String validationJson, JavaRunner.RunResult run, String sourceCode) {
    if (validationJson == null || validationJson.isBlank()) {
      return new ValidationResult(false, "No validation configured for this task.");
    }

    try {
      JsonNode node = MAPPER.readTree(validationJson);

      if (node.isArray()) {
        for (JsonNode rule : node) {
          ValidationResult r = validateOne(rule, run, sourceCode);
          if (!r.ok) return r;
        }
        return new ValidationResult(true, "All rules passed.");
      }

      return validateOne(node, run, sourceCode);
    } catch (Exception e) {
      return new ValidationResult(false, "Invalid validation JSON: " + e.getMessage());
    }
  }

  // Backwards-compatible overload (old callers)
  public ValidationResult validate(String validationJson, JavaRunner.RunResult run) {
    return validate(validationJson, run, null);
  }

  private ValidationResult validateOne(JsonNode rule, JavaRunner.RunResult run, String sourceCode) {
    String type = rule.path("type").asText("");

    return switch (type) {
      case "RUN_STDOUT_CONTAINS" -> {
        String v = rule.path("value").asText();
        String out = run.stdout() == null ? "" : run.stdout();
        yield out.contains(v)
            ? new ValidationResult(true, "stdout contains expected text.")
            : new ValidationResult(false, "Expected stdout to contain: " + v);
      }
      case "RUN_STDOUT_EQUALS" -> {
        String v = rule.path("value").asText();
        String out = (run.stdout() == null ? "" : run.stdout()).replace("\r\n", "\n").trim();
        yield out.equals(v.trim())
            ? new ValidationResult(true, "stdout matches expected output.")
            : new ValidationResult(false, "Expected stdout == '" + v + "' but was '" + out + "'");
      }
      case "RUN_EXIT_CODE_IS" -> {
        int v = rule.path("value").asInt(0);
        yield run.exitCode() == v
            ? new ValidationResult(true, "Exit code ok.")
            : new ValidationResult(false, "Expected exit code " + v + " but got " + run.exitCode());
      }

      // --------------------
      // NEW: source code validations
      // --------------------
      case "SRC_CONTAINS" -> {
        String v = rule.path("value").asText();
        String src = normalizedSource(sourceCode, rule);
        yield src.contains(v)
            ? new ValidationResult(true, "source contains expected text.")
            : new ValidationResult(false, "Expected source to contain: " + v);
      }
      case "SRC_REGEX" -> {
        String pattern = rule.path("pattern").asText();
        String src = normalizedSource(sourceCode, rule);

        if (pattern == null || pattern.isBlank()) {
          yield new ValidationResult(false, "SRC_REGEX missing 'pattern'.");
        }

        Pattern p;
        try {
          // DOTALL by default makes it easier to match across lines.
          p = Pattern.compile(pattern, Pattern.DOTALL);
        } catch (Exception e) {
          yield new ValidationResult(false, "Invalid SRC_REGEX pattern: " + e.getMessage());
        }

        yield p.matcher(src).find()
            ? new ValidationResult(true, "source matches expected pattern.")
            : new ValidationResult(false, "Expected source to match regex: " + pattern);
      }
      case "RUN_STDOUT_REGEX" -> {
        String pattern = rule.path("pattern").asText();
        String out = (run.stdout() == null ? "" : run.stdout()).replace("\r\n", "\n");
      
        if (pattern == null || pattern.isBlank()) {
          yield new ValidationResult(false, "RUN_STDOUT_REGEX missing 'pattern'.");
        }
      
        Pattern p;
        try {
          p = Pattern.compile(pattern, Pattern.DOTALL | Pattern.MULTILINE);
        } catch (Exception e) {
          yield new ValidationResult(false, "Invalid RUN_STDOUT_REGEX pattern: " + e.getMessage());
        }
      
        yield p.matcher(out).find()
            ? new ValidationResult(true, "stdout matches expected pattern.")
            : new ValidationResult(false, "Expected stdout to match regex: " + pattern);
      }

      default -> new ValidationResult(false, "Unknown validation type: " + type);
    };
  }

  private static String normalizedSource(String sourceCode, JsonNode rule) {
    String src = sourceCode == null ? "" : sourceCode;

    boolean stripComments = rule.path("stripComments").asBoolean(false);
    if (stripComments) {
      src = stripJavaComments(src);
    }

    // Normalize line endings for more stable regexes
    return src.replace("\r\n", "\n");
  }

  private static String stripJavaComments(String src) {
    // Remove block comments /* ... */
    src = src.replaceAll("(?s)/\\*.*?\\*/", " ");
    // Remove line comments // ...
    src = src.replaceAll("(?m)//.*?$", " ");
    return src;
  }
}