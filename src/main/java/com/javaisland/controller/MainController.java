package com.javaisland.controller;

import com.javaisland.capture.CaptureModeEvaluator;
import com.javaisland.capture.TaskCapture;
import com.javaisland.capture.TaskCaptureExtractor;
import com.javaisland.model.LevelDto;
import com.javaisland.model.TaskDto;
import com.javaisland.repo.HintRepository;
import com.javaisland.repo.LevelRepository;
import com.javaisland.repo.PlayerRepository;
import com.javaisland.repo.PlayerVarRepository;
import com.javaisland.repo.TaskRepository;
import com.javaisland.run.JavaRunner;
import com.javaisland.template.StarterCodeTemplate;
import com.javaisland.validation.ValidationEngine;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class MainController {

  // --------------------
  // UI (main)
  // --------------------
  @FXML private Label statusLabel;
  @FXML private Label levelTitleLabel;
  @FXML private Label taskTitleLabel;

  @FXML private Button hintButton;
  @FXML private Label hintStatusLabel;
  @FXML private Label hintsLabel;

  @FXML private TextArea codeTextArea;
  @FXML private Button submitButton;

  // --------------------
  // UI (dialog overlay - always visible)
  // --------------------
  @FXML private VBox dialogOverlay;
  @FXML private Label dialogTitleLabel;
  @FXML private Label dialogTextLabel;
  @FXML private Button dialogNextButton;

  // --------------------
  // Repos/Services
  // --------------------
  private final LevelRepository levelRepo = new LevelRepository();
  private final TaskRepository taskRepo = new TaskRepository();
  private final HintRepository hintRepo = new HintRepository();
  private final ValidationEngine validationEngine = new ValidationEngine();
  private final PlayerRepository playerRepo = new PlayerRepository();
  private final PlayerVarRepository playerVarRepo = new PlayerVarRepository();

  // --------------------
  // State
  // --------------------
  private long playerId;

  private LevelDto currentLevel;
  private TaskDto currentTask;

  private List<String> currentHints = new ArrayList<>();
  private int shownHintCount = 0;

  private enum PageKind { TEXT, TASK }

  private record DialogPage(PageKind kind, String title, String text) {}

  private final Deque<DialogPage> dialogQueue = new ArrayDeque<>();
  private DialogPage currentDialogPage;

  /**
   * Optional callback which is invoked when the current dialog queue is exhausted
   * and the user presses "Weiter" (i.e., there is no next page).
   */
  private Runnable dialogOnFinished;

  private boolean levelIntroAlreadyShown = false;

  private static final String DEFAULT_CODE = """
      public class Main {
          public static void main(String[] args) {
              System.out.println("...");
          }
      }
      """;

  // --------------------
  // Lifecycle
  // --------------------
  @FXML
  private void initialize() {
    // dialogOverlay is always visible per requirement; we only change its content + button visibility.
    // Disable editing until we hit a TASK page.
    codeTextArea.setDisable(true);
    submitButton.setDisable(true);
    hintButton.setDisable(true);

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
        currentTask = null;
        codeTextArea.setText(DEFAULT_CODE);
        setDialog(PageKind.TEXT, "Fehler", "Keine Levels in der DB gefunden.");
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
        setDialog(PageKind.TEXT, "Fehler", "Keine Tasks für dieses Level gefunden.");
        return;
      }

      showTask(currentTask);
      startDialogForTaskStart(currentLevel, currentTask);
      statusLabel.setText("Ready.");
    } catch (Exception e) {
      statusLabel.setText("DB error: " + e.getMessage());
      currentTask = null;
      codeTextArea.setText(DEFAULT_CODE);
      setDialog(PageKind.TEXT, "DB error", String.valueOf(e.getMessage()));
    }
  }

  // --------------------
  // Task UI
  // --------------------
  private void showTask(TaskDto task) {
    taskTitleLabel.setText(task.title() != null ? task.title() : "-");

    // code
    String starter = (task.starterCode() != null && !task.starterCode().isBlank())
    ? task.starterCode()
    : DEFAULT_CODE;

    // NEW: resolve templates using player_var
    starter = StarterCodeTemplate.resolve(starter, playerId, playerVarRepo);

    codeTextArea.setText(starter);

    // hints
    currentHints = hintRepo.findHintsForTask(task.id());
    shownHintCount = 0;
    hintsLabel.setText("");
    updateHintUi();
  }

  private void showNoTask() {
    taskTitleLabel.setText("-");
    if (codeTextArea.getText() == null || codeTextArea.getText().isBlank()) {
      codeTextArea.setText(DEFAULT_CODE);
    }
    currentHints = List.of();
    shownHintCount = 0;
    hintsLabel.setText("");
    updateHintUi();
  }

  // --------------------
  // Dialog scripting
  // --------------------
  private void startDialogForTaskStart(LevelDto level, TaskDto task) {
    dialogQueue.clear();
    dialogOnFinished = null;

    // Level intro should appear once per app run
    if (!levelIntroAlreadyShown) {
      enqueueText("Prolog", level.introText());
      if (isNotBlank(level.introText())) levelIntroAlreadyShown = true;
    }

    enqueueText(levelTitleLabel.getText(), task.story());
    enqueueTask("Aufgabe", task.description());

    showNextDialogPage();
  }

  private void startDialogForTaskSuccessThenAdvance(TaskDto completedTask) {
    dialogQueue.clear();
    dialogOnFinished = null;

    enqueueText("Erfolg", completedTask.successText());

    TaskDto next = taskRepo.findNextTaskInLevel(
        completedTask.levelId(),
        completedTask.orderIndex(),
        completedTask.id()
    );

    if (next != null) {
      currentTask = next;
      showTask(currentTask);

      enqueueText(levelTitleLabel.getText(), currentTask.story());
      enqueueTask("Aufgabe", currentTask.description());
    } else {
      // End of level: outro text, then advancing to next level when user clicks "Weiter"
      enqueueText("Abschluss", currentLevel != null ? currentLevel.outroText() : null);
      dialogOnFinished = this::advanceToNextLevelOrEnd;
    }

    showNextDialogPage();
  }

  private void enqueueText(String title, String text) {
    if (!isNotBlank(text)) return;
    dialogQueue.addLast(new DialogPage(PageKind.TEXT, title, text));
  }

  private void enqueueTask(String title, String text) {
    if (!isNotBlank(text)) return;
    dialogQueue.addLast(new DialogPage(PageKind.TASK, title, text));
  }

  private static boolean isNotBlank(String s) {
    return s != null && !s.isBlank();
  }

  private void showNextDialogPage() {
    DialogPage page = dialogQueue.pollFirst();

    if (page == null) {
      // Queue exhausted: perform finish action (if any) and stop.
      if (dialogOnFinished != null) {
        Runnable r = dialogOnFinished;
        dialogOnFinished = null;
        r.run();
      }
      return;
    }

    currentDialogPage = page;
    applyDialogPage(page);
  }

  private void applyDialogPage(DialogPage page) {
    setDialog(page.kind(), page.title(), page.text());
  }

  private void setDialog(PageKind kind, String title, String text) {
    dialogTitleLabel.setText(title != null ? title : "");
    dialogTextLabel.setText(text != null ? text : "");

    boolean isTask = kind == PageKind.TASK;

    // "Weiter" button only for non-task pages
    dialogNextButton.setVisible(!isTask);
    dialogNextButton.setManaged(!isTask);

    // During TEXT pages: disable editor + submit; during TASK pages enable
    codeTextArea.setDisable(!isTask);
    submitButton.setDisable(!isTask);

    // Hints only during TASK pages
    if (isTask) updateHintUi();
    else hintButton.setDisable(true);
  }

  @FXML
  private void onDialogNextClicked() {
    // Only exists for TEXT pages; TASK pages have button hidden.
    showNextDialogPage();
  }

  private void advanceToNextLevelOrEnd() {
    if (currentLevel == null) {
      statusLabel.setText("No level loaded.");
      setDialog(PageKind.TEXT, "Ende", "Kein Level geladen.");
      return;
    }

    LevelDto nextLevel = levelRepo.findNextLevel(currentLevel.orderIndex(), currentLevel.id());
    if (nextLevel == null) {
      statusLabel.setText("All levels completed.");
      setDialog(PageKind.TEXT, "Ende", "Du hast alle verfügbaren Level abgeschlossen.");

      // no further pages -> no button
      dialogNextButton.setVisible(false);
      dialogNextButton.setManaged(false);

      // lock editing because there's no next task
      codeTextArea.setDisable(true);
      submitButton.setDisable(true);
      hintButton.setDisable(true);
      return;
    }

    currentLevel = nextLevel;
    levelTitleLabel.setText(
        (currentLevel.title() != null && !currentLevel.title().isBlank())
            ? currentLevel.title()
            : currentLevel.code()
    );

    TaskDto firstTask = taskRepo.findFirstTaskOfLevel(currentLevel.id());
    if (firstTask == null) {
      statusLabel.setText("No tasks found for next level.");
      showNoTask();
      setDialog(PageKind.TEXT, "Fehler", "Keine Tasks für das nächste Level gefunden.");
      return;
    }

    currentTask = firstTask;
    showTask(currentTask);

    // For a new level we should show its intro (even if levelIntroAlreadyShown is true for the previous level)
    dialogQueue.clear();
    dialogOnFinished = null;

    enqueueText("Prolog", currentLevel.introText());
    enqueueText(levelTitleLabel.getText(), currentTask.story());
    enqueueTask("Aufgabe", currentTask.description());

    showNextDialogPage();
  }

  // --------------------
  // Submit / validate
  // --------------------
  @FXML
  private void onSubmitClicked() {
    if (currentTask == null) {
      statusLabel.setText("No task loaded.");
      return;
    }
    if (currentDialogPage == null || currentDialogPage.kind() != PageKind.TASK) {
      statusLabel.setText("Not on a task page.");
      return;
    }

    statusLabel.setText("Running...");

    Thread.startVirtualThread(() -> {
      String code = codeTextArea.getText() == null ? "" : codeTextArea.getText();

      var run = JavaRunner.compileAndRunMain(code, Duration.ofSeconds(2));
      var validation = validationEngine.validate(currentTask.validation(), run, code);

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

        // NEW: persist captured value (if capture_json exists)
        tryPersistCapturedValue(currentTask, run.stdout());

        statusLabel.setText("Success!");
        startDialogForTaskSuccessThenAdvance(currentTask);
      });
    });
  }

  private void tryPersistCapturedValue(TaskDto task, String stdout) {
    if (task == null) return;

    // 1) Prolog special-case: store player name if present in stdout
    // Format expected in stdout: "Mein Name ist <Name>"
    if (playerId > 0) {
      String maybeName = extractPlayerNameFromStdout(stdout);
      if (maybeName != null) {
        try {
          playerRepo.updatePlayerName(playerId, maybeName);
        } catch (Exception e) {
          System.err.println("Failed to save player name: " + e.getMessage());
        }
        // Note: do NOT return here; a task could also have capture_json.
      }
    }

    // 2) Generic capture_json -> player_var
    if (playerId > 0) {
      String maybeName = extractPlayerNameFromStdout(stdout);
      if (maybeName != null) {
        try {
          playerRepo.updatePlayerName(playerId, maybeName);
        } catch (Exception e) {
          System.err.println("Failed to save player name: " + e.getMessage());
        }
      }
    }
  
    if (playerId <= 0) return;
  
    TaskCapture cap;
    try {
      cap = TaskCaptureExtractor.parse(task.captureJson());
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return;
    }
    if (cap == null) return;
  
    String extracted = TaskCaptureExtractor.extractLastCapturedValue(stdout, cap.stdoutRegex());
    if (extracted == null) return;
  
    var decision = CaptureModeEvaluator.evaluate(
        task.captureMode(),
        task.captureParams(),
        cap,
        extracted,
        playerId,
        playerVarRepo
    );
  
    if (!decision.shouldPersist()) {
      // If you want capture failures to FAIL the task, return a boolean instead and handle it in onSubmitClicked.
      System.err.println("Capture skipped: " + decision.errorMessage());
      return;
    }
  
    playerVarRepo.upsert(playerId, cap.key(), cap.type(), decision.finalValueText());
  }

  private static String extractPlayerNameFromStdout(String stdout) {
    if (stdout == null || stdout.isBlank()) return null;

    String normalized = stdout.replace("\r\n", "\n");
    for (String line : normalized.split("\n")) {
      String t = line.trim();
      String prefix = "Mein Name ist ";
      if (!t.startsWith(prefix)) continue;

      String name = t.substring(prefix.length()).trim();
      if (!name.isBlank()) return name;
    }
    return null;
  }

  // --------------------
  // Hints (only enabled in TASK pages)
  // --------------------
  @FXML
  private void onHintClicked() {
    if (currentTask == null) return;

    if (shownHintCount < currentHints.size()) shownHintCount++;

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

    boolean isTaskPage = (currentDialogPage != null && currentDialogPage.kind() == PageKind.TASK);
    boolean noMore = shownHintCount >= total;

    hintButton.setDisable(!isTaskPage || noMore);

    if (noMore) {
      hintButton.setText("No more hints");
      hintStatusLabel.setText("(" + total + "/" + total + ")");
    } else {
      hintButton.setText("Show hint (" + (shownHintCount + 1) + "/" + total + ")");
      hintStatusLabel.setText("(" + shownHintCount + "/" + total + ")");
    }
  }
}