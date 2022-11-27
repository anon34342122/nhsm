package com.anon.nhsm.data;

import com.anon.nhsm.app.Application;
import com.anon.nhsm.yuzu_island.YuzuIslandController;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.anon.nhsm.edit_island.EditIslandController;
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
    public static final String APPDATA_ENV = "APPDATA";
    public static List<SaveData> ISLAND_SAVES = new ArrayList<SaveData>();
    public static SaveData LOCAL_YUZU_SAVE;
    public static File YUZU_SAVE_DIRECTORY;
    public static File APPDATA_DIRECTORY;
    public static File ISLANDS_DIRECTORY;

    private static SaveManagerProperties PROPERTIES;

    public static void setup() throws IOException {
        final String appDataLoc = System.getenv(APPDATA_ENV);
        APPDATA_DIRECTORY = new File(appDataLoc);

        if (APPDATA_DIRECTORY.exists()) {
            YUZU_SAVE_DIRECTORY = new File(appDataLoc + ACNHConsts.SAVE_PATH);
            final File localYuzuSaveMetadata = new File(YUZU_SAVE_DIRECTORY, ACNHConsts.SAVE_METADATA_FILE);
            if (localYuzuSaveMetadata.exists()) {
                final SaveData metadata = readMetdataFile(localYuzuSaveMetadata);
                if (metadata != null) {
                    LOCAL_YUZU_SAVE = metadata;
                }
            }

            if (LOCAL_YUZU_SAVE == null) {
                LOCAL_YUZU_SAVE = new SaveData("Yuzu Island", YUZU_SAVE_DIRECTORY.getAbsolutePath(), "", new Date());
            }

            extractIslands();
            validateAndLoadProperties();
        }
    }

    private static void validateAndLoadProperties() throws IOException {
        final File propertiesFile = new File(ISLANDS_DIRECTORY, ACNHConsts.PROPERTIES_FILE);
        final SaveManagerProperties properties = readPropertiesFile(propertiesFile);

        if (properties != null) {
            final File nhseExecutableFile = new File(properties.pathToNHSExecutable());

            if (nhseExecutableFile.exists()) {
                PROPERTIES = properties;
            } else { // Delete because NHSE executable file no longer exists
                try {
                    FileUtils.delete(propertiesFile);
                } catch (IOException e) {
                    throw new IOException("Failed to delete properties file in acnh_islands directory.", e);
                }
            }
        }
    }

    public static boolean promptToConvertLocalYuzuIntoIslands(final File yuzuMetadataFile) throws IOException {
        logger.info("Prompting to convert local yuzu save into an island");
        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(YuzuIslandController.class.getResource("yuzu_island.fxml"));
        DialogPane newIslandDialogPane = fxmlLoader.load();

        YuzuIslandController controller = fxmlLoader.getController();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(newIslandDialogPane);
        dialog.setTitle("Name your Local Yuzu Save as an Island");
        dialog.initOwner(Application.PRIMARY_STAGE);

        Optional<ButtonType> clickedbutton = dialog.showAndWait();

        if (clickedbutton.isPresent() && clickedbutton.get() == ButtonType.FINISH) {
            final SaveData newMetadata = new SaveData(controller.getIslandName(), new File(ISLANDS_DIRECTORY, controller.getIslandName()).getAbsolutePath(), controller.getIslandDescription(), new Date());

            logger.info("Creating new metadata for converting Local Yuzu Save: " + newMetadata);
            if (!verifyMetadataNameChange("Could not convert 'Yuzu Local Save' to list of Islands", newMetadata)) {
                logger.info("Could not write metadata file to Local Yuzu Save because of name conflict: " + newMetadata.island() + " already exists");
                return false;
            }

            writeMetadataFile(yuzuMetadataFile, newMetadata);
            logger.info("Written metadata file to Local Yuzu Save: " + yuzuMetadataFile.getAbsolutePath());
            return true;
        }

        return false;
    }

    public static boolean verifyMetadataNameChange(final String headerError, final SaveData metadata) {
        final File checkIfExistsAlready = new File(ISLANDS_DIRECTORY, metadata.island());
        if (checkIfExistsAlready.exists()) {
            showNamingConflictWarning(headerError, metadata.island());
            logger.info(headerError);
            return false;
        }
        return true;
    }

    public static void applyToYuzuSaveFolder(final SaveData requestedSaveData) throws IOException {
        logger.info("Attempting to apply '" + requestedSaveData.island() + "' to the yuzu save folder");
        if (!YUZU_SAVE_DIRECTORY.exists() && YUZU_SAVE_DIRECTORY.mkdirs()) {
            logger.info("Copying requested save data's island folder contents to the Local Yuzu Save directory.");
            FileUtils.copyDirectory(new File(requestedSaveData.folder()), YUZU_SAVE_DIRECTORY);
        } else {
            final File yuzuMetadataFile = new File(YUZU_SAVE_DIRECTORY, ACNHConsts.SAVE_METADATA_FILE);

            if (!yuzuMetadataFile.exists() && !promptToConvertLocalYuzuIntoIslands(yuzuMetadataFile)) {
                return;
            }

            final SaveData yuzuSaveMetadata = readMetdataFile(yuzuMetadataFile);

            if (yuzuSaveMetadata != null) {
                final Path tmpPath = Paths.get(FileUtils.getTempDirectory().toString(), UUID.randomUUID().toString());

                // Deleting tmpPath if it exists, then creating it
                if (tmpPath.toFile().exists()) {
                    FileUtils.deleteDirectory(tmpPath.toFile());
                }
                Files.createDirectories(tmpPath);

                final File tmpIslandDir = new File(tmpPath.toFile(), yuzuSaveMetadata.island());

                // Update metadata with new date timestmap
                writeMetadataFile(new File(YUZU_SAVE_DIRECTORY, ACNHConsts.SAVE_METADATA_FILE), SaveData.copyWithNewDate(yuzuSaveMetadata, new Date()));

                logger.info("Moving yuzu save directory contents to the temp island directory: " + tmpIslandDir.getAbsolutePath());
                FileUtils.moveDirectory(YUZU_SAVE_DIRECTORY, tmpIslandDir);

                logger.info("Copy all contents of requested saved island: " + requestedSaveData.folder() + ", to the local yuzu save directory: " + YUZU_SAVE_DIRECTORY.getAbsolutePath());
                FileUtils.copyDirectory(new File(requestedSaveData.folder()), YUZU_SAVE_DIRECTORY);

                final File islandSaveDir = new File(yuzuSaveMetadata.folder());
                logger.info("Delete all contents of the island directory the yuzu save file was from: " + yuzuSaveMetadata.folder());
                FileUtils.deleteDirectory(islandSaveDir);

                logger.info("Move all contents from the temp island directory to the island directory the yuzu save file was from: " + islandSaveDir.getAbsolutePath());
                FileUtils.moveDirectory(tmpIslandDir, islandSaveDir);

                logger.info("Delete the temp directory: " + tmpPath.toFile().getAbsolutePath());
                FileUtils.deleteDirectory(tmpPath.toFile());

                LOCAL_YUZU_SAVE = requestedSaveData;
                extractIslands();
                logger.info("Successfully applied '" + requestedSaveData.island() + "' to the yuzu save folder");
            }
        }
    }

    public static void writePropertiesFile(final File file, final SaveManagerProperties properties) throws IOException {
        try {
            if (file.exists()) {
                FileUtils.delete(file);
            }
        } catch (IOException e) {
            throw new IOException("Could not delete old Save Manager properties file: " + file.getAbsolutePath(), e);
        }

        try (final FileWriter fileWriter = new FileWriter(file)) {
            final Gson gson = new Gson();
            gson.toJson(properties, fileWriter);
        } catch (IOException e) {
            throw new IOException("Could not write Save Manager properties to file: " + file.getAbsolutePath(), e);
        }
    }

    public static SaveManagerProperties readPropertiesFile(final File file) throws IOException {
        if (file.exists()) {
            try (final FileReader fileReader = new FileReader(file)) {
                final Type type = new TypeToken<SaveManagerProperties>(){}.getType();
                final Gson gson = new Gson();
                return gson.fromJson(fileReader, type);
            } catch (IOException e) {
                throw new IOException("Could not read Save Manager properties from file: " + file.getAbsolutePath(), e);
            }
        }

        return null;
    }

    public static void writeMetadataFile(final File file, final SaveData saveData) throws IOException {
        if (!file.exists() || file.delete()) {
            try (final FileWriter fileWriter = new FileWriter(file)) {
                final Gson gson = new Gson();
                gson.toJson(saveData, fileWriter);
            } catch (IOException e) {
                throw new IOException("Could not write metadata to file: " + file.getAbsolutePath() + ", for island: " + saveData.island(), e);
            }
        }
    }

    public static SaveData readMetdataFile(final File file) throws IOException {
        if (file.exists()) {
            try (final FileReader fileReader = new FileReader(file)) {
                final Type type = new TypeToken<SaveData>(){}.getType();
                final Gson gson = new Gson();
                return gson.fromJson(fileReader, type);
            } catch (IOException e) {
                throw new IOException("Could not read metadata from file: " + file.getAbsolutePath(), e);
            }
        }

        return null;
    }

    public static void extractIslands() throws IOException {
        ISLAND_SAVES.clear();
        ISLANDS_DIRECTORY = new File(APPDATA_DIRECTORY.getAbsolutePath(), ACNHConsts.ACNH_ISLANDS_DIR);

        logger.info("Extracting island data from acnh_islands directory.");

        if (ISLANDS_DIRECTORY.exists()) {
            File[] directories = ISLANDS_DIRECTORY.listFiles((current, name) -> {
                final File islandDir = new File(current, name);

                if (islandDir.isFile()) {
                    return false;
                }

                final String[] subFiles = islandDir.list();

                if (subFiles == null) {
                    return false;
                }

                final List<String> fileNames = Arrays.asList(subFiles);
                final boolean isValidIsland = fileNames.contains(ACNHConsts.SAVE_METADATA_FILE) || fileNames.contains(ACNHConsts.MAIN_DAT);
                return isValidIsland && !islandDir.getName().equals(ACNHConsts.TMP_DIR_NAME) && islandDir.isDirectory();
            });

            if (directories != null) {
                for (final File islandDir : directories) {
                    final File metadata = new File(islandDir, ACNHConsts.SAVE_METADATA_FILE);
                    final SaveData saveDataFromMetadata = readMetdataFile(metadata);

                    if (LOCAL_YUZU_SAVE.equals(saveDataFromMetadata)) {
                        continue;
                    }

                    if (saveDataFromMetadata != null) {
                        ISLAND_SAVES.add(saveDataFromMetadata);
                    } else {
                        try {
                            final BasicFileAttributes attributes = Files.readAttributes(islandDir.toPath(), BasicFileAttributes.class);
                            final Date creationDate = new Date(attributes.creationTime().to(TimeUnit.MILLISECONDS));
                            final SaveData saveData = new SaveData(islandDir.getName(), islandDir.getPath(), "", creationDate);
                            writeMetadataFile(metadata, saveData);
                            ISLAND_SAVES.add(saveData);
                        } catch (IOException e) {
                            throw new IOException("Could not read basic file attributes at save location: " + islandDir.getAbsolutePath() + ", for island: " + islandDir.getName(), e);
                        }
                    }
                }
            }
        }
    }

    public static SaveData createSaveData(final String islandName, final String islandDescription) {
        return new SaveData(islandName, new File(ISLANDS_DIRECTORY, islandName).getAbsolutePath(), islandDescription, new Date());
    }

    public static void showNamingConflictWarning(final String headerText, final String newIslandName) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText("The name '" + newIslandName + "' you tried to give for this island already exists.");
        alert.setHeaderText(headerText);
        alert.initOwner(Application.PRIMARY_STAGE);
        alert.showAndWait();
    }

    public static SaveData editIslandDetails(SaveData oldSaveData) throws IOException {
        try {
            logger.info("Handling edit of" + oldSaveData.island());
            final FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(EditIslandController.class.getResource("edit_island.fxml"));
            DialogPane newIslandDialogPane = fxmlLoader.load();
            EditIslandController controller = fxmlLoader.getController();
            controller.setIslandName(oldSaveData.island());
            controller.setIslandDescription(oldSaveData.description());

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(newIslandDialogPane);
            dialog.setTitle("Edit '" + oldSaveData.island() + "' Island");
            dialog.initOwner(Application.PRIMARY_STAGE);

            Optional<ButtonType> clickedbutton = dialog.showAndWait();

            if (clickedbutton.isPresent() && clickedbutton.get() == ButtonType.FINISH) {
                final SaveData updatedSaveData = SaveManager.createSaveData(controller.getIslandName(), controller.getIslandDescription());
                if (SaveManager.updateIslandDetails(oldSaveData, updatedSaveData)) {
                    return updatedSaveData;
                }
            }
        } catch (IOException e) {
            throw new IOException("Something went wrong editing the details of an island.", e);
        }

        return oldSaveData;
    }

    public static boolean updateIslandDetails(final SaveData oldSaveData, final SaveData newSaveData) throws IOException {
        logger.info("Updating island details from: '" + oldSaveData.island() + "' into: '" + newSaveData.island() + "'");
        try {
            final File oldSaveDir = new File(oldSaveData.folder());
            final File newSaveDir = new File(ISLANDS_DIRECTORY, newSaveData.island());

            if (!oldSaveData.island().equals(newSaveData.island())) {
                if (newSaveDir.exists()) {
                    SaveManager.showNamingConflictWarning("Could not edit '" + oldSaveData.island() + "' island", newSaveData.island());
                    logger.info("Could not edit '" + oldSaveData.island() + "' island since the new name '" + newSaveData.island() + "' already exists");
                    return false;
                }

                FileUtils.forceMkdir(newSaveDir);
                FileUtils.copyDirectory(oldSaveDir, newSaveDir);
                FileUtils.deleteDirectory(oldSaveDir);
            }

            final File newMetadataFile = new File(newSaveDir, ACNHConsts.SAVE_METADATA_FILE);
            writeMetadataFile(newMetadataFile, newSaveData);
            extractIslands();
        } catch (IOException e) {
            throw new IOException("Could not update details of save location: " + oldSaveData.folder() + ", for island: " + oldSaveData.island() + ", with new island name: " + newSaveData.island(), e);
        }
        return true;
    }

    public static boolean createNewIsland(String islandName, String islandDescription) throws IOException {
        final File newIslandDir = new File(ISLANDS_DIRECTORY, islandName);
        logger.info("Creating new island: " + islandName + ", with description: " + islandDescription);

        try {
            final File newIslandMetadata = new File(newIslandDir, ACNHConsts.SAVE_METADATA_FILE);
            final SaveData metadata = new SaveData(islandName, newIslandDir.getAbsolutePath(), islandDescription, new Date());

            if (!verifyMetadataNameChange("Could not create new '" + metadata.island() + "' island", metadata)) {
                return false;
            }

            FileUtils.forceMkdir(newIslandDir);
            writeMetadataFile(newIslandMetadata, metadata);
            extractIslands();
        } catch (IOException e) {
            throw new IOException("Could not create new island and save to: " + newIslandDir.getAbsolutePath() + ", for island: " + islandName, e);
        }
        return true;
    }

    public static boolean duplicateIsland(final SaveData islandToDuplicate) throws IOException {
        final String copiedName = islandToDuplicate.island().replaceFirst("(\\.[^.]*)?$", "-copy$0");
        logger.info("Duplicating island: " + islandToDuplicate.island() + ", duplicated island name will be: " + copiedName);

        final File oldIslandDir = new File(islandToDuplicate.folder());
        final File duplicateIslandDir = new File(ISLANDS_DIRECTORY, copiedName);

        try {
            final SaveData duplicateIslandMetadata = new SaveData(copiedName, duplicateIslandDir.getAbsolutePath(), islandToDuplicate.description(), new Date());

            if (!verifyMetadataNameChange("Could not duplicate '" + duplicateIslandMetadata.island() + "' island", duplicateIslandMetadata)) {
                return false;
            }

            FileUtils.forceMkdir(duplicateIslandDir);
            FileUtils.copyDirectory(oldIslandDir, duplicateIslandDir);

            final File metadata = new File(duplicateIslandDir, ACNHConsts.SAVE_METADATA_FILE);
            writeMetadataFile(metadata, duplicateIslandMetadata);
            extractIslands();
        } catch (IOException e) {
            throw new IOException("Could not create duplicate island and save to: " + duplicateIslandDir.getAbsolutePath() + ", for new duplicate island: " + copiedName, e);
        }

        return true;
    }

    public static void deleteIsland(SaveData saveData) throws IOException {
        try {
            logger.info("Deleting island: " + saveData.island());
            FileUtils.deleteDirectory(new File(saveData.folder()));
            extractIslands();
        } catch (IOException e) {
            throw new IOException("Could not delete island at save location: " + saveData.folder() + ", for island: " + saveData.island(), e);
        }
    }

    private static boolean promptSelectNHSEExecutable(final Stage primaryStage) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Set NHSE Executable Directory");
        alert.setContentText("In order to edit the save data of an island, you must select the directory of your NHSE executable. Press OK to select the directory or cancel to stop.");
        alert.setHeaderText("Select the NHSE directory to proceed");
        alert.initOwner(Application.PRIMARY_STAGE);
        final Optional<ButtonType> type = alert.showAndWait();

        if (type.isPresent() && type.get() == ButtonType.OK) {
            final DirectoryChooser directoryChooser = new DirectoryChooser();
            final File selectedDirectory = directoryChooser.showDialog(primaryStage);
            final File executable = new File(selectedDirectory, ACNHConsts.NHSE_EXECUTABLE);

            if (executable.exists()) {
                final File propertiesFile = new File(ISLANDS_DIRECTORY, ACNHConsts.PROPERTIES_FILE);
                final SaveManagerProperties properties = new SaveManagerProperties(executable.getAbsolutePath());
                writePropertiesFile(propertiesFile, properties);
                PROPERTIES = properties;
                return true;
            }
        }

        return false;
    }

    public static boolean openSaveEditorFor(final Stage primaryStage, final Path islandDirectory) throws IOException {
        if (PROPERTIES == null && !promptSelectNHSEExecutable(primaryStage)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setContentText("The selected directory does not contain an NHSE executable with the following name: " + ACNHConsts.NHSE_EXECUTABLE);
            alert.setHeaderText("Cannot use Save Editor");
            alert.initOwner(Application.PRIMARY_STAGE);
            alert.showAndWait();
            return false;
        }

        try {
            final Path mainDat = islandDirectory.resolve(ACNHConsts.MAIN_DAT);

            if (mainDat.toFile().exists()) {
                Application.ANCHOR_PANE.setDisable(true);
                final Process process = new ProcessBuilder(PROPERTIES.pathToNHSExecutable(), mainDat.toString()).start();
                process.onExit().thenAccept(p -> Application.ANCHOR_PANE.setDisable(false));
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setContentText("The '" + islandDirectory.toFile().getName() + "' island does not have a main.dat file, so the Save Editor cannot open.");
                alert.setHeaderText("Cannot use Save Editor");
                alert.initOwner(Application.PRIMARY_STAGE);
                alert.showAndWait();
            }
        } catch (IOException e) {
            throw new IOException("Something went wrong when starting the NHSE process.", e);
        }

        return true;
    }
}