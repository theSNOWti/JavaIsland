package com.javaisland.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StatusTextFormatter {
  private StatusTextFormatter() {}

  private static final Pattern JAVAC_LOCATED_ERROR =
      Pattern.compile("^(.*?):(\\d+):\\s*error:\\s*(.*)$");

  public static String formatValidationMessage(String raw) {
    if (raw == null || raw.isBlank()) return "Das Ergebnis ist leider noch nicht korrekt.";

    String m = raw.trim();
    String lower = m.toLowerCase();

    if (lower.contains("expected") || lower.contains("regex")) {
      return "Die Ausgabe stimmt noch nicht ganz. Prüfe Format und Inhalt der Ausgabe.";
    }
    if (lower.contains("nullpointer")) {
      return "Da ist etwas schiefgelaufen (Null-Wert). Prüfe Variablen und Rückgabewerte.";
    }
    if (lower.contains("assert")) {
      return "Die Aufgabe ist noch nicht erfüllt. Schau dir die Anforderungen nochmal an.";
    }
    return "Fast! " + m;
  }

  public static String formatCompileError(String stderr) {
    String summary = extractJavacErrorSummaryWithContext(stderr);
    if (summary != null && !summary.isBlank()) return summary;
    return firstNonBlankLines(stderr, 3);
  }

  /**
   * Extracts a short javac error message for the UI.
   * Includes line number if available and (if present) the source line + caret marker.
   */
  static String extractJavacErrorSummaryWithContext(String stderr) {
    if (stderr == null || stderr.isBlank()) return null;

    String s = stderr.replace("\r\n", "\n");
    String[] lines = s.split("\n");

    for (int i = 0; i < lines.length; i++) {
      String raw = lines[i];
      String line = raw.trim();
      if (line.isBlank()) continue;

      Integer lineNo = null;
      String msg = null;

      // Case 1: "Main.java:3: error: <msg>"
      Matcher m = JAVAC_LOCATED_ERROR.matcher(line);
      if (m.matches()) {
        try { lineNo = Integer.parseInt(m.group(2)); } catch (NumberFormatException ignored) {}
        msg = m.group(3).trim();
      } else if (line.startsWith("error:")) {
        // Case 2: "error: <msg>"
        msg = line.substring("error:".length()).trim();
      }

      if (msg == null || msg.isBlank()) continue;

      StringBuilder out = new StringBuilder();
      if (lineNo != null) out.append("Zeile ").append(lineNo).append(": ");
      out.append(msg);

      String srcLine = (i + 1 < lines.length) ? lines[i + 1] : null;
      String caretLine = (i + 2 < lines.length) ? lines[i + 2] : null;

      boolean hasCaret = caretLine != null && caretLine.trim().equals("^");
      if (srcLine != null && !srcLine.trim().isBlank() && hasCaret) {
        out.append("\n").append(srcLine.stripTrailing());
        out.append("\n").append(caretLine.stripTrailing());
      }

      return out.toString();
    }

    return null;
  }

  static String firstNonBlankLines(String text, int maxLines) {
    if (text == null || text.isBlank()) return "Kompilierfehler.";

    String s = text.replace("\r\n", "\n");
    String[] lines = s.split("\n");

    StringBuilder out = new StringBuilder();
    int added = 0;

    for (String raw : lines) {
      String line = raw.stripTrailing();
      if (line.isBlank()) continue;

      if (out.length() > 0) out.append("\n");
      out.append(line);

      added++;
      if (added >= maxLines) break;
    }

    return out.length() == 0 ? "Kompilierfehler." : out.toString();
  }
}