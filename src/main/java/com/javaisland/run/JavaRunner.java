package com.javaisland.run;

import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class JavaRunner {

  public record RunResult(boolean compiled, int exitCode, String stdout, String stderr, String message) {}

  private JavaRunner() {}

  // Backwards compatible
  public static RunResult compileAndRunMain(String userCode, Duration timeout) {
    return compileAndRunMain(userCode, timeout, -1);
  }

  public static RunResult compileAndRunMain(String userCode, Duration timeout, long taskId) {
    Path tempDir = null;

    try {
      tempDir = Files.createTempDirectory("javaisland-");

      String finalCode = augmentUserCodeForTask(userCode, taskId);

      Path src = tempDir.resolve("Main.java");
      Files.writeString(src, finalCode, StandardCharsets.UTF_8);

      RunResult compile = compile(tempDir, src);
      if (!compile.compiled) return compile;

      return runJava(tempDir, timeout);

    } catch (Exception e) {
      return new RunResult(false, -1, "", "", "Internal error: " + e.getMessage());
    } finally {
      if (tempDir != null) {
        try { deleteRecursive(tempDir); } catch (Exception ignored) {}
      }
    }
  }

  /**
   * Injects helper methods into class Main for specific tasks.
   *
   * IMPORTANT: This assumes the user's code contains "class Main" and compiles to Main.java/Main.class.
   */
  private static String augmentUserCodeForTask(String userCode, long taskId) {
    if (userCode == null) userCode = "";

    String helperBlock = helperMethodsForTask(taskId);
    if (helperBlock == null || helperBlock.isBlank()) return userCode;

    return injectIntoMainClass(userCode, helperBlock);
  }

  private static String helperMethodsForTask(long taskId) {
    // Level 3 - Task 3
    if (taskId == 14) {
      // 5 steps, then peak reached
      return """
          
          // --- injected by JavaIsland runner (do not edit) ---
          static int __ji_step = 0;
          static boolean isAtPeak() {
              __ji_step++;
              return __ji_step > 5;
          }
          // --- end injected ---
          """;
    }

    // Level 3 - Task 5
    if (taskId == 16) {
      // Found on 6th check
      return """
          
          // --- injected by JavaIsland runner (do not edit) ---
          static int __ji_scan = 0;
          static boolean fragmentFound() {
              __ji_scan++;
              return __ji_scan == 6;
          }
          // --- end injected ---
          """;
    }

    return null;
  }

  private static String injectIntoMainClass(String userCode, String helperBlock) {
    int classIdx = userCode.indexOf("class Main");
    if (classIdx < 0) return userCode; // don't break other tasks

    int braceIdx = userCode.indexOf('{', classIdx);
    if (braceIdx < 0) return userCode;

    // Insert directly after "class Main {"
    return userCode.substring(0, braceIdx + 1)
        + helperBlock
        + userCode.substring(braceIdx + 1);
  }

  private static RunResult compile(Path outDir, Path javaFile) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      return new RunResult(false, -1, "", "",
          "No system Java compiler found. Run with a JDK (javac), not only a JRE.");
    }

    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {

      Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(List.of(javaFile.toFile()));
      List<String> options = List.of("-d", outDir.toString());

      boolean ok = compiler.getTask(null, fm, diagnostics, options, null, units).call();

      StringBuilder diagText = new StringBuilder();
      for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
        diagText.append(formatDiagnostic(d)).append('\n');
      }

      if (!ok) {
        return new RunResult(false, -1, "", diagText.toString(), "Compilation failed");
      }

      return new RunResult(true, 0, "", diagText.toString(), "Compilation ok");
    }
  }

  private static String formatDiagnostic(Diagnostic<? extends JavaFileObject> d) {
    String src = (d.getSource() == null) ? "?" : Paths.get(d.getSource().toUri()).getFileName().toString();
    return src + ":" + d.getLineNumber() + ":" + d.getColumnNumber()
        + " " + d.getKind() + " " + d.getMessage(null);
  }

  private static RunResult runJava(Path classDir, Duration timeout) throws IOException, InterruptedException {
    String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", classDir.toString(), "Main");
    Process p = pb.start();

    StreamGobbler out = new StreamGobbler(p.getInputStream());
    StreamGobbler err = new StreamGobbler(p.getErrorStream());
    Thread tOut = new Thread(out, "stdout-gobbler");
    Thread tErr = new Thread(err, "stderr-gobbler");
    tOut.start();
    tErr.start();

    boolean finished = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    if (!finished) {
      p.destroyForcibly();
      p.waitFor();

      tOut.join(200);
      tErr.join(200);

      return new RunResult(true, -1, out.text(), err.text(), "Execution timed out");
    }

    tOut.join(200);
    tErr.join(200);

    return new RunResult(true, p.exitValue(), out.text(), err.text(), "Execution finished");
  }

  private static final class StreamGobbler implements Runnable {
    private final InputStream in;
    private final StringWriter sw = new StringWriter();

    private StreamGobbler(InputStream in) {
      this.in = in;
    }

    @Override
    public void run() {
      try (var r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        String line;
        while ((line = r.readLine()) != null) {
          sw.write(line);
          sw.write('\n');
        }
      } catch (IOException ignored) {}
    }

    String text() {
      return sw.toString();
    }
  }

  private static void deleteRecursive(Path root) throws IOException {
    if (!Files.exists(root)) return;
    try (var walk = Files.walk(root)) {
      walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
          .forEach(p -> {
            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
          });
    }
  }
}