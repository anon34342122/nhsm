package com.anon.nhsm.controllers;

import com.anon.nhsm.AppProperties;
import com.anon.nhsm.Main;
import com.anon.nhsm.Stages;
import com.anon.nhsm.app.Application;
import com.anon.nhsm.app.JavaFXHelper;
import com.anon.nhsm.data.AppPaths;
import com.anon.nhsm.data.EmulatorType;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class EmulatorSelectorController {
    private AppProperties appProperties;

    @FXML private AnchorPane ap;
    @FXML private VBox contentAreaNoSelection;
    @FXML private Pane contentAreaRyujinx;
    @FXML private Pane contentAreaYuzu;
    @FXML private HBox sideMenuRyujinx;
    @FXML private HBox sideMenuYuzu;
    @FXML private Button buttonOpenSavesManager;
    @FXML private Button buttonLocateSaveDirectory;
    @FXML private Text currentSaveDirectoryText;

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

    public void init(final AppProperties appProperties) {
        this.appProperties = appProperties;
        contentAreas = List.of(contentAreaNoSelection, contentAreaRyujinx, contentAreaYuzu);
        menuToEmulatorData = new IdentityHashMap<>();

        menuToEmulatorData.put(sideMenuRyujinx, new EmulatorContentArea(EmulatorType.RYUJINX, contentAreaRyujinx).setViable(() -> this.appProperties.ryujinxSaveDirectory() != null));
        menuToEmulatorData.put(sideMenuYuzu, new EmulatorContentArea(EmulatorType.YUZU, contentAreaYuzu).setViable(() -> true).onSelected(() -> {
            try {
                updateYuzuSaveDirectory(AppPaths.createYuzuSaveDirectory());
            } catch (final IOException e) {
                JavaFXHelper.openErrorAlert(e);
            }
        }));
        if (appProperties.ryujinxSaveDirectory() != null) {
            currentSaveDirectoryText.setText(appProperties.ryujinxSaveDirectory().getAbsolutePath());
        }
    }

    private void updateYuzuSaveDirectory(final File directory) throws IOException {
        appProperties = Main.writeAppProperties(appProperties.copy().yuzuSaveDirectory(directory).build());
        //currentSaveDirectoryText.setText(appProperties.ryujinxSaveDirectory().getAbsolutePath()); TODO: Have text for yuzu emulator content area
    }

    private void updateRyujinxSaveDirectory(final File directory) throws IOException {
        appProperties = Main.writeAppProperties(appProperties.copy().ryujinxSaveDirectory(directory).build());
        currentSaveDirectoryText.setText(appProperties.ryujinxSaveDirectory().getAbsolutePath());
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
        final File homeDirectory = AppPaths.createRyujinxSavesDirectory();

        if (homeDirectory.exists() && homeDirectory.isDirectory()) {
            return homeDirectory;
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
            alert.setTitle("Warning");
            alert.setContentText("The selected directory is incorrect - please select a directory called '0' in your Ryujinx directory. It should look something like 'Ryujinx/bis/user/save/0000000000000001/0'.");
            alert.setHeaderText("Invalid Save Directory");
            alert.initOwner(Application.PRIMARY_STAGE);
            alert.showAndWait();
            return;
        }

        final File mainDatFile = new File(selectedDirectory, AppPaths.MAIN_DAT);

        if (!mainDatFile.exists()) {
            final Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setContentText("The selected directory does not contain a 'main.dat' file. This is not an error and the directory will be used, but make sure to double check that this is indeed the Animal Crossing: New Horizons save directory.");
            alert.setHeaderText("Keep in mind");
            alert.initOwner(Application.PRIMARY_STAGE);
            alert.showAndWait();
        }

        updateRyujinxSaveDirectory(selectedDirectory);
    }
}
