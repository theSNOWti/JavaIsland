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
import com.javaisland.validation.ValidationEngine;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class MainController {

  // --------------------
  // Prologue config
  // --------------------
  private static final long PROLOGUE_LEVEL_ID = 1;

  // --------------------
  // UI (main)
  // --------------------
  @FXML private BorderPane mainRoot;

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

    totalTasks = taskResultRepo.countAllNonPrologueTasks();
    updateProgressUi();

    Platform.runLater(() -> {
      if (!autoStartEnabled) return; // NEW

      // no player yet; create only after Prolog Task 2 (name).
      playerId = 0;

      updateProgressUi();
      loadFirstLevelFirstTask();
    });
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

  private void loadFirstLevelFirstTask() {
    try {
      currentLevel = levelRepo.findFirstLevel();
      updateProgressUi();

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
    // Progress visibility can change when level changes, so refresh here too.
    updateProgressUi();

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
      // optional: also disable when hidden isn't necessary; but safe:
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

      var run = JavaRunner.compileAndRunMain(code, Duration.ofSeconds(2), currentTask.id());
      var validation = validationEngine.validate(currentTask.validation(), run, code);

      Platform.runLater(() -> {
        // ---- failure cases: count attempt (only if player exists and not prologue) ----
        boolean inPrologue = currentLevel != null && currentLevel.id() == PROLOGUE_LEVEL_ID;
        boolean canTrack = (!inPrologue && playerId > 0);

        if (!run.compiled()) {
          statusLabel.setText("Compile error.");
          System.err.println(run.stderr());
          if (canTrack) taskResultRepo.incrementAttempts(playerId, currentTask.id());
          return;
        }
        if (run.message() != null && run.message().contains("timed out")) {
          statusLabel.setText("Timed out (possible infinite loop).");
          System.err.println(run.stderr());
          if (canTrack) taskResultRepo.incrementAttempts(playerId, currentTask.id());
          return;
        }
        if (run.exitCode() != 0) {
          statusLabel.setText("Runtime error (exit " + run.exitCode() + ").");
          System.err.println(run.stderr());
          if (canTrack) taskResultRepo.incrementAttempts(playerId, currentTask.id());
          return;
        }
        if (!validation.ok()) {
          statusLabel.setText("Failed: " + validation.message());
          if (canTrack) taskResultRepo.incrementAttempts(playerId, currentTask.id());
          return;
        }

        // success: persist captured values + (possibly) create player via prologue name
        tryPersistCapturedValue(currentTask, run.stdout());

        statusLabel.setText("Success!");

        // Save hint usage + mark completed (non-prologue only)
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

    // Move to next task/level without awarding completion/score.
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
          updateProgressUi(); // progress becomes visible once we're out of prologue
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

    // Up to current level (by order_index). Prolog excluded in query.
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
    controls.getChildren().get(2).setStyle("-fx-hgrow: ALWAYS;"); // spacer hack-free would be Region; keep minimal

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

    // small default styling, including code blocks
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
}