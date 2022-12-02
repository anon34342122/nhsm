package com.anon.nhsm.app;

import com.anon.nhsm.Main;
import com.anon.nhsm.data.SaveManager;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class JavaFXHelper {
    private static final Logger logger = LogManager.getLogger(SaveManager.class);

    public static void setStageIcon(final Stage stage, final Class<?> resourceClass, final String pathToResource) throws IOException {
        try (final InputStream stream = resourceClass.getResourceAsStream(pathToResource)) {
            if (stream != null) {
                stage.getIcons().add(new Image(stream));
            }
        }
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
