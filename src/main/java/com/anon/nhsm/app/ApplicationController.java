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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ResourceBundle;

public class ApplicationController {
    private static final Logger logger = LogManager.getLogger(ApplicationController.class);
    @FXML private AnchorPane ap;
    @FXML private URL location;
    @FXML private ResourceBundle resources;
    @FXML private TableColumn<SaveData, String> island;
    @FXML private TableColumn<SaveData, String> folder;
    @FXML private TableColumn<SaveData, String> description;
    @FXML private TableColumn<SaveData, String> date;
    @FXML private TableColumn<SaveData, String> yuzuIsland;
    @FXML private TableColumn<SaveData, String> yuzuFolder;
    @FXML private TableColumn<SaveData, String> yuzuDescription;
    @FXML private TableColumn<SaveData, String> yuzuDate;
    @FXML private TableView<SaveData> saves;
    @FXML private TableView<SaveData> localYuzuSave;

    public AnchorPane getAnchorPane() {
        return ap;
    }

    public void refreshIslandTables() {
        saves.setItems(FXCollections.observableArrayList(SaveManager.ISLAND_SAVES));
        localYuzuSave.setItems(FXCollections.observableArrayList(SaveManager.LOCAL_YUZU_SAVE));
    }

    public void initialize() {
        island.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().island()));
        folder.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().folder()));
        description.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().description()));
        date.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().date().toString()));
        yuzuIsland.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().island()));
        yuzuFolder.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().folder()));
        yuzuDescription.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().description()));
        yuzuDate.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().date().toString()));

        refreshIslandTables();

        saves.setRowFactory(tv -> {
            TableRow<SaveData> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    final SaveData saveData = row.getItem();
                    showApplyToYuzuPopup(saveData);
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
                if (SaveManager.createNewIsland(controller.getIslandName(), controller.getIslandDescription())) {
                    refreshIslandTables();
                }
            }
        } catch (IOException e) {
            Application.openErrorAlert(e);
        }
    }

    public void handleApplyToYuzu(ActionEvent actionEvent) {
        final SaveData saveData = saves.getSelectionModel().getSelectedItem();
        if (saveData == null) {
            return;
        }

        showApplyToYuzuPopup(saveData);
    }

    private void showApplyToYuzuPopup(SaveData saveData) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Apply to Yuzu");
        alert.setContentText("Do you want to apply this island to your Yuzu folder?");
        alert.setHeaderText("Applying '" + saveData.island() + "' island to Yuzu");
        alert.setGraphic(new ImageView(getClass().getResource("yuzu.png").toString()));
        alert.initOwner(Application.PRIMARY_STAGE);
        final Optional<ButtonType> type = alert.showAndWait();

        if (type.isPresent() && type.get() == ButtonType.OK) {
            try {
                SaveManager.applyToYuzuSaveFolder(saveData);
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
                SaveManager.deleteIsland(saveData);
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
            SaveManager.duplicateIsland(saveData);
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
            final SaveData newSaveData = SaveManager.editIslandDetails(oldSaveData);
            if (newSaveData != oldSaveData) {
                refreshIslandTables();
            }
        } catch (IOException e) {
            Application.openErrorAlert(e);
        }
    }

    public void handleYuzuSaveEditor(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning");
        alert.setContentText("Make sure your Yuzu does not have this save file open FIRST before editing its save data.");
        alert.setHeaderText("WARNING: Make sure the game is not open");
        alert.setGraphic(new ImageView(getClass().getResource("delete.png").toString()));
        alert.initOwner(Application.PRIMARY_STAGE);
        final Optional<ButtonType> type = alert.showAndWait();

        if (type.isPresent() && type.get() == ButtonType.OK) {
            try {
                SaveManager.openSaveEditorFor(Application.PRIMARY_STAGE, SaveManager.YUZU_SAVE_DIRECTORY.toPath());
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
            SaveManager.openSaveEditorFor(Application.PRIMARY_STAGE, Paths.get(saveData.folder()));
        } catch (IOException e) {
            Application.openErrorAlert(e);
        }
    }
}
