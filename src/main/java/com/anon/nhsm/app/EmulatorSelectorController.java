package com.anon.nhsm.app;

import com.anon.nhsm.data.SaveManager;
import com.anon.nhsm.data.Utils;
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
    private SaveManager saveManager;

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

    public static class EmulatorContentArea {
        private final Pane contentArea;
        private BooleanSupplier viable = () -> false;

        public EmulatorContentArea(final Pane contentArea) {
            this.contentArea = contentArea;
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
    }

    public AnchorPane getAnchorPane() {
        return ap;
    }

    public void init(final SaveManager saveManager) {
        this.saveManager = saveManager;
        contentAreas = List.of(contentAreaNoSelection, contentAreaRyujinx, contentAreaYuzu);
        menuToEmulatorData = new IdentityHashMap<>();

        menuToEmulatorData.put(sideMenuRyujinx, new EmulatorContentArea(contentAreaRyujinx).setViable(() -> saveManager.getProperties().ryujinxSaveDirectory() != null));
        menuToEmulatorData.put(sideMenuYuzu, new EmulatorContentArea(contentAreaYuzu).setViable(() -> true));
        if (saveManager.getProperties().ryujinxSaveDirectory() != null) {
            currentSaveDirectoryText.setText(saveManager.getProperties().ryujinxSaveDirectory().getAbsolutePath());
        }
    }

    private void updateRyujinxSaveDirectory(final File directory) throws IOException {
        final AppProperties properties = saveManager.getProperties().copy().ryujinxSaveDirectory(directory).build();
        saveManager.setAndWriteAppProperties(properties);
        currentSaveDirectoryText.setText(saveManager.getProperties().ryujinxSaveDirectory().getAbsolutePath());
    }

    public void selectEmulator(final EmulatorContentArea emulator) {
        contentAreas.forEach(area -> {
            area.setVisible(false);
            area.setDisable(true);
        });

        final Pane contentArea = emulator.getContentArea();
        contentArea.setDisable(false);
        contentArea.setVisible(true);

        buttonOpenSavesManager.setDisable(!emulator.isViable());
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
            Application.showApplication(Application.PRIMARY_STAGE);
        } catch (final IOException e) {
            Application.openErrorAlert(e);
        }
    }

    @FXML
    void handleLocateSaveDirectory(final MouseEvent event) {
        try {
            promptSelectRyujinxSaveDirectory(Application.PRIMARY_STAGE);
        } catch (final IOException e) {
            Application.openErrorAlert(e);
        }
    }

    private File tryGetRyujinxSavesDirectory() {
        final File homeDirectory = Utils.createRyujinxSavesDirectory();

        if (homeDirectory.exists() && homeDirectory.isDirectory()) {
            return homeDirectory;
        }

        return null;
    }

    private void promptSelectRyujinxSaveDirectory(final Stage primaryStage) throws IOException {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(tryGetRyujinxSavesDirectory());
        final File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (!selectedDirectory.getName().equals("0")) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Warning");
            alert.setContentText("The selected directory is incorrect - please select a directory called '0' in your Ryujinx directory. It should look something like 'Ryujinx/bis/user/save/0000000000000001/0'.");
            alert.setHeaderText("Invalid Save Directory");
            alert.initOwner(Application.PRIMARY_STAGE);
            alert.showAndWait();
            return;
        }

        final File mainDatFile = new File(selectedDirectory, Utils.MAIN_DAT);

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
