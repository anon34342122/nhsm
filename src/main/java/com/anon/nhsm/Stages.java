package com.anon.nhsm;

import com.anon.nhsm.app.Application;
import com.anon.nhsm.app.JavaFXHelper;
import com.anon.nhsm.controllers.EmulatorSelectorController;
import com.anon.nhsm.controllers.IslandManagerController;
import com.anon.nhsm.data.SaveManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Stages {
    public static Scene showEmulatorSelector(final AppProperties appProperties) throws IOException {
        final Stage stage = Application.PRIMARY_STAGE;
        final URL view = EmulatorSelectorController.class.getResource("emulator_selector.fxml");
        final FXMLLoader fxmlLoader = new FXMLLoader(view);
        fxmlLoader.setResources(Application.LANG.getResourceBundle());
        final Parent root = fxmlLoader.load();
        final Scene scene = new Scene(root);
        stage.setTitle(Main.APPLICATION_NAME);
        setScenePreservingSize(stage, scene);
        JavaFXHelper.setStageIcon(stage, Main.class, "icon_png.png");
        final EmulatorSelectorController controller = fxmlLoader.getController();
        controller.init(appProperties, Application.LANG);
        Application.ANCHOR_PANE = controller.getAnchorPane();
        stage.show();
        return scene;
    }

    private static SaveManager createSaveManager(final AppProperties appProperties) {
        try {
            final SaveManager.Config saveManagerConfig = new SaveManager.Config(appProperties.emulatorTarget().getSaveDirectory(appProperties));
            final SaveManager saveManager = new SaveManager(appProperties, saveManagerConfig);
            saveManager.setup();
            return saveManager;
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e, Platform::exit);
        }
        return null;
    }

    public static Scene showIslandManager(final AppProperties appProperties) throws IOException {
        final SaveManager saveManager = createSaveManager(appProperties);

        if (saveManager == null) {
            return null;
        }

        final Stage stage = Application.PRIMARY_STAGE;
        final URL view = IslandManagerController.class.getResource("island_manager.fxml");
        final FXMLLoader fxmlLoader = new FXMLLoader(view);
        fxmlLoader.setResources(Application.LANG.getResourceBundle());
        final Parent root = fxmlLoader.load();
        final Scene scene = new Scene(root);
        stage.setTitle(Main.APPLICATION_NAME);
        setScenePreservingSize(stage, scene);
        JavaFXHelper.setStageIcon(stage, Main.class, "icon_png.png");
        final IslandManagerController controller = fxmlLoader.getController();
        controller.init(saveManager, Application.LANG);
        Application.ANCHOR_PANE = controller.getAnchorPane();
        stage.show();
        return scene;
    }

    private static void setScenePreservingSize(final Stage stage, final Scene scene) {
        final boolean shouldPreserverSize = stage.getScene() != null;
        final double prevWidth = stage.getWidth();
        final double prevHeight = stage.getHeight();
        stage.setScene(scene);
        if (shouldPreserverSize) {
            stage.setHeight(prevHeight);
            stage.setWidth(prevWidth);
        }
    }
}
