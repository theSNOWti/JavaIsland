package com.javaisland.capture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class TaskCaptureExtractorTest {

  @Test
  void parse_nullOrBlank_returnsNull() {
    assertNull(TaskCaptureExtractor.parse(null));
    assertNull(TaskCaptureExtractor.parse(""));
    assertNull(TaskCaptureExtractor.parse("   "));
  }

  @Test
  void parse_validJson_parsesRecord() {
    String json = """
        {"key":"trees","type":"NUMBER","stdoutRegex":"trees=(\\\\d+)"}
        """;

    TaskCapture cap = TaskCaptureExtractor.parse(json);

    assertNotNull(cap);
    assertEquals("trees", cap.key());
    assertEquals("NUMBER", cap.type());
    assertEquals("trees=(\\d+)", cap.stdoutRegex());
  }

  @Test
  void parse_invalidJson_throwsIllegalArgumentExceptionWithHint() {
    String json = "{not-json}";

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> TaskCaptureExtractor.parse(json));

    assertTrue(ex.getMessage().startsWith("Invalid capture_json:"), ex.getMessage());
  }

  @Test
  void extractLastCapturedValue_blankRegex_returnsNull() {
    assertNull(TaskCaptureExtractor.extractLastCapturedValue("x=1", null));
    assertNull(TaskCaptureExtractor.extractLastCapturedValue("x=1", ""));
    assertNull(TaskCaptureExtractor.extractLastCapturedValue("x=1", "   "));
  }

  @Test
  void extractLastCapturedValue_noMatch_returnsNull() {
    String stdout = "a=1\nb=2\n";
    assertNull(TaskCaptureExtractor.extractLastCapturedValue(stdout, "c=(\\d+)"));
  }

  @Test
  void extractLastCapturedValue_returnsGroup1FromLastMatch_multiline() {
    String stdout = "x=1\ny=2\nx=3\n";
    String regex = "x=(\\d+)";

    String last = TaskCaptureExtractor.extractLastCapturedValue(stdout, regex);

    assertEquals("3", last);
  }

  @Test
  void extractLastCapturedValue_windowsNewlines_areNormalized() {
    String stdout = "x=1\r\nx=2\r\n";
    String last = TaskCaptureExtractor.extractLastCapturedValue(stdout, "x=(\\d+)");
    assertEquals("2", last);
  }

  @Test
  void extractLastCapturedValue_regexWithoutCapturingGroup_returnsNull() {
    String stdout = "x=1\nx=2\n";
    // groupCount == 0, should never set last
    String last = TaskCaptureExtractor.extractLastCapturedValue(stdout, "x=\\d+");
    assertNull(last);
  }
}