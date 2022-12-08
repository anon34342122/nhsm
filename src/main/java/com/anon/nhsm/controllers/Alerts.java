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
        alert.setTitle(Application.LANG.get("alerts.main_dat_missing.title"));
        alert.setContentText(Application.LANG.get("alerts.main_dat_missing.content", islandName));
        alert.setHeaderText(Application.LANG.get("alerts.main_dat_missing.header"));
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    public static Optional<ButtonType> notifyNamingConflict(final String headerError, final String newIslandName) {
        logger.info(headerError);
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(Application.LANG.get("alerts.naming_conflict.title"));
        alert.setContentText(Application.LANG.get("alerts.naming_conflict.content", newIslandName));
        alert.setHeaderText(headerError);
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> notifyIslandLocked() {
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(Application.LANG.get("alerts.island_locked.title"));
        alert.setContentText(Application.LANG.get("alerts.island_locked.content"));
        alert.setHeaderText(Application.LANG.get("alerts.island_locked.header"));
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> promptSwapIsland(final SaveMetadata saveMetadata, final ImageView emulatorLogo) {
        final Alert alert = new Alert(Alert.AlertType.WARNING,
                Application.LANG.get("alerts.swap_island.content"),
                ButtonType.YES,
                ButtonType.CANCEL);
        alert.setTitle(Application.LANG.get("alerts.swap_island.title"));
        alert.setHeaderText(Application.LANG.get("alerts.swap_island.header", saveMetadata.island()));
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
        dialog.setTitle(Application.LANG.get("alerts.name_local_save.title"));
        dialog.initOwner(Application.PRIMARY_STAGE);
        return dialog.showAndWait();
    }

    static Optional<ButtonType> promptDeleteIsland(final SaveMetadata saveMetadata) {
        final Alert alert = new Alert(Alert.AlertType.WARNING,
                Application.LANG.get("alerts.delete_island.content"),
                ButtonType.YES,
                ButtonType.CANCEL);
        alert.setTitle(Application.LANG.get("alerts.delete_island.title"));
        alert.setHeaderText(Application.LANG.get("alerts.delete_island.header", saveMetadata.island()));
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> promptEditIsland(final SaveMetadata oldSaveMetadata, final DialogPane newIslandDialogPane) {
        final Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(newIslandDialogPane);
        dialog.setTitle(Application.LANG.get("alerts.edit_island.title", oldSaveMetadata.island()));
        dialog.initOwner(Application.PRIMARY_STAGE);
        return dialog.showAndWait();
    }

    static  Optional<ButtonType> promptSelectIslandSaveEditor() {
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(Application.LANG.get("alerts.select_island_nhse.title"));
        alert.setContentText(Application.LANG.get("alerts.select_island_nhse.content"));
        alert.setHeaderText(Application.LANG.get("alerts.select_island_nhse.header"));
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> notifyEnsureGameNotRunning() {
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(Application.LANG.get("alerts.ensure_game_not_running.title"));
        alert.setContentText(Application.LANG.get("alerts.ensure_game_not_running.content"));
        alert.setHeaderText(Application.LANG.get("alerts.ensure_game_not_running.header"));
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> promptNewIsland(final DialogPane newIslandDialogPane) {
        final Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(newIslandDialogPane);
        dialog.setTitle(Application.LANG.get("alerts.new_island.title"));
        dialog.initOwner(Application.PRIMARY_STAGE);
        return dialog.showAndWait();
    }

    static Optional<ButtonType> notifyMissingNHSE() {
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(Application.LANG.get("alerts.missing_nhse.title"));
        alert.setContentText(Application.LANG.get("alerts.missing_nhse.content"));
        alert.setHeaderText(Application.LANG.get("alerts.missing_nhse.header"));
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> promptSetNHSEExecutable() {
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(Application.LANG.get("alerts.set_nhse.title"));
        alert.setContentText(Application.LANG.get("alerts.set_nhse.content"));
        alert.setHeaderText(Application.LANG.get("alerts.set_nhse.header"));
        alert.initOwner(Application.PRIMARY_STAGE);
        return alert.showAndWait();
    }

    static Optional<ButtonType> notifySelectedDirectoryNoNHSE() {
        final Alert exeMissing = new Alert(Alert.AlertType.WARNING);
        exeMissing.setTitle(Application.LANG.get("alerts.not_nhse.title"));
        exeMissing.setContentText(Application.LANG.get("alerts.not_nhse.content", AppPaths.NHSE_EXECUTABLE));
        exeMissing.setHeaderText(Application.LANG.get("alerts.not_nhse.header"));
        exeMissing.initOwner(Application.PRIMARY_STAGE);
        return exeMissing.showAndWait();
    }
}
