package com.anon.nhsm.app;

import com.anon.nhsm.Stages;
import javafx.application.Platform;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class Application extends javafx.application.Application {
    public static Stage PRIMARY_STAGE;
    public static AnchorPane ANCHOR_PANE;
    public IOException setupExceptionThrown;

    public Application() {
        super();
    }

    public Application(final IOException setupExceptionThrown) {
        this.setupExceptionThrown = setupExceptionThrown;
        launch();
    }

    @Override
    public void start(final Stage stage) throws IOException {
        if (setupExceptionThrown != null) {
            JavaFXHelper.openErrorAlert(setupExceptionThrown, Platform::exit);
        } else {
            PRIMARY_STAGE = stage;
            Stages.showEmulatorSelector();
        }
    }
}