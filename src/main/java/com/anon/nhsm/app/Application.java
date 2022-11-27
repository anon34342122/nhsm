package com.anon.nhsm.app;

import com.anon.nhsm.data.SaveManager;
import com.anon.nhsm.data.SystemInfo;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

public class Application extends javafx.application.Application {
    public static File APPLICATION_DIRECTORY;

    public static File USER_HOME;
    public static String APPLICATION_NAME = "NHSM";

    static Logger logger;
    public static Stage PRIMARY_STAGE;
    public static AnchorPane ANCHOR_PANE;

    private static IOException SETUP_EXCEPTION_THROWN;

    private static SaveManager SAVE_MANAGER;

    @Override
    public void start(Stage stage) throws IOException {
        logger = LogManager.getLogger(Application.class);
        logger.info("Starting application");

        if (SETUP_EXCEPTION_THROWN != null) {
            openErrorAlert(SETUP_EXCEPTION_THROWN, Platform::exit);
        } else {
            final URL view = Application.class.getResource("application.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(view);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            stage.setTitle(APPLICATION_NAME);
            stage.setScene(scene);
            stage.getIcons().add(new Image(getClass().getResourceAsStream("app_icon.png")));
            ApplicationController applicationController = fxmlLoader.getController();
            applicationController.init(SAVE_MANAGER);
            ANCHOR_PANE = applicationController.getAnchorPane();
            PRIMARY_STAGE = stage;
            stage.show();
        }
    }

    public static void main(String[] args) {
        try {
            APPLICATION_DIRECTORY = createApplicationDirectory();
            SAVE_MANAGER = new SaveManager(SaveManager.Config.createYuzu());
            SAVE_MANAGER.setup();
        } catch (IOException e) {
            SETUP_EXCEPTION_THROWN = e;
        }

        launch(args);
    }

    public static void openErrorAlert(final Throwable throwable) {
        openErrorAlert(throwable, () -> {});
    }

    public static void openErrorAlert(final Throwable throwable, Runnable onClickedOk) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(APPLICATION_NAME + " Error");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(APPLICATION_NAME + " has thrown an exception, please feel free to send the contents of it below to the developers so we can fix it.");
        alert.initOwner(Application.PRIMARY_STAGE);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        logger.error(sw.toString());
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.setOnCloseRequest(event -> onClickedOk.run());

        alert.showAndWait();
    }

    private static File createApplicationDirectory() throws IOException {
        final SystemInfo.Platform platform = SystemInfo.getPlatform();
        USER_HOME = switch (platform) {
            case LINUX, SOLARIS, MAC -> getUnixHomeDirectory();
            case WINDOWS -> getWindowsHomeDirectory();
            default -> throw new IOException("OS not supported: " + platform.name());
        };

        final File applicationDirectory = new File(USER_HOME, Application.APPLICATION_NAME);

        if (!applicationDirectory.exists() && !applicationDirectory.mkdirs()) {
            throw new IOException("The application directory could not be created: " + applicationDirectory);
        }

        return applicationDirectory;
    }

    private static File getUnixHomeDirectory() {
        return new File(FileUtils.getUserDirectoryPath(), ".local" + File.separator + "share" + File.separator);
    }

    private static File getWindowsHomeDirectory() throws IOException {
        final String applicationData = System.getenv("APPDATA");

        if (applicationData == null) {
            throw new IOException("Appdata was not found on Windows device, aborting.");
        }

        return new File(applicationData);
    }
}