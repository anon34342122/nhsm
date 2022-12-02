package com.anon.nhsm.data;

import com.anon.nhsm.app.AppProperties;
import com.anon.nhsm.app.Application;
import com.anon.nhsm.edit_island.EditIslandController;
import com.anon.nhsm.emulator_local_save.EmulatorLocalSaveController;
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

    private final List<SaveData> islandsMetadata = new ArrayList<>();
    private SaveData emulatorSaveMetadata;

    private AppProperties properties;
    public record Config(File emulatorSaveDirectory) {}

    private final Config config;

    public SaveManager(final AppProperties properties, final Config config) {
        this.properties = properties;
        this.config = config;
    }

    public AppProperties getProperties() {
        return properties;
    }

    public void setAndWriteAppProperties(final AppProperties properties) throws IOException {
        this.properties = Application.writeAppProperties(properties);
    }

    public SaveData getEmulatorSaveMetadata() {
        return emulatorSaveMetadata;
    }

    public List<SaveData> getIslandsMetadata() {
        return islandsMetadata;
    }

    public Config getConfig() {
        return config;
    }

    public void setup() throws IOException {
        final File emulatorSaveMetadataFile = new File(config.emulatorSaveDirectory(), Utils.SAVE_METADATA_FILE);
        if (emulatorSaveMetadataFile.exists()) {
            final SaveData metadata = readMetdataFile(emulatorSaveMetadataFile);
            if (metadata != null) {
                emulatorSaveMetadata = metadata;
            }
        }

        if (emulatorSaveMetadata == null) {
            emulatorSaveMetadata = new SaveData(UNNAMED_ISLAND, config.emulatorSaveDirectory().getAbsolutePath(), "", new Date());
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
            final SaveData newMetadata = new SaveData(controller.getIslandName(), new File(properties.islandsDirectory(), controller.getIslandName()).getAbsolutePath(), controller.getIslandDescription(), new Date());

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

    public boolean verifyMetadataNameChange(final String headerError, final SaveData metadata) {
        final File checkIfExistsAlready = new File(properties.islandsDirectory(), metadata.island());
        if (checkIfExistsAlready.exists()) {
            showNamingConflictWarning(headerError, metadata.island());
            logger.info(headerError);
            return false;
        }
        return true;
    }

    public void swapWithLocalSave(final SaveData requestedSaveData) throws IOException {
        logger.info("Attempting to swap the contents of the '" + requestedSaveData.island() + "' island with the emulator local save");
        if (!config.emulatorSaveDirectory().exists() && config.emulatorSaveDirectory().mkdirs()) {
            logger.info("Copying requested save data's island folder contents to the emulator's local save directory.");
            FileUtils.copyDirectory(new File(requestedSaveData.folder()), config.emulatorSaveDirectory());
        } else {
            final File localSaveMetadataFile = new File(config.emulatorSaveDirectory(), Utils.SAVE_METADATA_FILE);

            if (!localSaveMetadataFile.exists() && !promptToConvertLocalSaveIntoIslands(localSaveMetadataFile)) {
                return;
            }

            final SaveData localSaveMetadata = readMetdataFile(localSaveMetadataFile);

            if (localSaveMetadata != null) {
                final Path tmpPath = Paths.get(FileUtils.getTempDirectory().toString(), UUID.randomUUID().toString());

                // Deleting tmpPath if it exists, then creating it
                if (tmpPath.toFile().exists()) {
                    FileUtils.deleteDirectory(tmpPath.toFile());
                }
                Files.createDirectories(tmpPath);

                final File tmpIslandDir = new File(tmpPath.toFile(), localSaveMetadata.island());

                // Update metadata with new date timestmap
                writeMetadataFile(new File(config.emulatorSaveDirectory(), Utils.SAVE_METADATA_FILE), SaveData.copyWithNewDate(localSaveMetadata, new Date()));

                logger.info("Moving local save directory contents to the temp island directory: " + tmpIslandDir.getAbsolutePath());
                FileUtils.moveDirectory(config.emulatorSaveDirectory(), tmpIslandDir);

                logger.info("Copy all contents of requested saved island: " + requestedSaveData.folder() + ", to the local save directory: " + config.emulatorSaveDirectory().getAbsolutePath());
                FileUtils.copyDirectory(new File(requestedSaveData.folder()), config.emulatorSaveDirectory());

                final File islandSaveDir = new File(localSaveMetadata.folder());
                logger.info("Delete all contents of the island directory the local save file was from: " + localSaveMetadata.folder());
                FileUtils.deleteDirectory(islandSaveDir);

                logger.info("Move all contents from the temp island directory to the island directory the local save file was from: " + islandSaveDir.getAbsolutePath());
                FileUtils.moveDirectory(tmpIslandDir, islandSaveDir);

                logger.info("Delete the temp directory: " + tmpPath.toFile().getAbsolutePath());
                FileUtils.deleteDirectory(tmpPath.toFile());

                this.emulatorSaveMetadata = requestedSaveData;
                extractIslands();
                logger.info("Successfully applied '" + requestedSaveData.island() + "' to the local save directory");
            }
        }
    }

    public void extractIslands() throws IOException {
        islandsMetadata.clear();
        logger.info("Extracting island data from acnh_islands directory.");

        if (properties.islandsDirectory().exists()) {
            final File[] directories = properties.islandsDirectory().listFiles((current, name) -> {
                final File islandDir = new File(current, name);

                if (islandDir.isFile()) {
                    return false;
                }

                final String[] subFiles = islandDir.list();

                if (subFiles == null) {
                    return false;
                }

                final List<String> fileNames = Arrays.asList(subFiles);
                final boolean isValidIsland = fileNames.contains(Utils.SAVE_METADATA_FILE) || fileNames.contains(Utils.MAIN_DAT);
                return isValidIsland && !islandDir.getName().equals(Utils.TMP_DIR_NAME) && islandDir.isDirectory();
            });

            if (directories != null) {
                for (final File islandDir : directories) {
                    final File metadata = new File(islandDir, Utils.SAVE_METADATA_FILE);
                    final SaveData saveDataFromMetadata = readMetdataFile(metadata);

                    if (emulatorSaveMetadata.equals(saveDataFromMetadata)) {
                        continue;
                    }

                    if (saveDataFromMetadata != null) {
                        islandsMetadata.add(saveDataFromMetadata);
                    } else {
                        try {
                            final BasicFileAttributes attributes = Files.readAttributes(islandDir.toPath(), BasicFileAttributes.class);
                            final Date creationDate = new Date(attributes.creationTime().to(TimeUnit.MILLISECONDS));
                            final SaveData saveData = new SaveData(islandDir.getName(), islandDir.getPath(), "", creationDate);
                            writeMetadataFile(metadata, saveData);
                            islandsMetadata.add(saveData);
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

    public SaveData editIslandDetails(final SaveData oldSaveData) throws IOException {
        try {
            logger.info("Handling edit of" + oldSaveData.island());
            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(EditIslandController.class.getResource("edit_island.fxml"));
            final DialogPane newIslandDialogPane = fxmlLoader.load();
            final EditIslandController controller = fxmlLoader.getController();
            controller.setIslandName(oldSaveData.island());
            controller.setIslandDescription(oldSaveData.description());

            final Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(newIslandDialogPane);
            dialog.setTitle("Edit '" + oldSaveData.island() + "' Island");
            dialog.initOwner(Application.PRIMARY_STAGE);

            final Optional<ButtonType> clickedbutton = dialog.showAndWait();

            if (clickedbutton.isPresent() && clickedbutton.get() == ButtonType.FINISH) {
                final SaveData updatedSaveData = createSaveData(controller.getIslandName(), controller.getIslandDescription());
                if (updateIslandDetails(oldSaveData, updatedSaveData)) {
                    return updatedSaveData;
                }
            }
        } catch (final IOException e) {
            throw new IOException("Something went wrong editing the details of an island.", e);
        }

        return oldSaveData;
    }

    public boolean updateIslandDetails(final SaveData oldSaveData, final SaveData newSaveData) throws IOException {
        logger.info("Updating island details from: '" + oldSaveData.island() + "' into: '" + newSaveData.island() + "'");
        try {
            final File oldSaveDir = new File(oldSaveData.folder());
            final File newSaveDir = new File(properties.islandsDirectory(), newSaveData.island());

            if (!oldSaveData.island().equals(newSaveData.island())) {
                if (newSaveDir.exists()) {
                    showNamingConflictWarning("Could not edit '" + oldSaveData.island() + "' island", newSaveData.island());
                    logger.info("Could not edit '" + oldSaveData.island() + "' island since the new name '" + newSaveData.island() + "' already exists");
                    return false;
                }

                FileUtils.forceMkdir(newSaveDir);
                FileUtils.copyDirectory(oldSaveDir, newSaveDir);
                FileUtils.deleteDirectory(oldSaveDir);
            }

            final File newMetadataFile = new File(newSaveDir, Utils.SAVE_METADATA_FILE);
            writeMetadataFile(newMetadataFile, newSaveData);
            extractIslands();
        } catch (final IOException e) {
            throw new IOException("Could not update details of save location: " + oldSaveData.folder() + ", for island: " + oldSaveData.island() + ", with new island name: " + newSaveData.island(), e);
        }
        return true;
    }

    public boolean createNewIsland(final String islandName, final String islandDescription) throws IOException {
        final File newIslandDir = new File(properties.islandsDirectory(), islandName);
        logger.info("Creating new island: " + islandName + ", with description: " + islandDescription);

        try {
            final File newIslandMetadata = new File(newIslandDir, Utils.SAVE_METADATA_FILE);
            final SaveData metadata = new SaveData(islandName, newIslandDir.getAbsolutePath(), islandDescription, new Date());

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

    public void duplicateIsland(final SaveData islandToDuplicate) throws IOException {
        final String copiedName = islandToDuplicate.island().replaceFirst("(\\.[^.]*)?$", "-copy$0");
        logger.info("Duplicating island: " + islandToDuplicate.island() + ", duplicated island name will be: " + copiedName);

        final File oldIslandDir = new File(islandToDuplicate.folder());
        final File duplicateIslandDir = new File(properties.islandsDirectory(), copiedName);

        try {
            final SaveData duplicateIslandMetadata = new SaveData(copiedName, duplicateIslandDir.getAbsolutePath(), islandToDuplicate.description(), new Date());

            if (!verifyMetadataNameChange("Could not duplicate '" + duplicateIslandMetadata.island() + "' island", duplicateIslandMetadata)) {
                return;
            }

            FileUtils.forceMkdir(duplicateIslandDir);
            FileUtils.copyDirectory(oldIslandDir, duplicateIslandDir);

            final File metadata = new File(duplicateIslandDir, Utils.SAVE_METADATA_FILE);
            writeMetadataFile(metadata, duplicateIslandMetadata);
            extractIslands();
        } catch (final IOException e) {
            throw new IOException("Could not create duplicate island and save to: " + duplicateIslandDir.getAbsolutePath() + ", for new duplicate island: " + copiedName, e);
        }

    }

    public void deleteIsland(final SaveData saveData) throws IOException {
        try {
            logger.info("Deleting island: " + saveData.island());
            FileUtils.deleteDirectory(new File(saveData.folder()));
            extractIslands();
        } catch (final IOException e) {
            throw new IOException("Could not delete island at save location: " + saveData.folder() + ", for island: " + saveData.island(), e);
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
            final File executable = new File(selectedDirectory, Utils.NHSE_EXECUTABLE);

            if (executable.exists()) {
                setAndWriteAppProperties(this.properties.copy().nhsExecutable(executable).build());
                return true;
            }
        }

        return false;
    }

    public void openSaveEditorFor(final Stage primaryStage, final Path islandDirectory) throws IOException {
        if (properties.nhsExecutable() == null) {
            if (!promptSelectNHSEExecutable(primaryStage)) {
                final Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setContentText("The selected directory does not contain an NHSE executable with the following name: " + Utils.NHSE_EXECUTABLE);
                alert.setHeaderText("Cannot use Save Editor");
                alert.initOwner(Application.PRIMARY_STAGE);
                alert.showAndWait();
                return;
            }
        } else {
            if (!properties.nhsExecutable().exists()) {
                final Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setContentText("Your previously chosen directory for the NHSE executable no longer exists or the executable is missing. Next prompt will have you select the directory again.");
                alert.setHeaderText("Re-select NHSE directory");
                alert.initOwner(Application.PRIMARY_STAGE);
                alert.showAndWait();

                if (!promptSelectNHSEExecutable(primaryStage)) {
                    final Alert alert1 = new Alert(Alert.AlertType.WARNING);
                    alert1.setTitle("Warning");
                    alert1.setContentText("The selected directory does not contain an NHSE executable with the following name: " + Utils.NHSE_EXECUTABLE);
                    alert1.setHeaderText("Cannot use Save Editor");
                    alert1.initOwner(Application.PRIMARY_STAGE);
                    alert1.showAndWait();
                    return;
                }
            }
        }

        try {
            final Path mainDat = islandDirectory.resolve(Utils.MAIN_DAT);

            if (mainDat.toFile().exists()) {
                Application.ANCHOR_PANE.setDisable(true);
                final Process process = new ProcessBuilder(properties.nhsExecutable().getAbsolutePath(), mainDat.toString()).start();
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

    public SaveData createSaveData(final String islandName, final String islandDescription) {
        return new SaveData(islandName, new File(properties.islandsDirectory(), islandName).getAbsolutePath(), islandDescription, new Date());
    }

    public static void writeMetadataFile(final File file, final SaveData saveData) throws IOException {
        if (!file.exists() || file.delete()) {
            try (final FileWriter fileWriter = new FileWriter(file)) {
                final Gson gson = Application.GSON.create();
                final JsonElement jsonElement = gson.toJsonTree(saveData);
                jsonElement.getAsJsonObject().addProperty("version", Application.DATA_VERSION.toString());
                gson.toJson(jsonElement, fileWriter);
            } catch (final IOException e) {
                throw new IOException("Could not write metadata to file: " + file.getAbsolutePath() + ", for island: " + saveData.island(), e);
            }
        }
    }

    public static SaveData readMetdataFile(final File file) throws IOException {
        if (file.exists()) {
            try (final FileReader fileReader = new FileReader(file)) {
                final Type type = new TypeToken<SaveData>(){}.getType();
                final Gson gson = Application.GSON.create();
                return gson.fromJson(fileReader, type);
            } catch (final IOException e) {
                throw new IOException("Could not read metadata from file: " + file.getAbsolutePath(), e);
            }
        }

        return null;
    }
}