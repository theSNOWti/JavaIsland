package com.javaisland.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaisland.run.JavaRunner;

public final class ValidationEngine {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public record ValidationResult(boolean ok, String message) {}

  public ValidationResult validate(String validationJson, JavaRunner.RunResult run) {
    if (validationJson == null || validationJson.isBlank()) {
      return new ValidationResult(false, "No validation configured for this task.");
    }

    try {
      JsonNode node = MAPPER.readTree(validationJson);

      if (node.isArray()) {
        for (JsonNode rule : node) {
          ValidationResult r = validateOne(rule, run);
          if (!r.ok) return r;
        }
        return new ValidationResult(true, "All rules passed.");
      }

      return validateOne(node, run);
    } catch (Exception e) {
      return new ValidationResult(false, "Invalid validation JSON: " + e.getMessage());
    }
  }

  private ValidationResult validateOne(JsonNode rule, JavaRunner.RunResult run) {
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
      default -> new ValidationResult(false, "Unknown validation type: " + type);
    };
  }
}