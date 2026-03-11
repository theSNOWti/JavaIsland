package com.javaisland.capture;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TaskCaptureExtractor {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static TaskCapture parse(String captureJson) {
    if (captureJson == null || captureJson.isBlank()) return null;
    try {
      return MAPPER.readValue(captureJson, TaskCapture.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid capture_json: " + e.getMessage(), e);
    }
  }

  /**
   * Returns the value of capture group 1 from the LAST match of stdoutRegex.
   */
  public static String extractLastCapturedValue(String stdout, String stdoutRegex) {
    if (stdoutRegex == null || stdoutRegex.isBlank()) return null;

    String out = (stdout == null ? "" : stdout).replace("\r\n", "\n");
    Pattern p = Pattern.compile(stdoutRegex, Pattern.MULTILINE);
    Matcher m = p.matcher(out);

    String last = null;
    while (m.find()) {
      if (m.groupCount() >= 1) last = m.group(1);
    }
    return last;
  }

  private TaskCaptureExtractor() {}
}