package com.anon.nhsm.app;

import com.anon.nhsm.data.SaveData;
import com.anon.nhsm.data.SaveManager;
import com.anon.nhsm.new_island.NewIslandController;
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

public class ApplicationController {
    private SaveManager saveManager;
    @FXML private AnchorPane ap;
    @FXML private TableColumn<SaveData, String> island;
    @FXML private TableColumn<SaveData, String> folder;
    @FXML private TableColumn<SaveData, String> description;
    @FXML private TableColumn<SaveData, String> date;
    @FXML private TableColumn<SaveData, String> localIsland;
    @FXML private TableColumn<SaveData, String> localFolder;
    @FXML private TableColumn<SaveData, String> localDescription;
    @FXML private TableColumn<SaveData, String> localDate;
    @FXML private TableView<SaveData> saves;
    @FXML private TableView<SaveData> emulatorLocalSave;

    public AnchorPane getAnchorPane() {
        return ap;
    }

    public void refreshIslandTables() {
        saves.setItems(FXCollections.observableArrayList(saveManager.getIslandsMetadata()));
        emulatorLocalSave.setItems(FXCollections.observableArrayList(saveManager.getEmulatorSaveMetadata()));
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
            TableRow<SaveData> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    final SaveData saveData = row.getItem();
                    showSwapWithLocalPopup(saveData);
                }
            });
            return row ;
        });
    }

    public void handleAddIsland(ActionEvent event) {
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(NewIslandController.class.getResource("new_island.fxml"));
            DialogPane newIslandDialogPane = fxmlLoader.load();

            NewIslandController controller = fxmlLoader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(newIslandDialogPane);
            dialog.setTitle("Add New Island");
            dialog.initOwner(Application.PRIMARY_STAGE);

            Optional<ButtonType> clickedbutton = dialog.showAndWait();

            if (clickedbutton.isPresent() && clickedbutton.get() == ButtonType.FINISH) {
                if (saveManager.createNewIsland(controller.getIslandName(), controller.getIslandDescription())) {
                    refreshIslandTables();
                }
            }
        } catch (IOException e) {
            Application.openErrorAlert(e);
        }
    }

    public void handleSwapWithLocal(ActionEvent actionEvent) {
        final SaveData saveData = saves.getSelectionModel().getSelectedItem();
        if (saveData == null) {
            return;
        }

        showSwapWithLocalPopup(saveData);
    }

    private void showSwapWithLocalPopup(SaveData saveData) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Swap with Local");
        alert.setContentText("Do you want to swap this island with your Emulator Local Save?");
        alert.setHeaderText("Swapping '" + saveData.island() + "' island with Emulator Local Save");
        alert.setGraphic(new ImageView(getClass().getResource("yuzu.png").toString()));
        alert.initOwner(Application.PRIMARY_STAGE);
        final Optional<ButtonType> type = alert.showAndWait();

        if (type.isPresent() && type.get() == ButtonType.OK) {
            try {
                saveManager.swapWithLocalSave(saveData);
                refreshIslandTables();
            } catch (IOException e) {
                Application.openErrorAlert(e);
            }
        }
    }

    public void handleDeleteIsland(ActionEvent actionEvent) {
        final SaveData saveData = saves.getSelectionModel().getSelectedItem();
        if (saveData == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Island");
        alert.setContentText("WARNING: Are you ABSOLUTELY sure you want to delete this island? This is an IRREVERSIBLE action.");
        alert.setHeaderText("Deleting '" + saveData.island() + "' Island");
        alert.setGraphic(new ImageView(getClass().getResource("delete.png").toString()));
        alert.initOwner(Application.PRIMARY_STAGE);
        final Optional<ButtonType> type = alert.showAndWait();

        if (type.isPresent() && type.get() == ButtonType.OK) {
            try {
                saveManager.deleteIsland(saveData);
                refreshIslandTables();
            } catch (IOException e) {
                Application.openErrorAlert(e);
            }
        }
    }

    public void handleDuplicateIsland(ActionEvent actionEvent) {
        final SaveData saveData = saves.getSelectionModel().getSelectedItem();
        if (saveData == null) {
            return;
        }

        try {
            saveManager.duplicateIsland(saveData);
            refreshIslandTables();
        } catch (IOException e) {
            Application.openErrorAlert(e);
        }
    }

    public void handleEdit(ActionEvent actionEvent) {
        final SaveData oldSaveData = saves.getSelectionModel().getSelectedItem();

        if (oldSaveData == null) {
            return;
        }

        try {
            final SaveData newSaveData = saveManager.editIslandDetails(oldSaveData);
            if (newSaveData != oldSaveData) {
                refreshIslandTables();
            }
        } catch (IOException e) {
            Application.openErrorAlert(e);
        }
    }

    public void handleLocalSaveEditor(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning");
        alert.setContentText("Make sure your Emulator is not running the game FIRST before editing this save data.");
        alert.setHeaderText("WARNING: Make sure the game is not open");
        alert.setGraphic(new ImageView(getClass().getResource("delete.png").toString()));
        alert.initOwner(Application.PRIMARY_STAGE);
        final Optional<ButtonType> type = alert.showAndWait();

        if (type.isPresent() && type.get() == ButtonType.OK) {
            try {
                saveManager.openSaveEditorFor(Application.PRIMARY_STAGE, saveManager.getConfig().emulatorSaveDirectory().toPath());
            } catch (IOException e) {
                Application.openErrorAlert(e);
            }
        }
    }

    public void handleSaveEditor(ActionEvent actionEvent) {
        final SaveData saveData = saves.getSelectionModel().getSelectedItem();
        if (saveData == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setContentText("Please select an Island to use the Save Editor with.");
            alert.setHeaderText("Select an Island");
            alert.initOwner(Application.PRIMARY_STAGE);
            alert.showAndWait();
            return;
        }

        try {
            saveManager.openSaveEditorFor(Application.PRIMARY_STAGE, Paths.get(saveData.folder()));
        } catch (IOException e) {
            Application.openErrorAlert(e);
        }
    }

    @FXML
    void changeEmulatorTarget(ActionEvent event) {
        try {
            Application.showEmulatorSelector(Application.PRIMARY_STAGE);
        } catch (IOException e) {
            Application.openErrorAlert(e);
        }
    }
}
