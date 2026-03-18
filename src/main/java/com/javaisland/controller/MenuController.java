package com.javaisland.controller;

import com.javaisland.MainApp;
import com.javaisland.repo.PlayerRepository;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public final class MenuController {

  @FXML private Label statusLabel;

  private final PlayerRepository playerRepo = new PlayerRepository();

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

    try {
      FXMLLoader fxml = new FXMLLoader(MainApp.class.getResource("/com/javaisland/main.fxml"));
      Parent root = fxml.load();

      MainController controller = fxml.getController();
      controller.startLoadedGame(last.getAsLong());

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
    if (statusLabel != null) statusLabel.setText("Noch nicht implementiert.");
  }
}