package com.anon.nhsm.app;

import com.anon.nhsm.AppProperties;
import com.anon.nhsm.Stages;
import com.anon.nhsm.data.AppPaths;
import javafx.application.Platform;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class Application extends javafx.application.Application {
    public static Stage PRIMARY_STAGE;
    public static AnchorPane ANCHOR_PANE;
    private IOException setupExceptionThrown;
    private AppProperties appProperties;

    public Application() {
        super();

        try {
            AppPaths.bootStrap();
            this.appProperties = AppProperties.IO.loadAndValidateAppProperties();
        } catch (final IOException e) {
            this.setupExceptionThrown = e;
        }
    }

    public static void launchApp() {
        launch();
    }

    @Override
    public void start(final Stage stage) throws IOException {
        PRIMARY_STAGE = stage;
        Stages.showEmulatorSelector(appProperties);

        if (setupExceptionThrown != null) {
            JavaFXHelper.openErrorAlert(setupExceptionThrown, Platform::exit);
            return;
        }

        if (appProperties.emulatorTarget() != null) {
            if (appProperties.emulatorTarget().getSaveDirectory(appProperties) == null) {
                JavaFXHelper.openErrorAlert(new RuntimeException("The save directory was null for this emulator even though the app properties has a set emulator target"));
                Stages.showEmulatorSelector(appProperties);
            } else {
                Stages.showIslandManager(appProperties);
            }
        }
    }
}