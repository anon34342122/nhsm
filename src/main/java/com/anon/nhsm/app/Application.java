package com.anon.nhsm.app;

import com.anon.nhsm.AppProperties;
import com.anon.nhsm.LanguageMap;
import com.anon.nhsm.Stages;
import com.anon.nhsm.data.AppPaths;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;

public class Application extends javafx.application.Application {
    public static Stage PRIMARY_STAGE;
    public static AnchorPane ANCHOR_PANE;
    public static LanguageMap LANG;
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

    public static void setLanguage(final Locale locale) {
        Locale.setDefault(locale);
        LANG = new LanguageMap(Locale.getDefault());
    }

    @Override
    public void start(final Stage stage) throws IOException {
        setLanguage(LanguageMap.ID_TO_LOCALE.getOrDefault(appProperties.languageId(), Locale.US));

        PRIMARY_STAGE = stage;
        Stages.showEmulatorSelector(appProperties);

        if (setupExceptionThrown != null) {
            JavaFXHelper.openErrorAlert(setupExceptionThrown, Platform::exit);
            return;
        }

        if (appProperties.emulatorTarget() != null) {
            final Scene scene;
            if (appProperties.emulatorTarget().getSaveDirectory(appProperties) == null) {
                JavaFXHelper.openErrorAlert(new RuntimeException("The save directory was null for this emulator even though the app properties has a set emulator target"));
                scene = Stages.showEmulatorSelector(appProperties);
            } else {
                scene = Stages.showIslandManager(appProperties);
            }

            if (scene != null) {
                scene.getWindow().centerOnScreen();
            }
        }
    }
}