package com.javaisland;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class MainApp extends Application {

  @Override
  public void start(Stage stage) throws Exception {
    var fxml = new FXMLLoader(MainApp.class.getResource("/com/javaisland/menu.fxml"));
    Parent root = fxml.load();

    var scene = new Scene(root, 900, 600);
    scene.getStylesheets().add(MainApp.class.getResource("/com/javaisland/styles.css").toExternalForm());

    stage.setTitle("JavaIsland");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}