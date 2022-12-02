package com.anon.nhsm.app;

import com.anon.nhsm.AppProperties;
import com.anon.nhsm.Main;
import com.anon.nhsm.controllers.EmulatorSelectorController;
import com.anon.nhsm.controllers.IslandManagerController;
import com.anon.nhsm.data.AppPaths;
import com.anon.nhsm.data.SaveManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

public class Application extends javafx.application.Application {
    private static final Logger logger = LogManager.getLogger(SaveManager.class);
    public static Stage PRIMARY_STAGE;
    public static AnchorPane ANCHOR_PANE;
    private static SaveManager SAVE_MANAGER;

    public IOException setupExceptionThrown;

    public Application() {
        super();
    }

    public Application(final IOException setupExceptionThrown, final SaveManager saveManager) {
        this.setupExceptionThrown = setupExceptionThrown;
        SAVE_MANAGER = saveManager;
        launch();
    }

    @Override
    public void start(final Stage stage) throws IOException {
        if (setupExceptionThrown != null) {
            openErrorAlert(setupExceptionThrown, Platform::exit);
        } else {
            showEmulatorSelector(stage);
        }
    }

    public static void showEmulatorSelector(final Stage stage) throws IOException {
        final URL view = EmulatorSelectorController.class.getResource("emulator_selector.fxml");
        final FXMLLoader fxmlLoader = new FXMLLoader(view);
        final Parent root = fxmlLoader.load();
        final Scene scene = new Scene(root);
        stage.setTitle(Main.APPLICATION_NAME);
        stage.setScene(scene);
        JavaFXHelper.setStageIcon(stage, Application.class, "app_icon.png");
        final EmulatorSelectorController applicationController = fxmlLoader.getController();
        applicationController.init(SAVE_MANAGER);
        ANCHOR_PANE = applicationController.getAnchorPane();
        PRIMARY_STAGE = stage;
        stage.show();
    }

    public static void showApplication(final Stage stage) throws IOException {
        final URL view = IslandManagerController.class.getResource("island_manager.fxml");
        final FXMLLoader fxmlLoader = new FXMLLoader(view);
        final Parent root = fxmlLoader.load();
        final Scene scene = new Scene(root);
        stage.setTitle(Main.APPLICATION_NAME);
        stage.setScene(scene);
        JavaFXHelper.setStageIcon(stage, Application.class, "app_icon.png");
        final IslandManagerController islandManagerController = fxmlLoader.getController();
        islandManagerController.init(SAVE_MANAGER);
        ANCHOR_PANE = islandManagerController.getAnchorPane();
        PRIMARY_STAGE = stage;
        stage.show();
    }

    public static AppProperties writeAppProperties(final AppProperties properties) throws IOException {
        AppProperties.IO.writeAppPropertiesFile(AppPaths.APP_PROPERTIES_FILE, properties);
        return properties;
    }

    public static void openErrorAlert(final Throwable throwable) {
        openErrorAlert(throwable, () -> {});
    }

    public static void openErrorAlert(final Throwable throwable, final Runnable onClickedOk) {
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(Main.APPLICATION_NAME + " Error");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(Main.APPLICATION_NAME + " has thrown an exception, please feel free to send the contents of it below to the developers so we can fix it.");
        alert.initOwner(Application.PRIMARY_STAGE);

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        logger.error(sw.toString());
        final String exceptionText = sw.toString();

        final Label label = new Label("The exception stacktrace was:");

        final TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        final GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.setOnCloseRequest(event -> onClickedOk.run());

        alert.showAndWait();
    }
}