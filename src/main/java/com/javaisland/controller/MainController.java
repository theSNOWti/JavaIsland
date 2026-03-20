package com.javaisland.controller;

import com.javaisland.capture.CaptureModeEvaluator;
import com.javaisland.capture.TaskCapture;
import com.javaisland.capture.TaskCaptureExtractor;
import com.javaisland.model.LevelDto;
import com.javaisland.model.TaskDto;
import com.javaisland.repo.HintRepository;
import com.javaisland.repo.LevelRepository;
import com.javaisland.repo.PlayerRepository;
import com.javaisland.repo.PlayerTaskResultRepository;
import com.javaisland.repo.PlayerVarRepository;
import com.javaisland.repo.TaskRepository;
import com.javaisland.repo.TutorialRepository;
import com.javaisland.run.JavaRunner;
import com.javaisland.template.StarterCodeTemplate;
import com.javaisland.ui.StatusTextFormatter;
import com.javaisland.validation.ValidationEngine;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MainController {

  // --------------------
  // Prologue config
  // --------------------
  private static final long PROLOGUE_LEVEL_ID = 1;

  // --------------------
  // UI (main)
  // --------------------
  @FXML private StackPane mainRoot;
  @FXML private ImageView backgroundImageView;

  @FXML private Label statusLabel;
  @FXML private Label levelTitleLabel;
  @FXML private Label taskTitleLabel;

  @FXML private Button hintButton;
  @FXML private Label hintStatusLabel;
  @FXML private Label hintsLabel;

  @FXML private TextArea codeTextArea;
  @FXML private Button submitButton;
  @FXML private Button skipButton;

  @FXML private Label scoreLabel;
  @FXML private ProgressBar progressBar;
  @FXML private Label progressLabel;

  // --------------------
  // UI (dialog overlay - always visible)
  // --------------------
  @FXML private VBox dialogOverlay;
  @FXML private Label dialogTitleLabel;
  @FXML private Label dialogTextLabel;
  @FXML private Button dialogNextButton;

  @FXML private ScrollPane hintBox;
  @FXML private VBox codeBox;

  @FXML private Button tutorialButton;

  // --------------------
  // Repos/Services
  // --------------------
  private final LevelRepository levelRepo = new LevelRepository();
  private final TaskRepository taskRepo = new TaskRepository();
  private final HintRepository hintRepo = new HintRepository();
  private final ValidationEngine validationEngine = new ValidationEngine();
  private final PlayerRepository playerRepo = new PlayerRepository();
  private final PlayerVarRepository playerVarRepo = new PlayerVarRepository();
  private final PlayerTaskResultRepository taskResultRepo = new PlayerTaskResultRepository();
  private final TutorialRepository tutorialRepo = new TutorialRepository();

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

  private final Parser mdParser = Parser.builder().build();
  private final HtmlRenderer mdRenderer = HtmlRenderer.builder().build();

  private int totalTasks = 0;

  private static final String BG_BASE = "/com/javaisland/images/";
  private static final String DEFAULT_BG = "JavaIsland0.png";

  private final Map<String, Image> bgCache = new HashMap<>();
  private String currentBgKey;

  /**
   * Optional callback which is invoked when the current dialog queue is exhausted
   * and the user presses "Weiter" (i.e., there is no next page).
   */
  private Runnable dialogOnFinished;

  private boolean levelIntroAlreadyShown = false;

  private boolean autoStartEnabled = true;

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
    codeTextArea.setDisable(true);
    submitButton.setDisable(true);
    hintButton.setDisable(true);

    setBackgroundFromDbValue(DEFAULT_BG);

    if (mainRoot != null && backgroundImageView != null) {
      backgroundImageView.fitWidthProperty().bind(mainRoot.widthProperty());
      backgroundImageView.fitHeightProperty().bind(mainRoot.heightProperty());
    }

    totalTasks = taskResultRepo.countAllNonPrologueTasks();
    updateProgressUi();
    updateTutorialButtonState();

    Platform.runLater(() -> {
      if (!autoStartEnabled) return;

      // no player yet; create only after Prolog Task 2 (name).
      playerId = 0;

      updateProgressUi();
      loadFirstLevelFirstTask();
    });

    setupGlassFocus();
  }

  private void applyTaskBackground(TaskDto task) {
    if (task == null) return;

    String dbVal = task.backgroundImage();
    if (dbVal == null || dbVal.isBlank()) return; // keep current background

    setBackgroundFromDbValue(dbVal.trim());
  }

  private void setBackgroundFromDbValue(String dbVal) {
    if (backgroundImageView == null) return;
    if (dbVal == null || dbVal.isBlank()) return;

    // allow absolute resource path OR file name
    String resourcePath = dbVal.startsWith("/") ? dbVal : (BG_BASE + dbVal);

    // avoid redundant reloads
    if (resourcePath.equals(currentBgKey)) return;

    Image img = bgCache.get(resourcePath);
    if (img == null) {
      var url = MainController.class.getResource(resourcePath);
      if (url == null) {
        System.err.println("Background image not found: " + resourcePath);
        return;
      }
      img = new Image(url.toExternalForm(), true);
      bgCache.put(resourcePath, img);
    }

    backgroundImageView.setImage(img);
    currentBgKey = resourcePath;
  }

  private void setupGlassFocus() {
    // Helper: activate exactly one pane
    Runnable clearAll = () -> {
      removeFocusedGlass(dialogOverlay);
      removeFocusedGlass(hintBox);
      removeFocusedGlass(codeBox);
    };

    ChangeListener<Boolean> onDialogFocus = (obs, oldV, now) -> {
      if (!now) return;
      clearAll.run();
      addFocusedGlass(dialogOverlay);
    };

    ChangeListener<Boolean> onHintFocus = (obs, oldV, now) -> {
      if (!now) return;
      clearAll.run();
      addFocusedGlass(hintBox);
    };

    ChangeListener<Boolean> onCodeFocus = (obs, oldV, now) -> {
      if (!now) return;
      clearAll.run();
      addFocusedGlass(codeBox);
    };

    // Dialog area: next button gets focus; also allow overlay itself if focus traversable
    if (dialogNextButton != null) dialogNextButton.focusedProperty().addListener(onDialogFocus);
    if (dialogOverlay != null) {
      dialogOverlay.setFocusTraversable(true);
      dialogOverlay.focusedProperty().addListener(onDialogFocus);
    }

    // Hint area: hint button / tutorial button / scroll pane
    if (hintButton != null) hintButton.focusedProperty().addListener(onHintFocus);
    if (tutorialButton != null) tutorialButton.focusedProperty().addListener(onHintFocus);
    if (hintBox != null) {
      hintBox.setFocusTraversable(true);
      hintBox.focusedProperty().addListener(onHintFocus);
    }

    // Code area: textarea + submit/skip
    if (codeTextArea != null) codeTextArea.focusedProperty().addListener(onCodeFocus);
    if (submitButton != null) submitButton.focusedProperty().addListener(onCodeFocus);
    if (skipButton != null) skipButton.focusedProperty().addListener(onCodeFocus);
    if (codeBox != null) {
      codeBox.setFocusTraversable(true);
      codeBox.focusedProperty().addListener(onCodeFocus);
    }
  }

  private static void addFocusedGlass(Node n) {
    if (n == null) return;
    if (!n.getStyleClass().contains("focused-glass")) n.getStyleClass().add("focused-glass");
  }

  private static void removeFocusedGlass(Node n) {
    if (n == null) return;
    n.getStyleClass().remove("focused-glass");
  }

  private void updateTutorialButtonState() {
    if (tutorialButton == null) return;

    boolean inPrologue = currentLevel != null && currentLevel.id() == PROLOGUE_LEVEL_ID;
    tutorialButton.setDisable(inPrologue);
  }

  private void updateProgressUi() {
    if (progressBar == null || progressLabel == null || scoreLabel == null) return;

    boolean inPrologue = currentLevel != null && currentLevel.id() == PROLOGUE_LEVEL_ID;

    // Hide during prologue
    progressBar.setManaged(!inPrologue);
    progressBar.setVisible(!inPrologue);
    progressLabel.setManaged(!inPrologue);
    progressLabel.setVisible(!inPrologue);

    scoreLabel.setManaged(!inPrologue);
    scoreLabel.setVisible(!inPrologue);

    if (inPrologue) return;

    if (playerId <= 0 || totalTasks <= 0) {
      progressBar.setProgress(0.0);
      progressLabel.setText("0/" + Math.max(totalTasks, 0) + " (0%)");
      scoreLabel.setText("Score: 0");
      return;
    }

    int done = taskResultRepo.countCompletedNonPrologue(playerId);
    double pct = Math.max(0.0, Math.min(1.0, (done * 1.0) / totalTasks));
    progressBar.setProgress(pct);
    progressLabel.setText(done + "/" + totalTasks + " (" + Math.round(pct * 100) + "%)");

    int totalScore = taskResultRepo.totalScoreNonPrologue(playerId);
    scoreLabel.setText("Score: " + totalScore);
  }

  private enum StatusKind { INFO, SUCCESS, WARNING, ERROR }

  private void setStatus(StatusKind kind, String userMessage) {
    if (statusLabel == null) return;

    statusLabel.setText(userMessage == null ? "" : userMessage);

    // Optional: color-code (works even without CSS)
    String color = switch (kind) {
      case SUCCESS -> "#B7F5C5";
      case WARNING -> "#FFE08A";
      case ERROR -> "#FFB3B3";
      default -> "#F2F4F8";
    };
    statusLabel.setStyle("-fx-text-fill: " + color + ";");
  }

  /**
   * Produces a short, user-readable message from an exception
   * without printing the whole stack trace to the UI.
   *
   * Examples:
   * - "NullPointerException: x was null"
   * - "SQLException: NOT NULL constraint failed: player.created_at"
   *
   * If the exception has no message, falls back to the exception class simple name.
   */
  private static String exceptionSummary(Throwable e) {
    if (e == null) return "Unbekannter Fehler.";

    String msg = e.getMessage();
    msg = (msg == null) ? "" : msg.trim();

    String type = e.getClass().getSimpleName();
    if (msg.isBlank()) return type;

    // Avoid repeating "Type: Type: message" patterns
    if (msg.startsWith(type + ":")) return msg;

    return type + ": " + msg;
  }

  /**
   * If you want the "message of the stacktrace" but not the full trace,
   * often the root cause message is the most relevant.
   */
  private static Throwable rootCause(Throwable e) {
    if (e == null) return null;

    Throwable cur = e;
    // guard against cycles just in case
    for (int i = 0; i < 32 && cur.getCause() != null && cur.getCause() != cur; i++) {
      cur = cur.getCause();
    }
    return cur;
  }

  private String formatValidationMessage(String raw) {
    return StatusTextFormatter.formatValidationMessage(raw);
  }

  /**
   * Compile errors are not Exceptions here (javac output), so we parse stderr
   * to show the first meaningful error line (like "Zeile 3: ...").
   */
  private String formatCompileError(String stderr) {
    return StatusTextFormatter.formatCompileError(stderr);
  }

  private String formatRuntimeError(int exitCode) {
    return "Beim Ausführen ist ein Fehler passiert. Prüfe Exceptions und Logik.";
  }

  /**
   * Convenience method if you want to display "the exception message"
   * (root cause) in the status bar.
   */
  private void setStatusFromException(StatusKind kind, String prefix, Throwable e) {
    Throwable rc = rootCause(e);
    String summary = exceptionSummary(rc);
    setStatus(kind, (prefix == null || prefix.isBlank()) ? summary : (prefix + ": " + summary));
  }

  private void loadFirstLevelFirstTask() {
    try {
      currentLevel = levelRepo.findFirstLevel();
      updateProgressUi();
      updateTutorialButtonState();

      if (currentLevel == null) {
        setStatus(StatusKind.ERROR, "Keine Level gefunden.");
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
        setStatus(StatusKind.ERROR, "Keine Aufgaben für dieses Level gefunden.");
        showNoTask();
        setDialog(PageKind.TEXT, "Fehler", "Keine Tasks für dieses Level gefunden.");
        return;
      }

      showTask(currentTask);
      startDialogForTaskStart(currentLevel, currentTask);
      setStatus(StatusKind.INFO, "Bereit.");
    } catch (Exception e) {
      // This now uses your new helper (root cause + message, not full trace)
      setStatusFromException(StatusKind.ERROR, "DB-Fehler", e);
      e.printStackTrace();

      currentTask = null;
      codeTextArea.setText(DEFAULT_CODE);
      setDialog(PageKind.TEXT, "DB-Fehler", exceptionSummary(rootCause(e)));
    }
  }

  // --------------------
  // Task UI
  // --------------------
  private void showTask(TaskDto task) {
    updateProgressUi();
    updateTutorialButtonState();

    taskTitleLabel.setText(task.title() != null ? task.title() : "-");

    // code
    String starter = (task.starterCode() != null && !task.starterCode().isBlank())
        ? task.starterCode()
        : DEFAULT_CODE;

    // resolve templates using player_var
    starter = StarterCodeTemplate.resolve(starter, playerId, playerVarRepo);
    codeTextArea.setText(starter);

    // hints
    currentHints = hintRepo.findHintsForTask(task.id());
    shownHintCount = 0;
    hintsLabel.setText("");
    updateHintUi();
  }

  private void showNoTask() {
    updateProgressUi();
    updateTutorialButtonState();

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

    applyTaskBackground(completedTask);
    
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

    if (skipButton != null) {
      boolean inPrologue = currentLevel != null && currentLevel.id() == PROLOGUE_LEVEL_ID;
      boolean showSkip = isTask && !inPrologue;

      skipButton.setVisible(showSkip);
      skipButton.setManaged(showSkip);
      skipButton.setDisable(!showSkip);
    }

    // Hints only during TASK pages
    if (isTask) updateHintUi();
    else hintButton.setDisable(true);
  }

  @FXML
  private void onDialogNextClicked() {
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

      dialogNextButton.setVisible(false);
      dialogNextButton.setManaged(false);

      codeTextArea.setDisable(true);
      submitButton.setDisable(true);
      hintButton.setDisable(true);
      updateTutorialButtonState();
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
      setStatus(StatusKind.WARNING, "Keine Aufgabe geladen.");
      return;
    }
    if (currentDialogPage == null || currentDialogPage.kind() != PageKind.TASK) {
      setStatus(StatusKind.WARNING, "Du bist gerade nicht in der Aufgabenansicht.");
      return;
    }

    setStatus(StatusKind.INFO, "Ich prüfe deine Lösung...");

    Thread.startVirtualThread(() -> {
      String code = codeTextArea.getText() == null ? "" : codeTextArea.getText();

      var run = JavaRunner.compileAndRunMain(code, Duration.ofSeconds(2), currentTask.id());
      var validation = validationEngine.validate(currentTask.validation(), run, code);

      Platform.runLater(() -> {
        boolean inPrologue = currentLevel != null && currentLevel.id() == PROLOGUE_LEVEL_ID;
        boolean canTrack = (!inPrologue && playerId > 0);

        if (!run.compiled()) {
          setStatus(StatusKind.ERROR, formatCompileError(run.stderr()));
          if (canTrack) taskResultRepo.incrementAttempts(playerId, currentTask.id());
          return;
        }

        if (run.message() != null && run.message().contains("timed out")) {
          setStatus(StatusKind.ERROR, "Dein Programm braucht zu lange. Vermutlich gibt es eine Endlosschleife.");
          System.err.println("--- TIMEOUT STDERR ---\n" + run.stderr());
          if (canTrack) taskResultRepo.incrementAttempts(playerId, currentTask.id());
          return;
        }

        if (run.exitCode() != 0) {
          setStatus(StatusKind.ERROR, formatRuntimeError(run.exitCode()));
          System.err.println("--- RUNTIME STDERR (exit " + run.exitCode() + ") ---\n" + run.stderr());
          if (canTrack) taskResultRepo.incrementAttempts(playerId, currentTask.id());
          return;
        }

        if (!validation.ok()) {
          setStatus(StatusKind.WARNING, formatValidationMessage(validation.message()));
          System.err.println("--- VALIDATION ---\n" + validation.message());
          if (canTrack) taskResultRepo.incrementAttempts(playerId, currentTask.id());
          return;
        }

        tryPersistCapturedValue(currentTask, run.stdout());

        setStatus(StatusKind.SUCCESS, "Super, das war korrekt!");

        if (!inPrologue && playerId > 0) {
          taskResultRepo.setHintsUsed(playerId, currentTask.id(), shownHintCount);
          taskResultRepo.markCompleted(playerId, currentTask.id());
          updateProgressUi();
        }

        startDialogForTaskSuccessThenAdvance(currentTask);
      });
    });
  }

  @FXML
  private void onSkipClicked() {
    if (currentTask == null) {
      statusLabel.setText("No task loaded.");
      return;
    }
    if (currentDialogPage == null || currentDialogPage.kind() != PageKind.TASK) {
      statusLabel.setText("Not on a task page.");
      return;
    }

    boolean inPrologue = currentLevel != null && currentLevel.id() == PROLOGUE_LEVEL_ID;
    if (inPrologue) {
      statusLabel.setText("Skip disabled in Prolog.");
      return;
    }

    statusLabel.setText("Skipped.");

    dialogQueue.clear();
    dialogOnFinished = null;

    enqueueText("Übersprungen", "Diese Aufgabe wurde übersprungen. Es gibt dafür keine Punkte.");

    TaskDto next = taskRepo.findNextTaskInLevel(
        currentTask.levelId(),
        currentTask.orderIndex(),
        currentTask.id()
    );

    if (next != null) {
      currentTask = next;
      showTask(currentTask);

      enqueueText(levelTitleLabel.getText(), currentTask.story());
      enqueueTask("Aufgabe", currentTask.description());
    } else {
      enqueueText("Abschluss", currentLevel != null ? currentLevel.outroText() : null);
      dialogOnFinished = this::advanceToNextLevelOrEnd;
    }

    showNextDialogPage();
  }

  private void tryPersistCapturedValue(TaskDto task, String stdout) {
    if (task == null) return;

    // 1) Prolog special-case: store player name if present in stdout
    // Format expected in stdout: "Mein Name ist <Name>"
    String maybeName = extractPlayerNameFromStdout(stdout);
    if (maybeName != null) {
      try {
        if (playerId <= 0) {
          playerId = playerRepo.createPlayer();
          updateProgressUi();
        }
        playerRepo.updatePlayerName(playerId, maybeName);
      } catch (Exception e) {
        System.err.println("Failed to save player name: " + e.getMessage());
      }
      // Note: do not return; task could also have capture_json later.
    }

    // 2) Task capture_json -> player_var (optional)
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

  // --------------------
  // Tutorial
  // --------------------
  @FXML
  private void onTutorialClicked() {
    if (currentLevel == null) return;

    List<TutorialRepository.TutorialRow> tutorials =
        tutorialRepo.findTutorialsUpToLevel(currentLevel.orderIndex());

    if (tutorials.isEmpty()) {
      statusLabel.setText("No tutorials available yet.");
      return;
    }

    showTutorialBookPopup(tutorials);
  }

  private void showTutorialBookPopup(List<TutorialRepository.TutorialRow> tutorials) {
    Stage dialog = new Stage();
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.setTitle("Tutorials");

    WebView web = new WebView();

    Label header = new Label();
    header.setStyle("-fx-font-weight: bold; -fx-padding: 8;");

    Button prev = new Button("←");
    Button next = new Button("→");
    Button close = new Button("Schließen");

    final int[] idx = { tutorials.size() - 1 }; // open at current/latest by default

    Runnable render = () -> {
      var t = tutorials.get(idx[0]);
      header.setText("Level " + t.levelOrderIndex() + ": " + (t.levelTitle() == null ? "" : t.levelTitle()));

      String md = t.text() == null ? "" : t.text();
      String html = markdownToHtml(md);

      web.getEngine().loadContent(html);

      prev.setDisable(idx[0] <= 0);
      next.setDisable(idx[0] >= tutorials.size() - 1);
    };

    prev.setOnAction(e -> { idx[0]--; render.run(); });
    next.setOnAction(e -> { idx[0]++; render.run(); });
    close.setOnAction(e -> dialog.close());

    HBox controls = new HBox(10, prev, next, new HBox(), close);
    controls.setStyle("-fx-padding: 10;");
    controls.getChildren().get(2).setStyle("-fx-hgrow: ALWAYS;");

    BorderPane root = new BorderPane();
    root.setTop(header);
    root.setCenter(web);
    root.setBottom(controls);

    Scene scene = new Scene(root, 720, 520);
    dialog.setScene(scene);

    render.run();
    dialog.showAndWait();
  }

  private String markdownToHtml(String md) {
    String body = mdRenderer.render(mdParser.parse(md == null ? "" : md));

    String css = """
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; padding: 16px; }
        pre, code { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, 'Courier New', monospace; }
        pre { background: #111; color: #eee; padding: 10px; border-radius: 8px; overflow-x: auto; }
        code { background: rgba(0,0,0,0.06); padding: 2px 4px; border-radius: 4px; }
        h1,h2,h3 { margin-top: 1.2em; }
        """;

    return """
        <!doctype html>
        <html>
          <head>
            <meta charset="utf-8"/>
            <style>%s</style>
          </head>
          <body>%s</body>
        </html>
        """.formatted(css, body);
  }

  public void startLoadedGame(long loadedPlayerId) {
    this.autoStartEnabled = false;

    this.playerId = loadedPlayerId;

    var maybeTask = taskRepo.findFirstUncompletedNonPrologue(playerId);

    if (maybeTask.isPresent()) {
      TaskDto task = maybeTask.get();

      this.currentTask = task;
      this.currentLevel = levelRepo.findById(task.levelId()).orElse(null);
      updateTutorialButtonState();

      if (currentLevel == null) {
        statusLabel.setText("Load failed: level not found for task " + task.id());
        showNoTask();
        setDialog(PageKind.TEXT, "Fehler", "Level zum Spielstand nicht gefunden.");
        return;
      }

      levelTitleLabel.setText(
          (currentLevel.title() != null && !currentLevel.title().isBlank())
              ? currentLevel.title()
              : currentLevel.code()
      );

      showTask(task);
      startDialogForTaskStart(currentLevel, task);
      updateProgressUi();
      statusLabel.setText("Loaded.");
      return;
    }

    this.currentLevel = levelRepo.findLastNonPrologueLevel().orElse(null);
    this.currentTask = null;
    updateTutorialButtonState();

    if (currentLevel != null) {
      levelTitleLabel.setText(
          (currentLevel.title() != null && !currentLevel.title().isBlank())
              ? currentLevel.title()
              : currentLevel.code()
      );
    }

    updateProgressUi();
    showNoTask();
    setDialog(PageKind.TEXT, "Fertig", "Du hast alle verfügbaren Level abgeschlossen.");
  }

  public void startAtLevel(long playerId, long levelId) {
    this.autoStartEnabled = false;
    this.playerId = playerId;

    this.currentLevel = levelRepo.findById(levelId).orElse(null);
    updateTutorialButtonState();

    if (currentLevel == null) {
      statusLabel.setText("Level not found: " + levelId);
      showNoTask();
      setDialog(PageKind.TEXT, "Fehler", "Level nicht gefunden.");
      return;
    }

    levelTitleLabel.setText(
        (currentLevel.title() != null && !currentLevel.title().isBlank())
            ? currentLevel.title()
            : currentLevel.code()
    );

    this.currentTask = taskRepo.findFirstTaskOfLevel(levelId);
    if (currentTask == null) {
      statusLabel.setText("No tasks found for level " + levelId);
      showNoTask();
      setDialog(PageKind.TEXT, "Fehler", "Keine Tasks für dieses Level gefunden.");
      return;
    }

    showTask(currentTask);
    startDialogForTaskStart(currentLevel, currentTask);
    updateProgressUi();
    statusLabel.setText("Ready.");
  }
}