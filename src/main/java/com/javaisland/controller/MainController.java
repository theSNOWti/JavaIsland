package com.javaisland.controller;

import com.javaisland.model.LevelDto;
import com.javaisland.model.TaskDto;
import com.javaisland.repo.LevelRepository;
import com.javaisland.repo.PlayerRepository;
import com.javaisland.repo.TaskRepository;
import com.javaisland.run.JavaRunner;
import com.javaisland.validation.ValidationEngine;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.time.Duration;

public final class MainController {

  @FXML private Label statusLabel;

  @FXML private Label levelTitleLabel;
  @FXML private Label taskTitleLabel;
  @FXML private Label instructionLabel;

  @FXML private TextArea codeTextArea;

  private final LevelRepository levelRepo = new LevelRepository();
  private final TaskRepository taskRepo = new TaskRepository();
  private final ValidationEngine validationEngine = new ValidationEngine();
  private final PlayerRepository playerRepo = new PlayerRepository();

  private long playerId;

  private LevelDto currentLevel;
  private TaskDto currentTask;

  private static final String DEFAULT_CODE = """
      public class Main {
          public static void main(String[] args) {
              System.out.println("...");
          }
      }
      """;

  @FXML
  private void initialize() {
    // Make sure UI is ready, then load player + first task
    Platform.runLater(() -> {
      try {
        playerId = playerRepo.ensureCurrentPlayerId();
      } catch (Exception e) {
        // Non-fatal: game can still run without saving player state
        System.err.println("Failed to ensure player: " + e.getMessage());
        playerId = 0;
      }

      loadFirstLevelFirstTask();
    });
  }

  private void loadFirstLevelFirstTask() {
    try {
      currentLevel = levelRepo.findFirstLevel();
      if (currentLevel == null) {
        statusLabel.setText("No levels found (SQLite table: level)");
        codeTextArea.setText(DEFAULT_CODE);
        currentTask = null;
        return;
      }

      levelTitleLabel.setText(
          (currentLevel.title() != null && !currentLevel.title().isBlank())
              ? currentLevel.title()
              : currentLevel.code()
      );

      currentTask = taskRepo.findFirstTaskOfLevel(currentLevel.id());
      if (currentTask == null) {
        statusLabel.setText("No tasks found for first level (SQLite table: task)");
        showNoTask();
        return;
      }

      showTask(currentTask);
      statusLabel.setText("Ready.");
    } catch (Exception e) {
      statusLabel.setText("DB error: " + e.getMessage());
      currentTask = null;
      codeTextArea.setText(DEFAULT_CODE);
    }
  }

  private void showTask(TaskDto task) {
    taskTitleLabel.setText(task.title() != null ? task.title() : "-");
    instructionLabel.setText(task.description() != null ? task.description() : "");

    String starter = (task.starterCode() != null && !task.starterCode().isBlank())
        ? task.starterCode()
        : DEFAULT_CODE;

    codeTextArea.setText(starter);

    int idx = codeTextArea.getText().indexOf("\"...\"");
    if (idx >= 0) codeTextArea.positionCaret(idx + 1);
  }

  private void showNoTask() {
    taskTitleLabel.setText("-");
    instructionLabel.setText("");
    if (codeTextArea.getText() == null || codeTextArea.getText().isBlank()) {
      codeTextArea.setText(DEFAULT_CODE);
    }
  }

  @FXML
  private void onSubmitClicked() {
    if (currentTask == null) {
      statusLabel.setText("No task loaded.");
      return;
    }

    statusLabel.setText("Running...");

    Thread.startVirtualThread(() -> {
      String code = codeTextArea.getText() == null ? "" : codeTextArea.getText();

      var run = JavaRunner.compileAndRunMain(code, Duration.ofSeconds(2));
      var validation = validationEngine.validate(currentTask.validation(), run);

      Platform.runLater(() -> {
        if (!run.compiled()) {
          statusLabel.setText("Compile error.");
          System.err.println(run.stderr());
          return;
        }
        if (run.message() != null && run.message().contains("timed out")) {
          statusLabel.setText("Timed out (possible infinite loop).");
          System.err.println(run.stderr());
          return;
        }
        if (run.exitCode() != 0) {
          statusLabel.setText("Runtime error (exit " + run.exitCode() + ").");
          System.err.println(run.stderr());
          return;
        }

        if (!validation.ok()) {
          statusLabel.setText("Failed: " + validation.message());
          return;
        }

        // If stdout contains "Mein Name ist ...", store the name
        String maybeName = extractNameFromStdout(run.stdout());
        if (maybeName != null && playerId > 0) {
          try {
            playerRepo.updatePlayerName(playerId, maybeName);
          } catch (Exception e) {
            statusLabel.setText("Success, but failed to save name: " + e.getMessage());
            return;
          }
        }

        // Success -> next task in same level
        TaskDto next = taskRepo.findNextTaskInLevel(
            currentTask.levelId(),
            currentTask.orderIndex(),
            currentTask.id()
        );

        if (next == null) {
          statusLabel.setText("Success! No more tasks in this level.");
          return;
        }

        currentTask = next;
        showTask(currentTask);
        statusLabel.setText("Success! Next task loaded.");
      });
    });
  }

  /**
   * Takes program stdout, looks for a second line:
   * "Mein Name ist <name>" and returns <name>.
   */
  private static String extractNameFromStdout(String stdout) {
    if (stdout == null) return null;

    String normalized = stdout.replace("\r\n", "\n").trim();
    String[] lines = normalized.split("\n");

    if (lines.length < 2) return null;

    String line2 = lines[1].trim();
    String prefix = "Mein Name ist ";
    if (!line2.startsWith(prefix)) return null;

    String name = line2.substring(prefix.length()).trim();
    return name.isBlank() ? null : name;
  }
}