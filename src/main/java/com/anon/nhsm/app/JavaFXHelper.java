package com.anon.nhsm.app;

import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class JavaFXHelper {
    public static void setStageIcon(final Stage stage, final Class<?> resourceClass, final String pathToResource) throws IOException {
        try (final InputStream stream = resourceClass.getResourceAsStream(pathToResource)) {
            if (stream != null) {
                stage.getIcons().add(new Image(stream));
            }
        }
    }
}
