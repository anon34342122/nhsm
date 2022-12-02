package com.anon.nhsm;

import com.anon.nhsm.app.Application;
import com.anon.nhsm.app.JavaFXHelper;
import com.anon.nhsm.controllers.EmulatorSelectorController;
import com.anon.nhsm.controllers.IslandManagerController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Stages {
    public static void showEmulatorSelector() throws IOException {
        final Stage stage = Application.PRIMARY_STAGE;
        final URL view = EmulatorSelectorController.class.getResource("emulator_selector.fxml");
        final FXMLLoader fxmlLoader = new FXMLLoader(view);
        final Parent root = fxmlLoader.load();
        final Scene scene = new Scene(root);
        stage.setTitle(Main.APPLICATION_NAME);
        stage.setScene(scene);
        JavaFXHelper.setStageIcon(stage, Application.class, "app_icon.png");
        final EmulatorSelectorController controller = fxmlLoader.getController();
        controller.init(Main.SAVE_MANAGER);
        Application.ANCHOR_PANE = controller.getAnchorPane();
        stage.show();
    }

    public static void showIslandManager() throws IOException {
        final Stage stage = Application.PRIMARY_STAGE;
        final URL view = IslandManagerController.class.getResource("island_manager.fxml");
        final FXMLLoader fxmlLoader = new FXMLLoader(view);
        final Parent root = fxmlLoader.load();
        final Scene scene = new Scene(root);
        stage.setTitle(Main.APPLICATION_NAME);
        stage.setScene(scene);
        JavaFXHelper.setStageIcon(stage, Application.class, "app_icon.png");
        final IslandManagerController controller = fxmlLoader.getController();
        controller.init(Main.SAVE_MANAGER);
        Application.ANCHOR_PANE = controller.getAnchorPane();
        stage.show();
    }
}
