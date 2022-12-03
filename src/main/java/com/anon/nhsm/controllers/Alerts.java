package com.anon.nhsm.controllers;

import com.anon.nhsm.app.Application;
import com.anon.nhsm.data.AppPaths;
import com.anon.nhsm.data.SaveMetadata;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class Alerts {
    private static final Logger logger = LogManager.getLogger(Alerts.class);

    static Optional<ButtonType> notifyMainDatMissing(final String islandName) {
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText("The '" + islandName + "' island does not have a main.dat file, so the Save Editor cannot open.");
        alert.setHeaderText("Cannot use Save Editor");
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    public static Optional<ButtonType> notifyNamingConflict(final String headerError, final String newIslandName) {
        logger.info(headerError);
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText("The name '" + newIslandName + "' you tried to give for this island already exists.");
        alert.setHeaderText(headerError);
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> notifyIslandLocked() {
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText("This island cannot be swapped, deleted, edited or duplicated since it is being used currently by a different target emulator." +
                " Switch to the other emulator it's using by clicking 'Settings -> Change Emulator Target' to make any changes to this island.");
        alert.setHeaderText("WARNING: Island in use by another emulator");
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> promptSwapIsland(final SaveMetadata saveMetadata, final ImageView emulatorLogo) {
        final Alert alert = new Alert(Alert.AlertType.WARNING,
                "Do you want to swap this island with your Emulator Local Save?",
                ButtonType.YES,
                ButtonType.CANCEL);
        alert.setTitle("Swap with Local");
        alert.setHeaderText("Swapping '" + saveMetadata.island() + "' island with Emulator Local Save");
        final ImageView graphic = new ImageView(emulatorLogo.getImage());
        graphic.setFitWidth(45);
        graphic.setFitHeight(45);

        alert.setGraphic(graphic);
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> promptNameLocalSave(final DialogPane newIslandDialogPane) {
        final Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(newIslandDialogPane);
        dialog.setTitle("Name your Emulator Local Save as an Island");
        dialog.initOwner(Application.PRIMARY_STAGE);
        return dialog.showAndWait();
    }

    static Optional<ButtonType> promptDeleteIsland(final SaveMetadata saveMetadata) {
        final Alert alert = new Alert(Alert.AlertType.WARNING,
                "WARNING: Are you ABSOLUTELY sure you want to delete this island? This is an IRREVERSIBLE action.",
                ButtonType.YES,
                ButtonType.CANCEL);
        alert.setTitle("Delete Island");
        alert.setHeaderText("Deleting '" + saveMetadata.island() + "' Island");
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> promptEditIsland(final SaveMetadata oldSaveMetadata, final DialogPane newIslandDialogPane) {
        final Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(newIslandDialogPane);
        dialog.setTitle("Edit '" + oldSaveMetadata.island() + "' Island");
        dialog.initOwner(Application.PRIMARY_STAGE);
        return dialog.showAndWait();
    }

    static  Optional<ButtonType> promptSelectIslandSaveEditor() {
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText("Please select an Island to use the Save Editor with.");
        alert.setHeaderText("Select an Island");
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> notifyEnsureGameNotRunning() {
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText("Make sure your Emulator is not running the game FIRST before editing this save data.");
        alert.setHeaderText("WARNING: Make sure the game is not open");
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> promptNewIsland(final DialogPane newIslandDialogPane) {
        final Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(newIslandDialogPane);
        dialog.setTitle("Add New Island");
        dialog.initOwner(Application.PRIMARY_STAGE);
        return dialog.showAndWait();
    }

    static Optional<ButtonType> notifyMissingNHSE() {
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText("Your previously chosen directory for the NHSE executable no longer exists or the executable is missing. Next prompt will have you select the directory again.");
        alert.setHeaderText("Re-select NHSE directory");
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> promptSetNHSEExecutable() {
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Set NHSE Executable Directory");
        alert.setContentText("In order to edit the save data of an island, you must select the directory of your NHSE executable. Press OK to select the directory or cancel to stop.");
        alert.setHeaderText("Select the NHSE directory to proceed");
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> notifySelectedDirectoryNoNHSE() {
        final Alert exeMissing = new Alert(Alert.AlertType.WARNING);
        exeMissing.setTitle("Warning");
        exeMissing.setContentText("The selected directory does not contain an NHSE executable with the following name: " + AppPaths.NHSE_EXECUTABLE);
        exeMissing.setHeaderText("Cannot use Save Editor");
        exeMissing.initOwner(Application.PRIMARY_STAGE);
        return exeMissing.showAndWait();
    }
}
