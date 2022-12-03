package com.anon.nhsm.controllers;

import com.anon.nhsm.Stages;
import com.anon.nhsm.app.Application;
import com.anon.nhsm.app.JavaFXHelper;
import com.anon.nhsm.data.AppPaths;
import com.anon.nhsm.data.EmulatorType;
import com.anon.nhsm.data.SaveManager;
import com.anon.nhsm.data.SaveMetadata;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class IslandManagerController {
    private static final Logger logger = LogManager.getLogger(IslandManagerController.class);
    private static final String LOCKED_SUFFIX = " (LOCKED)";
    private SaveManager saveManager;
    @FXML private AnchorPane ap;
    @FXML private TableColumn<SaveMetadata, String> island;
    @FXML private TableColumn<SaveMetadata, String> folder;
    @FXML private TableColumn<SaveMetadata, String> description;
    @FXML private TableColumn<SaveMetadata, String> date;
    @FXML private TableColumn<SaveMetadata, String> localIsland;
    @FXML private TableColumn<SaveMetadata, String> localFolder;
    @FXML private TableColumn<SaveMetadata, String> localDescription;
    @FXML private TableColumn<SaveMetadata, String> localDate;
    @FXML private TableView<SaveMetadata> saves;
    @FXML private TableView<SaveMetadata> emulatorLocalSave;
    @FXML private ImageView yuzuLogo;
    @FXML private ImageView ryujinxLogo;
    private EmulatorType targetEmulator;

    public AnchorPane getAnchorPane() {
        return ap;
    }

    public void refreshIslandTables() {
        saves.setItems(FXCollections.observableArrayList(saveManager.getIslandsMetadata()));
        emulatorLocalSave.setItems(FXCollections.observableArrayList(saveManager.getEmulatorSaveMetadata()));
    }

    private String getIslandSuffix(final SaveMetadata island) {
        return island.emulatorLocked() ? LOCKED_SUFFIX : "";
    }

    public void init(final SaveManager saveManager) {
        this.saveManager = saveManager;
        island.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().island() + getIslandSuffix(p.getValue())));
        folder.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().nhsmIslandDirectory(saveManager.getAppProperties()).getAbsolutePath()));
        description.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().description()));
        date.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().date().toString()));
        localIsland.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().island()));
        localFolder.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().nhsmIslandDirectory(saveManager.getAppProperties()).getAbsolutePath()));
        localDescription.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().description()));
        localDate.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().date().toString()));

        refreshIslandTables();

        saves.setRowFactory(tv -> {
            final TableRow<SaveMetadata> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    swapWithLocal(row.getItem());
                }
            });
            return row ;
        });

        targetEmulator = saveManager.getAppProperties().emulatorTarget();
        yuzuLogo.setVisible(targetEmulator == EmulatorType.YUZU);
        ryujinxLogo.setVisible(targetEmulator == EmulatorType.RYUJINX);
    }

    public void handleAddIsland(final ActionEvent event) {
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(NewIslandController.class.getResource("new_island.fxml"));
            final DialogPane newIslandDialogPane = fxmlLoader.load();
            final NewIslandController controller = fxmlLoader.getController();
            final Optional<ButtonType> clickedButton = Alerts.promptNewIsland(newIslandDialogPane);

            if (clickedButton.isPresent() && clickedButton.get() == ButtonType.FINISH) {
                final boolean createdNewIsland = saveManager.createNewIsland(controller.getIslandName(), controller.getIslandDescription(), conflictingMetadata -> {
                    final String islandName = conflictingMetadata.island();
                    Alerts.notifyNamingConflict("Could not create new '" + islandName + "' island", islandName);
                });

                if (createdNewIsland) {
                    refreshIslandTables();
                }
            }
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e);
        }
    }

    public void handleSwapWithLocal(final ActionEvent actionEvent) {
        final SaveMetadata saveMetadata = saves.getSelectionModel().getSelectedItem();
        swapWithLocal(saveMetadata);
    }

    private void swapWithLocal(final SaveMetadata saveMetadata) {
        if (saveMetadata == null) {
            return;
        }

        if (saveMetadata.emulatorLocked()) {
            Alerts.notifyIslandLocked();
            return;
        }

        final ImageView emulatorLogo = targetEmulator == EmulatorType.YUZU ? yuzuLogo : ryujinxLogo;
        final Optional<ButtonType> clickedButton = Alerts.promptSwapIsland(saveMetadata, emulatorLogo);

        if (clickedButton.isPresent() && clickedButton.get() == ButtonType.YES) {
            try {
                saveManager.swapWithLocalSave(saveMetadata, this::promptToConvertLocalSaveIntoIsland);
                refreshIslandTables();
            } catch (final IOException e) {
                JavaFXHelper.openErrorAlert(e);
            }
        }
    }

    public boolean promptToConvertLocalSaveIntoIsland(final File localSaveMetadataFile) throws IOException {
        logger.info("Prompting to convert emulator local save into an island");
        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(EmulatorLocalSaveController.class.getResource("emulator_local_save.fxml"));
        final DialogPane newIslandDialogPane = fxmlLoader.load();
        final EmulatorLocalSaveController controller = fxmlLoader.getController();
        final Optional<ButtonType> clickedButton = Alerts.promptNameLocalSave(newIslandDialogPane);

        if (clickedButton.isPresent() && clickedButton.get() == ButtonType.FINISH) {
            return saveManager.convertLocalSaveToIsland(localSaveMetadataFile, controller.getIslandName(), controller.getIslandDescription(), conflictingMetadata -> {
               final String islandName = conflictingMetadata.island();
               Alerts.notifyNamingConflict("Could not convert 'Emulator Local Save' to list of Islands", islandName);
            });
        }

        return false;
    }

    public void handleDeleteIsland(final ActionEvent actionEvent) {
        final SaveMetadata saveMetadata = saves.getSelectionModel().getSelectedItem();
        if (saveMetadata == null) {
            return;
        }

        if (saveMetadata.emulatorLocked()) {
            Alerts.notifyIslandLocked();
            return;
        }

        final Optional<ButtonType> clickedButton = Alerts.promptDeleteIsland(saveMetadata);

        if (clickedButton.isPresent() && clickedButton.get() == ButtonType.YES) {
            try {
                saveManager.deleteIsland(saveMetadata);
                refreshIslandTables();
            } catch (final IOException e) {
                JavaFXHelper.openErrorAlert(e);
            }
        }
    }

    public void handleDuplicateIsland(final ActionEvent actionEvent) {
        final SaveMetadata saveMetadata = saves.getSelectionModel().getSelectedItem();
        if (saveMetadata == null) {
            return;
        }

        if (saveMetadata.emulatorLocked()) {
            Alerts.notifyIslandLocked();
            return;
        }

        try {
            final boolean duplicatedIsland = saveManager.duplicateIsland(saveMetadata, conflictingMetadata -> {
                final String islandName = conflictingMetadata.island();
                Alerts.notifyNamingConflict("Could not duplicate '" + islandName + "' island", islandName);
            });

            if (duplicatedIsland) {
                refreshIslandTables();
            }
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e);
        }
    }

    public void handleEdit(final ActionEvent actionEvent) {
        final SaveMetadata oldSaveMetadata = saves.getSelectionModel().getSelectedItem();

        if (oldSaveMetadata == null) {
            return;
        }

        if (oldSaveMetadata.emulatorLocked()) {
            Alerts.notifyIslandLocked();
            return;
        }

        try {
            final SaveMetadata newSaveMetadata = editIslandDetails(oldSaveMetadata, conflictingMetadata -> {
                final String islandName = conflictingMetadata.island();
                Alerts.notifyNamingConflict("Could not edit '" + oldSaveMetadata.island() + "' island", islandName);
            });
            if (newSaveMetadata != oldSaveMetadata) {
                refreshIslandTables();
            }
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e);
        }
    }

    public SaveMetadata editIslandDetails(final SaveMetadata oldSaveMetadata, final SaveManager.OnNamingConflict onNamingConflict) throws IOException {
        try {
            logger.info("Handling edit of" + oldSaveMetadata.island());
            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(EditIslandController.class.getResource("edit_island.fxml"));
            final DialogPane newIslandDialogPane = fxmlLoader.load();
            final EditIslandController controller = fxmlLoader.getController();
            controller.setIslandName(oldSaveMetadata.island());
            controller.setIslandDescription(oldSaveMetadata.description());
            final Optional<ButtonType> clickedButton = Alerts.promptEditIsland(oldSaveMetadata, newIslandDialogPane);

            if (clickedButton.isPresent() && clickedButton.get() == ButtonType.FINISH) {
                final SaveMetadata updatedSaveMetadata = SaveMetadata.nameAndDescription(controller.getIslandName(), controller.getIslandDescription());
                if (saveManager.updateIslandDetails(oldSaveMetadata, updatedSaveMetadata, onNamingConflict)) {
                    return updatedSaveMetadata;
                }
            }
        } catch (final IOException e) {
            throw new IOException("Something went wrong editing the details of an island.", e);
        }

        return oldSaveMetadata;
    }

    public void handleLocalSaveEditor(final ActionEvent actionEvent) {
        final Optional<ButtonType> clickedButton = Alerts.notifyEnsureGameNotRunning();

        if (clickedButton.isPresent() && clickedButton.get() == ButtonType.OK) {
            try {
                final File islandSaveDirectory = saveManager.getConfig().emulatorSaveDirectory();
                final String islandName = saveManager.getEmulatorSaveMetadata().island();
                saveManager.openSaveEditorFor(islandSaveDirectory, islandName, this::trySelectNHSEAfterMissing, this::trySelectNHSE, Alerts::notifyMainDatMissing);
            } catch (final IOException e) {
                JavaFXHelper.openErrorAlert(e);
            }
        }
    }

    public void handleSaveEditor(final ActionEvent actionEvent) {
        final SaveMetadata saveMetadata = saves.getSelectionModel().getSelectedItem();
        if (saveMetadata == null) {
            Alerts.promptSelectIslandSaveEditor();
            return;
        }

        if (saveMetadata.emulatorLocked()) {
            Alerts.notifyIslandLocked();
            return;
        }

        try {
            final File islandSaveDirectory = saveMetadata.nhsmIslandDirectory(saveManager.getAppProperties());
            final String islandName = islandSaveDirectory.getName();
            saveManager.openSaveEditorFor(islandSaveDirectory, islandName, this::trySelectNHSEAfterMissing, this::trySelectNHSE, Alerts::notifyMainDatMissing);
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e);
        }
    }

    private boolean trySelectNHSEAfterMissing() throws IOException {
        Alerts.notifyMissingNHSE();
        return trySelectNHSE();
    }

    private boolean trySelectNHSE() throws IOException {
        final Optional<ButtonType> clickedButton = Alerts.promptSetNHSEExecutable();

        if (clickedButton.isPresent() && clickedButton.get() == ButtonType.OK) {
            final DirectoryChooser directoryChooser = new DirectoryChooser();
            final File selectedDirectory = directoryChooser.showDialog(Application.PRIMARY_STAGE);

            if (selectedDirectory != null) {
                final File executable = new File(selectedDirectory, AppPaths.NHSE_EXECUTABLE);

                if (executable.exists()) {
                    saveManager.setAndWriteAppProperties(saveManager.getAppProperties().copy().nhsExecutable(executable).build());
                    return true;
                }

                Alerts.notifySelectedDirectoryNoNHSE();
            }
        }

        return false;
    }

    @FXML
    void changeEmulatorTarget(final ActionEvent event) {
        try {
            Stages.showEmulatorSelector(saveManager.getAppProperties());
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e);
        }
    }
}
