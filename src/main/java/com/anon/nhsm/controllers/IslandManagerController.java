package com.anon.nhsm.controllers;

import com.anon.nhsm.Stages;
import com.anon.nhsm.app.Application;
import com.anon.nhsm.app.JavaFXHelper;
import com.anon.nhsm.data.SaveMetadata;
import com.anon.nhsm.data.SaveManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

public class IslandManagerController {
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

    public AnchorPane getAnchorPane() {
        return ap;
    }

    public void refreshIslandTables() {
        saves.setItems(FXCollections.observableArrayList(saveManager.getIslandsMetadata()));
        emulatorLocalSave.setItems(FXCollections.observableArrayList(saveManager.getEmulatorSaveData()));
    }

    public void init(final SaveManager saveManager) {
        this.saveManager = saveManager;
        island.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().island()));
        folder.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().folder()));
        description.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().description()));
        date.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().date().toString()));
        localIsland.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().island()));
        localFolder.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().folder()));
        localDescription.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().description()));
        localDate.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().date().toString()));

        refreshIslandTables();

        saves.setRowFactory(tv -> {
            final TableRow<SaveMetadata> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    final SaveMetadata saveMetadata = row.getItem();
                    showSwapWithLocalPopup(saveMetadata);
                }
            });
            return row ;
        });
    }

    public void handleAddIsland(final ActionEvent event) {
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(NewIslandController.class.getResource("new_island.fxml"));
            final DialogPane newIslandDialogPane = fxmlLoader.load();

            final NewIslandController controller = fxmlLoader.getController();

            final Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(newIslandDialogPane);
            dialog.setTitle("Add New Island");
            dialog.initOwner(Application.PRIMARY_STAGE);

            final Optional<ButtonType> clickedbutton = dialog.showAndWait();

            if (clickedbutton.isPresent() && clickedbutton.get() == ButtonType.FINISH) {
                if (saveManager.createNewIsland(controller.getIslandName(), controller.getIslandDescription())) {
                    refreshIslandTables();
                }
            }
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e);
        }
    }

    public void handleSwapWithLocal(final ActionEvent actionEvent) {
        final SaveMetadata saveMetadata = saves.getSelectionModel().getSelectedItem();
        if (saveMetadata == null) {
            return;
        }

        showSwapWithLocalPopup(saveMetadata);
    }

    private void showSwapWithLocalPopup(final SaveMetadata saveMetadata) {
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Swap with Local");
        alert.setContentText("Do you want to swap this island with your Emulator Local Save?");
        alert.setHeaderText("Swapping '" + saveMetadata.island() + "' island with Emulator Local Save");
        alert.setGraphic(new ImageView(Application.class.getResource("yuzu.png").toString()));
        alert.initOwner(Application.PRIMARY_STAGE);
        final Optional<ButtonType> type = alert.showAndWait();

        if (type.isPresent() && type.get() == ButtonType.OK) {
            try {
                saveManager.swapWithLocalSave(saveMetadata);
                refreshIslandTables();
            } catch (final IOException e) {
                JavaFXHelper.openErrorAlert(e);
            }
        }
    }

    public void handleDeleteIsland(final ActionEvent actionEvent) {
        final SaveMetadata saveMetadata = saves.getSelectionModel().getSelectedItem();
        if (saveMetadata == null) {
            return;
        }

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Island");
        alert.setContentText("WARNING: Are you ABSOLUTELY sure you want to delete this island? This is an IRREVERSIBLE action.");
        alert.setHeaderText("Deleting '" + saveMetadata.island() + "' Island");
        alert.setGraphic(new ImageView(Application.class.getResource("delete.png").toString()));
        alert.initOwner(Application.PRIMARY_STAGE);
        final Optional<ButtonType> type = alert.showAndWait();

        if (type.isPresent() && type.get() == ButtonType.OK) {
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

        try {
            saveManager.duplicateIsland(saveMetadata);
            refreshIslandTables();
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e);
        }
    }

    public void handleEdit(final ActionEvent actionEvent) {
        final SaveMetadata oldSaveMetadata = saves.getSelectionModel().getSelectedItem();

        if (oldSaveMetadata == null) {
            return;
        }

        try {
            final SaveMetadata newSaveMetadata = saveManager.editIslandDetails(oldSaveMetadata);
            if (newSaveMetadata != oldSaveMetadata) {
                refreshIslandTables();
            }
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e);
        }
    }

    public void handleLocalSaveEditor(final ActionEvent actionEvent) {
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning");
        alert.setContentText("Make sure your Emulator is not running the game FIRST before editing this save data.");
        alert.setHeaderText("WARNING: Make sure the game is not open");
        alert.setGraphic(new ImageView(Application.class.getResource("delete.png").toString()));
        alert.initOwner(Application.PRIMARY_STAGE);
        final Optional<ButtonType> type = alert.showAndWait();

        if (type.isPresent() && type.get() == ButtonType.OK) {
            try {
                saveManager.openSaveEditorFor(Application.PRIMARY_STAGE, saveManager.getConfig().emulatorSaveDirectory().toPath());
            } catch (final IOException e) {
                JavaFXHelper.openErrorAlert(e);
            }
        }
    }

    public void handleSaveEditor(final ActionEvent actionEvent) {
        final SaveMetadata saveMetadata = saves.getSelectionModel().getSelectedItem();
        if (saveMetadata == null) {
            final Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setContentText("Please select an Island to use the Save Editor with.");
            alert.setHeaderText("Select an Island");
            alert.initOwner(Application.PRIMARY_STAGE);
            alert.showAndWait();
            return;
        }

        try {
            saveManager.openSaveEditorFor(Application.PRIMARY_STAGE, Paths.get(saveMetadata.folder()));
        } catch (final IOException e) {
            JavaFXHelper.openErrorAlert(e);
        }
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
