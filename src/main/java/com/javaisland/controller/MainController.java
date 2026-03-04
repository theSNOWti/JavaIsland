package com.javaisland.controller;

import com.javaisland.model.LevelDto;
import com.javaisland.model.TaskDto;
import com.javaisland.repo.HintRepository;
import com.javaisland.repo.LevelRepository;
import com.javaisland.repo.PlayerRepository;
import com.javaisland.repo.TaskRepository;
import com.javaisland.run.JavaRunner;
import com.javaisland.validation.ValidationEngine;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class MainController {

  @FXML private Label statusLabel;

  @FXML private Label levelTitleLabel;
  @FXML private Label taskTitleLabel;
  @FXML private Label instructionLabel;

  @FXML private Button hintButton;
  @FXML private Label hintStatusLabel;
  @FXML private Label hintsLabel;

  @FXML private TextArea codeTextArea;

  private final LevelRepository levelRepo = new LevelRepository();
  private final TaskRepository taskRepo = new TaskRepository();
  private final HintRepository hintRepo = new HintRepository();
  private final ValidationEngine validationEngine = new ValidationEngine();
  private final PlayerRepository playerRepo = new PlayerRepository();

  private long playerId;

  private LevelDto currentLevel;
  private TaskDto currentTask;

  private List<String> currentHints = new ArrayList<>();
  private int shownHintCount = 0;

  private static final String DEFAULT_CODE = """
      public class Main {
          public static void main(String[] args) {
              System.out.println("...");
          }
      }
      """;

  @FXML
  private void initialize() {
    Platform.runLater(() -> {
      try {
        playerId = playerRepo.ensureCurrentPlayerId();
      } catch (Exception e) {
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

    // code
    String starter = (task.starterCode() != null && !task.starterCode().isBlank())
        ? task.starterCode()
        : DEFAULT_CODE;
    codeTextArea.setText(starter);

    int idx = codeTextArea.getText().indexOf("\"...\"");
    if (idx >= 0) codeTextArea.positionCaret(idx + 1);

    // hints
    currentHints = hintRepo.findHintsForTask(task.id());
    shownHintCount = 0;
    hintsLabel.setText("");
    updateHintUi();
  }

  private void showNoTask() {
    taskTitleLabel.setText("-");
    instructionLabel.setText("");
    if (codeTextArea.getText() == null || codeTextArea.getText().isBlank()) {
      codeTextArea.setText(DEFAULT_CODE);
    }

    currentHints = List.of();
    shownHintCount = 0;
    hintsLabel.setText("");
    updateHintUi();
  }

  @FXML
  private void onHintClicked() {
    if (currentTask == null) return;

    if (shownHintCount < currentHints.size()) {
      shownHintCount++;
    }

    // show 1..shownHintCount, including previous
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < shownHintCount; i++) {
      if (i > 0) sb.append("\n\n");
      sb.append("Hint ").append(i + 1).append(": ").append(currentHints.get(i));
    }
    hintsLabel.setText(sb.toString());

    updateHintUi();
  }

  private void updateHintUi() {
    int total = currentHints == null ? 0 : currentHints.size();

    if (total == 0) {
      hintButton.setDisable(true);
      hintButton.setText("No hints");
      hintStatusLabel.setText("");
      return;
    }

    hintButton.setDisable(shownHintCount >= total);
    if (shownHintCount >= total) {
      hintButton.setText("No more hints");
      hintStatusLabel.setText("(" + total + "/" + total + ")");
    } else {
      hintButton.setText("Show hint (" + (shownHintCount + 1) + "/" + total + ")");
      hintStatusLabel.setText("(" + shownHintCount + "/" + total + ")");
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

        String maybeName = extractNameFromStdout(run.stdout());
        if (maybeName != null && playerId > 0) {
          try {
            playerRepo.updatePlayerName(playerId, maybeName);
          } catch (Exception e) {
            statusLabel.setText("Success, but failed to save name: " + e.getMessage());
            return;
          }
        }

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