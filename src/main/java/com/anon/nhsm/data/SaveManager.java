package com.anon.nhsm.data;

import com.anon.nhsm.AppProperties;
import com.anon.nhsm.app.Application;
import com.anon.nhsm.Main;
import com.anon.nhsm.controllers.EditIslandController;
import com.anon.nhsm.controllers.EmulatorLocalSaveController;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SaveManager {
    private static final Logger logger = LogManager.getLogger(SaveManager.class);
    public static final String UNNAMED_ISLAND = "Unnamed Island";

    private final List<SaveMetadata> islandsMetadata = new ArrayList<>();
    private SaveMetadata emulatorSaveMetadata;

    private AppProperties appProperties;
    public record Config(File emulatorSaveDirectory) {}

    private final Config config;

    public SaveManager(final AppProperties appProperties, final Config config) {
        this.appProperties = appProperties;
        this.config = config;
    }

    public AppProperties getAppProperties() {
        return appProperties;
    }

    public void setAndWriteAppProperties(final AppProperties properties) throws IOException {
        this.appProperties = Main.writeAppProperties(properties);
    }

    public SaveMetadata getEmulatorSaveMetadata() {
        return emulatorSaveMetadata;
    }

    public List<SaveMetadata> getIslandsMetadata() {
        return islandsMetadata;
    }

    public Config getConfig() {
        return config;
    }

    public void setup() throws IOException {
        final File emulatorSaveMetadataFile = new File(config.emulatorSaveDirectory(), AppPaths.SAVE_METADATA_FILE_NAME);
        if (emulatorSaveMetadataFile.exists()) {
            final SaveMetadata metadata = readMetdataFile(emulatorSaveMetadataFile);
            if (metadata != null) {
                emulatorSaveMetadata = metadata;
            }
        }

        if (emulatorSaveMetadata == null) {
            emulatorSaveMetadata = new SaveMetadata(UNNAMED_ISLAND, "", new Date(), false);
        }

        extractIslands();
    }

    public boolean promptToConvertLocalSaveIntoIslands(final File localSaveMetadataFile) throws IOException {
        logger.info("Prompting to convert emulator local save into an island");
        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(EmulatorLocalSaveController.class.getResource("emulator_local_save.fxml"));
        final DialogPane newIslandDialogPane = fxmlLoader.load();

        final EmulatorLocalSaveController controller = fxmlLoader.getController();

        final Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(newIslandDialogPane);
        dialog.setTitle("Name your Emulator Local Save as an Island");
        dialog.initOwner(Application.PRIMARY_STAGE);

        final Optional<ButtonType> clickedbutton = dialog.showAndWait();

        if (clickedbutton.isPresent() && clickedbutton.get() == ButtonType.FINISH) {
            final SaveMetadata newMetadata = new SaveMetadata(controller.getIslandName(), controller.getIslandDescription(), new Date(), false);

            logger.info("Creating new metadata for converting Emulator Local Save: " + newMetadata);
            if (!verifyMetadataNameChange("Could not convert 'Emulator Local Save' to list of Islands", newMetadata)) {
                logger.info("Could not write metadata file to Emulator Local Save directory because of name conflict: " + newMetadata.island() + " already exists");
                return false;
            }

            writeMetadataFile(localSaveMetadataFile, newMetadata);
            logger.info("Written metadata file to emulator local save: " + localSaveMetadataFile.getAbsolutePath());
            return true;
        }

        return false;
    }

    public boolean verifyMetadataNameChange(final String headerError, final SaveMetadata metadata) {
        final File checkIfExistsAlready = new File(appProperties.islandsDirectory(), metadata.island());
        if (checkIfExistsAlready.exists()) {
            showNamingConflictWarning(headerError, metadata.island());
            logger.info(headerError);
            return false;
        }
        return true;
    }

    public void swapWithLocalSave(final SaveMetadata islandMetadata) throws IOException {
        final File nhsmIslandDirectory = islandMetadata.nhsmIslandDirectory(appProperties);
        logger.info("Attempting to swap the contents of the '" + islandMetadata.island() + "' island with the emulator local save");
        if (!config.emulatorSaveDirectory().exists() && config.emulatorSaveDirectory().mkdirs()) {
            logger.info("Copying requested save data's island folder contents to the emulator's local save directory.");
            FileUtils.copyDirectory(nhsmIslandDirectory, config.emulatorSaveDirectory());
        } else {
            final File localSaveMetadataFile = new File(config.emulatorSaveDirectory(), AppPaths.SAVE_METADATA_FILE_NAME);

            if (!localSaveMetadataFile.exists() && !promptToConvertLocalSaveIntoIslands(localSaveMetadataFile)) {
                return;
            }

            final SaveMetadata localSaveMetadata = readMetdataFile(localSaveMetadataFile);

            if (localSaveMetadata != null) {
                final Path tmpPath = Paths.get(FileUtils.getTempDirectory().toString(), UUID.randomUUID().toString());

                // Deleting tmpPath if it exists, then creating it
                if (tmpPath.toFile().exists()) {
                    FileUtils.deleteDirectory(tmpPath.toFile());
                }
                Files.createDirectories(tmpPath);

                final File tmpIslandDir = new File(tmpPath.toFile(), localSaveMetadata.island());

                // Update metadata with new date timestmap
                writeMetadataFile(new File(config.emulatorSaveDirectory(), AppPaths.SAVE_METADATA_FILE_NAME), localSaveMetadata.date(new Date()));

                logger.info("Moving local save directory contents to the temp island directory: " + tmpIslandDir.getAbsolutePath());
                FileUtils.moveDirectory(config.emulatorSaveDirectory(), tmpIslandDir);

                logger.info("Copy all contents of requested saved island: " + nhsmIslandDirectory + ", to the local save directory: " + config.emulatorSaveDirectory().getAbsolutePath());
                FileUtils.copyDirectory(nhsmIslandDirectory, config.emulatorSaveDirectory());

                logger.info("Create an emulator lock file for the requested save data's island directory: " + nhsmIslandDirectory);
                final File emulatorLockFile = new File(nhsmIslandDirectory, AppPaths.EMULATOR_LOCK_FILE_NAME);
                if (!emulatorLockFile.createNewFile()) {
                    logger.error("Something went wrong, the emulator lock file could not be made.");
                }

                final File localSaveNhsmIslandDirectory = localSaveMetadata.nhsmIslandDirectory(appProperties);
                logger.info("Delete all contents of the island directory the local save file was from: " + localSaveNhsmIslandDirectory.getAbsolutePath());
                FileUtils.deleteDirectory(localSaveNhsmIslandDirectory);

                logger.info("Move all contents from the temp island directory to the island directory the local save file was from: " + localSaveNhsmIslandDirectory.getAbsolutePath());
                FileUtils.moveDirectory(tmpIslandDir, localSaveNhsmIslandDirectory);

                logger.info("Delete the temp directory: " + tmpPath.toFile().getAbsolutePath());
                FileUtils.deleteDirectory(tmpPath.toFile());

                this.emulatorSaveMetadata = islandMetadata;
                extractIslands();
                logger.info("Successfully applied '" + islandMetadata.island() + "' to the local save directory");
            }
        }
    }

    public void extractIslands() throws IOException {
        islandsMetadata.clear();
        logger.info("Extracting island data from acnh_islands directory.");

        if (appProperties.islandsDirectory().exists()) {
            final File[] directories = appProperties.islandsDirectory().listFiles((current, name) -> {
                final File islandDir = new File(current, name);

                if (islandDir.isFile()) {
                    return false;
                }

                final String[] subFiles = islandDir.list();

                if (subFiles == null) {
                    return false;
                }

                final List<String> fileNames = Arrays.asList(subFiles);
                final boolean isValidIsland = fileNames.contains(AppPaths.SAVE_METADATA_FILE_NAME) || fileNames.contains(AppPaths.MAIN_DAT);
                return isValidIsland && !islandDir.getName().equals(AppPaths.TMP_DIR_NAME) && islandDir.isDirectory();
            });

            if (directories != null) {
                for (final File islandDir : directories) {
                    final File metadata = new File(islandDir, AppPaths.SAVE_METADATA_FILE_NAME);
                    final File emulatorLockFile = new File(islandDir, AppPaths.EMULATOR_LOCK_FILE_NAME);

                    final SaveMetadata islandSaveMetadata = readMetdataFile(metadata);

                    if (emulatorSaveMetadata.equals(islandSaveMetadata)) {
                        continue;
                    }

                    if (islandSaveMetadata != null) {
                        islandsMetadata.add(islandSaveMetadata.lock(emulatorLockFile.exists()));
                    } else {
                        try {
                            final BasicFileAttributes attributes = Files.readAttributes(islandDir.toPath(), BasicFileAttributes.class);
                            final Date creationDate = new Date(attributes.creationTime().to(TimeUnit.MILLISECONDS));
                            final SaveMetadata saveMetadata = new SaveMetadata(islandDir.getName(), "", creationDate, emulatorLockFile.exists());
                            writeMetadataFile(metadata, saveMetadata);
                            islandsMetadata.add(saveMetadata);
                        } catch (final IOException e) {
                            throw new IOException("Could not read basic file attributes at save location: " + islandDir.getAbsolutePath() + ", for island: " + islandDir.getName(), e);
                        }
                    }
                }
            }
        }
    }

    public void showNamingConflictWarning(final String headerText, final String newIslandName) {
        final Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText("The name '" + newIslandName + "' you tried to give for this island already exists.");
        alert.setHeaderText(headerText);
        alert.initOwner(Application.PRIMARY_STAGE);
        alert.showAndWait();
    }

    public SaveMetadata editIslandDetails(final SaveMetadata oldSaveMetadata) throws IOException {
        try {
            logger.info("Handling edit of" + oldSaveMetadata.island());
            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(EditIslandController.class.getResource("edit_island.fxml"));
            final DialogPane newIslandDialogPane = fxmlLoader.load();
            final EditIslandController controller = fxmlLoader.getController();
            controller.setIslandName(oldSaveMetadata.island());
            controller.setIslandDescription(oldSaveMetadata.description());

            final Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(newIslandDialogPane);
            dialog.setTitle("Edit '" + oldSaveMetadata.island() + "' Island");
            dialog.initOwner(Application.PRIMARY_STAGE);

            final Optional<ButtonType> clickedbutton = dialog.showAndWait();

            if (clickedbutton.isPresent() && clickedbutton.get() == ButtonType.FINISH) {
                final SaveMetadata updatedSaveMetadata = createSaveData(controller.getIslandName(), controller.getIslandDescription());
                if (updateIslandDetails(oldSaveMetadata, updatedSaveMetadata)) {
                    return updatedSaveMetadata;
                }
            }
        } catch (final IOException e) {
            throw new IOException("Something went wrong editing the details of an island.", e);
        }

        return oldSaveMetadata;
    }

    public boolean updateIslandDetails(final SaveMetadata oldSaveMetadata, final SaveMetadata newSaveMetadata) throws IOException {
        logger.info("Updating island details from: '" + oldSaveMetadata.island() + "' into: '" + newSaveMetadata.island() + "'");
        final File oldSaveDir = oldSaveMetadata.nhsmIslandDirectory(appProperties);
        final File newSaveDir = newSaveMetadata.nhsmIslandDirectory(appProperties);

        try {
            if (!oldSaveMetadata.island().equals(newSaveMetadata.island())) {
                if (newSaveDir.exists()) {
                    showNamingConflictWarning("Could not edit '" + oldSaveMetadata.island() + "' island", newSaveMetadata.island());
                    logger.info("Could not edit '" + oldSaveMetadata.island() + "' island since the new name '" + newSaveMetadata.island() + "' already exists");
                    return false;
                }

                FileUtils.forceMkdir(newSaveDir);
                FileUtils.copyDirectory(oldSaveDir, newSaveDir);
                FileUtils.deleteDirectory(oldSaveDir);
            }

            final File newMetadataFile = new File(newSaveDir, AppPaths.SAVE_METADATA_FILE_NAME);
            writeMetadataFile(newMetadataFile, newSaveMetadata);
            extractIslands();
        } catch (final IOException e) {
            throw new IOException("Could not update details of save location: " + oldSaveDir.getAbsolutePath() + ", for island: " + oldSaveMetadata.island() + ", with new island name: " + newSaveMetadata.island(), e);
        }
        return true;
    }

    public boolean createNewIsland(final String islandName, final String islandDescription) throws IOException {
        final File newIslandDir = new File(appProperties.islandsDirectory(), islandName);
        logger.info("Creating new island: " + islandName + ", with description: " + islandDescription);

        try {
            final File newIslandMetadata = new File(newIslandDir, AppPaths.SAVE_METADATA_FILE_NAME);
            final SaveMetadata metadata = new SaveMetadata(islandName, islandDescription, new Date(), false);

            if (!verifyMetadataNameChange("Could not create new '" + metadata.island() + "' island", metadata)) {
                return false;
            }

            FileUtils.forceMkdir(newIslandDir);
            writeMetadataFile(newIslandMetadata, metadata);
            extractIslands();
        } catch (final IOException e) {
            throw new IOException("Could not create new island and save to: " + newIslandDir.getAbsolutePath() + ", for island: " + islandName, e);
        }
        return true;
    }

    public void duplicateIsland(final SaveMetadata islandToDuplicate) throws IOException {
        final String copiedName = islandToDuplicate.island().replaceFirst("(\\.[^.]*)?$", "-copy$0");
        logger.info("Duplicating island: " + islandToDuplicate.island() + ", duplicated island name will be: " + copiedName);

        final File oldIslandDir = islandToDuplicate.nhsmIslandDirectory(appProperties);
        final File duplicateIslandDir = new File(appProperties.islandsDirectory(), copiedName);

        try {
            final SaveMetadata duplicateIslandMetadata = new SaveMetadata(copiedName, islandToDuplicate.description(), new Date(), false);

            if (!verifyMetadataNameChange("Could not duplicate '" + duplicateIslandMetadata.island() + "' island", duplicateIslandMetadata)) {
                return;
            }

            FileUtils.forceMkdir(duplicateIslandDir);
            FileUtils.copyDirectory(oldIslandDir, duplicateIslandDir);

            final File metadata = new File(duplicateIslandDir, AppPaths.SAVE_METADATA_FILE_NAME);
            writeMetadataFile(metadata, duplicateIslandMetadata);
            extractIslands();
        } catch (final IOException e) {
            throw new IOException("Could not create duplicate island and save to: " + duplicateIslandDir.getAbsolutePath() + ", for new duplicate island: " + copiedName, e);
        }

    }

    public void deleteIsland(final SaveMetadata saveMetadata) throws IOException {
        final File islandDirectory = saveMetadata.nhsmIslandDirectory(appProperties);
        try {
            logger.info("Deleting island: " + saveMetadata.island());
            FileUtils.deleteDirectory(islandDirectory);
            extractIslands();
        } catch (final IOException e) {
            throw new IOException("Could not delete island at save location: " + islandDirectory.getAbsolutePath() + ", for island: " + saveMetadata.island(), e);
        }
    }

    private boolean promptSelectNHSEExecutable(final Stage primaryStage) throws IOException {
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Set NHSE Executable Directory");
        alert.setContentText("In order to edit the save data of an island, you must select the directory of your NHSE executable. Press OK to select the directory or cancel to stop.");
        alert.setHeaderText("Select the NHSE directory to proceed");
        alert.initOwner(Application.PRIMARY_STAGE);
        final Optional<ButtonType> type = alert.showAndWait();

        if (type.isPresent() && type.get() == ButtonType.OK) {
            final DirectoryChooser directoryChooser = new DirectoryChooser();
            final File selectedDirectory = directoryChooser.showDialog(primaryStage);

            if (selectedDirectory != null) {
                final File executable = new File(selectedDirectory, AppPaths.NHSE_EXECUTABLE);

                if (executable.exists()) {
                    setAndWriteAppProperties(this.appProperties.copy().nhsExecutable(executable).build());
                    return true;
                }

                final Alert exeMissing = new Alert(Alert.AlertType.WARNING);
                exeMissing.setTitle("Warning");
                exeMissing.setContentText("The selected directory does not contain an NHSE executable with the following name: " + AppPaths.NHSE_EXECUTABLE);
                exeMissing.setHeaderText("Cannot use Save Editor");
                exeMissing.initOwner(Application.PRIMARY_STAGE);
                exeMissing.showAndWait();
            }
        }

        return false;
    }

    public void openSaveEditorFor(final Stage primaryStage, final Path islandDirectory) throws IOException {
        if (appProperties.nhsExecutable() == null) {
            if (!promptSelectNHSEExecutable(primaryStage)) {
                return;
            }
        } else {
            if (!appProperties.nhsExecutable().exists()) {
                final Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setContentText("Your previously chosen directory for the NHSE executable no longer exists or the executable is missing. Next prompt will have you select the directory again.");
                alert.setHeaderText("Re-select NHSE directory");
                alert.initOwner(Application.PRIMARY_STAGE);
                alert.showAndWait();

                if (!promptSelectNHSEExecutable(primaryStage)) {
                    return;
                }
            }
        }

        try {
            final Path mainDat = islandDirectory.resolve(AppPaths.MAIN_DAT);

            if (mainDat.toFile().exists()) {
                Application.ANCHOR_PANE.setDisable(true);
                final Process process = new ProcessBuilder(appProperties.nhsExecutable().getAbsolutePath(), mainDat.toString()).start();
                process.onExit().thenAccept(p -> Application.ANCHOR_PANE.setDisable(false));
            } else {
                final Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setContentText("The '" + islandDirectory.toFile().getName() + "' island does not have a main.dat file, so the Save Editor cannot open.");
                alert.setHeaderText("Cannot use Save Editor");
                alert.initOwner(Application.PRIMARY_STAGE);
                alert.showAndWait();
            }
        } catch (final IOException e) {
            throw new IOException("Something went wrong when starting the NHSE process.", e);
        }

    }

    public SaveMetadata createSaveData(final String islandName, final String islandDescription) {
        return new SaveMetadata(islandName, islandDescription, new Date(), false);
    }

    public void writeMetadataFile(final File file, final SaveMetadata saveMetadata) throws IOException {
        if (!file.exists() || file.delete()) {
            try (final FileWriter fileWriter = new FileWriter(file)) {
                final Gson gson = Main.GSON.create();
                final JsonElement jsonElement = gson.toJsonTree(saveMetadata);
                jsonElement.getAsJsonObject().addProperty("version", Main.DATA_VERSION.toString());
                jsonElement.getAsJsonObject().remove("emulatorLocked");
                gson.toJson(jsonElement, fileWriter);
            } catch (final IOException e) {
                throw new IOException("Could not write metadata to file: " + file.getAbsolutePath() + ", for island: " + saveMetadata.island(), e);
            }
        }
    }

    public SaveMetadata readMetdataFile(final File file) throws IOException {
        if (file.exists()) {
            try (final FileReader fileReader = new FileReader(file)) {
                final Type type = new TypeToken<SaveMetadata>(){}.getType();
                final Gson gson = Main.GSON.create();
                return gson.fromJson(fileReader, type);
            } catch (final IOException e) {
                throw new IOException("Could not read metadata from file: " + file.getAbsolutePath(), e);
            }
        }

        return null;
    }
}