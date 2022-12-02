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
        if (setupExceptionThrown != null) {
            JavaFXHelper.openErrorAlert(setupExceptionThrown, Platform::exit);
        } else {
            PRIMARY_STAGE = stage;
            // TODO: Should have an error if we have an emulator target for some reason with no directory
            if (appProperties.emulatorTarget() != null && appProperties.emulatorTarget().getSaveDirectory(appProperties) != null) {
                Stages.showIslandManager(appProperties);
            } else {
                Stages.showEmulatorSelector(appProperties);
            }
        }
    }
}