package com.javaisland.controller;

import com.javaisland.MainApp;
import com.javaisland.repo.LevelRepository;
import com.javaisland.repo.PlayerRepository;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class MenuController {

  @FXML private Label statusLabel;

  private final PlayerRepository playerRepo = new PlayerRepository();
  private final LevelRepository levelRepo = new LevelRepository();

  @FXML
  private void onNewGameClicked() {
    try {
      FXMLLoader fxml = new FXMLLoader(MainApp.class.getResource("/com/javaisland/main.fxml"));
      Parent root = fxml.load();

      // current window
      Stage stage = (Stage) statusLabel.getScene().getWindow();
      Scene scene = stage.getScene();

      // reuse existing scene + stylesheet (already attached in MainApp)
      scene.setRoot(root);

    } catch (Exception e) {
      if (statusLabel != null) statusLabel.setText("Fehler: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @FXML
  private void onLoadGameClicked() {
    var last = playerRepo.findLastPlayerId();
    if (last.isEmpty()) {
      statusLabel.setText("Kein Spielstand gefunden.");
      return;
    }

    long playerId = last.getAsLong();

    try {
      playerRepo.touchLastPlayed(playerId);

      FXMLLoader fxml = new FXMLLoader(MainApp.class.getResource("/com/javaisland/main.fxml"));
      Parent root = fxml.load();

      MainController controller = fxml.getController();
      controller.startLoadedGame(playerId);

      Stage stage = (Stage) statusLabel.getScene().getWindow();
      Scene scene = stage.getScene();
      scene.setRoot(root);

    } catch (Exception e) {
      statusLabel.setText("Fehler beim Laden: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @FXML
  private void onLevelSelectClicked() {
    var levels = levelRepo.findAllNonPrologueOrdered();
    if (levels.isEmpty()) {
      statusLabel.setText("Keine Level gefunden.");
      return;
    }

    Stage dialog = new Stage();
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.setTitle("Levelauswahl");

    VBox box = new VBox(10);
    box.setStyle("-fx-padding: 16;");

    box.getChildren().add(new Label("Wähle ein Level:"));

    for (var lvl : levels) {
      String title = (lvl.title() != null && !lvl.title().isBlank()) ? lvl.title() : lvl.code();
      Button b = new Button("Level " + lvl.orderIndex() + ": " + title);
      b.setMaxWidth(Double.MAX_VALUE);

      b.setOnAction(e -> {
        dialog.close();
        startNewPlayerAtLevel(lvl.id());
      });

      box.getChildren().add(b);
    }

    dialog.setScene(new Scene(box, 420, 520));
    dialog.showAndWait();
  }

  private void startNewPlayerAtLevel(long levelId) {
    try {
      long pid = playerRepo.createPlayer();

      FXMLLoader fxml = new FXMLLoader(MainApp.class.getResource("/com/javaisland/main.fxml"));
      Parent root = fxml.load();

      MainController controller = fxml.getController();
      controller.startAtLevel(pid, levelId);

      Stage stage = (Stage) statusLabel.getScene().getWindow();
      stage.getScene().setRoot(root);

    } catch (Exception e) {
      statusLabel.setText("Fehler: " + e.getMessage());
      e.printStackTrace();
    }
  }
}