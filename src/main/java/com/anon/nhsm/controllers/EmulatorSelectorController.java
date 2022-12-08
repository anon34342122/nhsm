package com.anon.nhsm.controllers;

import com.anon.nhsm.AppProperties;
import com.anon.nhsm.LanguageMap;
import com.anon.nhsm.Main;
import com.anon.nhsm.Stages;
import com.anon.nhsm.app.Application;
import com.anon.nhsm.app.JavaFXHelper;
import com.anon.nhsm.data.AppPaths;
import com.anon.nhsm.data.EmulatorType;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;

public class EmulatorSelectorController {
    private AppProperties appProperties;
    private LanguageMap lang;
    @FXML private AnchorPane ap;
    @FXML private VBox contentAreaNoSelection;
    @FXML private Pane contentAreaRyujinx;
    @FXML private Pane contentAreaYuzu;
    @FXML private HBox sideMenuRyujinx;
    @FXML private HBox sideMenuYuzu;
    @FXML private Button buttonOpenSavesManager;
    @FXML private Text currentSaveDirectoryTextRyujinx;
    @FXML private Text currentSaveDirectoryTextYuzu;
    @FXML private TextArea yuzuTextArea;
    @FXML private TextArea ryujinxTextArea;

    private List<Pane> contentAreas;
    private Map<HBox, EmulatorContentArea> menuToEmulatorData;
    private EmulatorContentArea selectedEmulator;

    public static class EmulatorContentArea {
        private final EmulatorType emulatorType;
        private final Pane contentArea;
        private BooleanSupplier viable = () -> false;

        private Runnable onSelected = () -> {};

        public EmulatorContentArea(final EmulatorType emulatorType, final Pane contentArea) {
            this.emulatorType = emulatorType;
            this.contentArea = contentArea;
        }

        public EmulatorType getEmulatorType() {
            return emulatorType;
        }

        public Pane getContentArea() {
            return contentArea;
        }

        public boolean isViable() {
            return viable.getAsBoolean();
        }

        public EmulatorContentArea setViable(final BooleanSupplier viable) {
            this.viable = viable;
            return this;
        }

        public EmulatorContentArea onSelected(final Runnable onSelected) {
            this.onSelected = onSelected;
            return this;
        }
    }

    public AnchorPane getAnchorPane() {
        return ap;
    }

    public void init(final AppProperties appProperties, final LanguageMap lang) {
        this.lang = lang;
        this.appProperties = appProperties;
        contentAreas = List.of(contentAreaNoSelection, contentAreaRyujinx, contentAreaYuzu);
        menuToEmulatorData = new IdentityHashMap<>();

        final EmulatorContentArea ryujinxEmulator = new EmulatorContentArea(EmulatorType.RYUJINX, contentAreaRyujinx).setViable(() -> this.appProperties.ryujinxSaveDirectory() != null);
        final EmulatorContentArea yuzuEmulator = new EmulatorContentArea(EmulatorType.YUZU, contentAreaYuzu).setViable(() -> true).onSelected(() -> {
            if (this.appProperties.yuzuSaveDirectory() != null) {
                return;
            }

            try {
                updateYuzuSaveDirectory(AppPaths.createYuzuSaveDirectory());
            } catch (final IOException e) {
                JavaFXHelper.openErrorAlert(e);
            }
        });

        if (appProperties.yuzuSaveDirectory() != null) {
            currentSaveDirectoryTextYuzu.setText(appProperties.yuzuSaveDirectory().toAbsolutePath().toString());
        }

        if (appProperties.ryujinxSaveDirectory() != null) {
            currentSaveDirectoryTextRyujinx.setText(appProperties.ryujinxSaveDirectory().toAbsolutePath().toString());
        }

        menuToEmulatorData.put(sideMenuRyujinx, ryujinxEmulator);
        menuToEmulatorData.put(sideMenuYuzu,yuzuEmulator);

        yuzuTextArea.setText(lang.get("yuzu_text_area"));
        ryujinxTextArea.setText(lang.get("ryujinx_text_area"));
    }

    private void updateYuzuSaveDirectory(final Path directory) throws IOException {
        appProperties = Main.writeAppProperties(appProperties.copy().yuzuSaveDirectory(directory).build());
        currentSaveDirectoryTextYuzu.setText(directory.toAbsolutePath().toString());
    }

    private void updateRyujinxSaveDirectory(final Path directory) throws IOException {
        appProperties = Main.writeAppProperties(appProperties.copy().ryujinxSaveDirectory(directory).build());
        currentSaveDirectoryTextRyujinx.setText(directory.toAbsolutePath().toString());
        buttonOpenSavesManager.setDisable(!selectedEmulator.isViable());
    }

    public void selectEmulator(final EmulatorContentArea emulator) {
        selectedEmulator = emulator;
        contentAreas.forEach(area -> {
            area.setVisible(false);
            area.setDisable(true);
        });

        final Pane contentArea = emulator.getContentArea();
        contentArea.setDisable(false);
        contentArea.setVisible(true);

        buttonOpenSavesManager.setDisable(!selectedEmulator.isViable());
        selectedEmulator.onSelected.run();
    }

    @FXML
    void handleClickMenu(final MouseEvent event) {
        menuToEmulatorData.keySet().stream()
                .filter(event.getSource()::equals)
                .map(menuToEmulatorData::get)
                .findFirst().ifPresent(this::selectEmulator);
    }

    @FXML
    void handleOpenSaveManager(final MouseEvent event) {
        try {
            appProperties = Main.writeAppProperties(appProperties.copy().emulatorTarget(selectedEmulator.getEmulatorType()).build());
            Stages.showIslandManager(appProperties);
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e);
        }
    }

    @FXML
    void handleLocateSaveDirectory(final MouseEvent event) {
        try {
            promptSelectRyujinxSaveDirectory(Application.PRIMARY_STAGE);
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e);
        }
    }

    private File tryGetRyujinxSavesDirectory() {
        final Path homeDirectory = AppPaths.createRyujinxSavesDirectory();

        if (Files.exists(homeDirectory) && Files.isDirectory(homeDirectory)) {
            return homeDirectory.toFile();
        }

        return null;
    }

    private void promptSelectRyujinxSaveDirectory(final Stage primaryStage) throws IOException {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(tryGetRyujinxSavesDirectory());
        final File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory == null) {
            return;
        }

        if (!selectedDirectory.getName().equals("0")) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(lang.get("alerts.incorrect_directory.title"));
            alert.setContentText(lang.get("alerts.incorrect_directory.content"));
            alert.setHeaderText(lang.get("alerts.incorrect_directory.header"));
            alert.initOwner(Application.PRIMARY_STAGE);
            alert.showAndWait();
            return;
        }

        final File mainDatFile = new File(selectedDirectory, AppPaths.MAIN_DAT);

        if (!mainDatFile.exists()) {
            final Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(lang.get("alerts.no_main_dat.title"));
            alert.setContentText(lang.get("alerts.no_main_dat.content"));
            alert.setHeaderText(lang.get("alerts.no_main_dat.header"));
            alert.initOwner(Application.PRIMARY_STAGE);
            alert.showAndWait();
        }

        updateRyujinxSaveDirectory(selectedDirectory.toPath());
    }

    @FXML
    void handleChangeLanguage(final ActionEvent event) {
        if (event.getSource() instanceof MenuItem menuItem) {
            try {
                final String languageId = menuItem.getId();
                appProperties = Application.changeLanguage(languageId, appProperties);
            } catch (final IOException e) {
                JavaFXHelper.openErrorAlert(e);
            }
        }
    }
}
