package com.javaisland.capture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaisland.repo.PlayerVarRepository;

public final class CaptureModeEvaluator {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public record PersistDecision(boolean shouldPersist, String errorMessage, String finalValueText) {}

  public static PersistDecision evaluate(
      String mode,
      String paramsJson,
      TaskCapture cap,
      String extractedValueText,
      long playerId,
      PlayerVarRepository playerVarRepo
  ) {
    String m = (mode == null || mode.isBlank()) ? "ABSOLUTE" : mode.trim().toUpperCase();

    return switch (m) {
      case "ABSOLUTE" -> new PersistDecision(true, null, extractedValueText);

      case "DELTA_ADD" -> evalDeltaAdd(paramsJson, cap, extractedValueText, playerId, playerVarRepo);

      default -> new PersistDecision(false, "Unknown capture_mode: " + mode, null);
    };
  }

  private static PersistDecision evalDeltaAdd(
      String paramsJson,
      TaskCapture cap,
      String extractedValueText,
      long playerId,
      PlayerVarRepository playerVarRepo
  ) {
    JsonNode p;
    try {
      p = (paramsJson == null || paramsJson.isBlank()) ? null : MAPPER.readTree(paramsJson);
    } catch (Exception e) {
      return new PersistDecision(false, "Invalid capture_params JSON: " + e.getMessage(), null);
    }

    String baseKey = p != null ? p.path("baseKey").asText(null) : null;
    if (baseKey == null || baseKey.isBlank()) {
      // Default: baseKey = same as stored key
      baseKey = cap.key();
    }

    double delta = p != null ? p.path("delta").asDouble(Double.NaN) : Double.NaN;
    if (Double.isNaN(delta)) {
      return new PersistDecision(false, "capture_params.delta missing for DELTA_ADD", null);
    }

    String oldText = playerVarRepo.findValue(playerId, baseKey);
    if (oldText == null) {
      return new PersistDecision(false, "No stored base value for " + baseKey, null);
    }

    Double oldVal = tryParseNumber(oldText);
    Double newVal = tryParseNumber(extractedValueText);
    if (oldVal == null || newVal == null) {
      return new PersistDecision(false, "Could not parse numeric values for DELTA_ADD", null);
    }

    double expected = oldVal + delta;
    if (!numbersEqual(newVal, expected)) {
      return new PersistDecision(false, "Expected " + baseKey + " + " + delta + " = " + expected + " but got " + newVal, null);
    }

    // Persist the extracted value as-is (or normalized)
    return new PersistDecision(true, null, extractedValueText);
  }

  private static Double tryParseNumber(String s) {
    if (s == null) return null;
    String t = s.trim();
    if (t.isEmpty()) return null;
    try {
      return Double.parseDouble(t);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static boolean numbersEqual(double a, double b) {
    // exact for ints, tolerant for doubles
    return Math.abs(a - b) < 1e-9;
  }

  private CaptureModeEvaluator() {}
}