package com.anon.nhsm.data;

import com.anon.nhsm.AppProperties;
import com.anon.nhsm.Main;
import com.anon.nhsm.app.Application;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.file.PathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class SaveManager {
    private static final Logger logger = LogManager.getLogger(SaveManager.class);
    public static final String UNNAMED_ISLAND = "Unnamed Island";

    private final List<SaveMetadata> islandsMetadata = new ArrayList<>();
    private SaveMetadata emulatorSaveMetadata;

    private AppProperties appProperties;
    public record Config(Path emulatorSaveDirectory) {}

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
        final Path emulatorSaveMetadataFile = config.emulatorSaveDirectory().resolve(AppPaths.SAVE_METADATA_FILE_NAME);
        if (Files.exists(emulatorSaveMetadataFile)) {
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

    public boolean verifyHasNoNameConflict(final SaveMetadata metadata) {
        final Path checkIfExistsAlready = appProperties.islandsDirectory().resolve(metadata.island());
        return !Files.exists(checkIfExistsAlready);
    }

    @FunctionalInterface
    public interface ConvertLocalSaveIntoIsland {
        boolean tryConvert(final Path localSaveMetadataFile) throws IOException;
    }

    public boolean convertLocalSaveToIsland(final Path metadataFile, final String islandName, final String islandDescription, final OnNamingConflict onNamingConflict) throws IOException {
        final SaveMetadata newMetadata = new SaveMetadata(islandName, islandDescription, new Date(), false);

        logger.info("Creating new metadata for converting Emulator Local Save: " + newMetadata);
        if (!verifyHasNoNameConflict(newMetadata)) {
            onNamingConflict.apply(newMetadata);
            logger.info("Could not write metadata file to Emulator Local Save directory because of name conflict: " + newMetadata.island() + " already exists");
            return false;
        }

        writeMetadataFile(metadataFile, newMetadata);
        logger.info("Written metadata file to emulator local save: " + metadataFile.toAbsolutePath());
        return true;
    }

    public boolean swapWithLocalSave(final SaveMetadata islandMetadata, final ConvertLocalSaveIntoIsland onLocalSaveMetadataMissing) throws IOException {
        final Path nhsmIslandDirectory = islandMetadata.islandDirectory(appProperties);
        logger.info("Attempting to swap the contents of the '" + islandMetadata.island() + "' island with the emulator local save");
        if (!Files.exists(config.emulatorSaveDirectory())) {
            logger.info("Copying requested save data's island folder contents to the emulator's local save directory.");
            Files.createDirectories(config.emulatorSaveDirectory());
            PathUtils.copyDirectory(nhsmIslandDirectory, config.emulatorSaveDirectory());
        } else {
            final Path localSaveMetadataFile = config.emulatorSaveDirectory().resolve(AppPaths.SAVE_METADATA_FILE_NAME);

            if (!Files.exists(localSaveMetadataFile) && !onLocalSaveMetadataMissing.tryConvert(localSaveMetadataFile)) {
                return false;
            }

            final SaveMetadata localSaveMetadata = readMetdataFile(localSaveMetadataFile);

            if (localSaveMetadata != null) {
                final Path localSaveNhsmIslandDirectory = localSaveMetadata.islandDirectory(appProperties);
                final Path tmpDirectory = Files.createTempDirectory(UUID.randomUUID().toString());

                // Deleting tmpPath if it exists, then creating it
                if (Files.exists(tmpDirectory)) {
                    PathUtils.deleteDirectory(tmpDirectory);
                }
                Files.createDirectories(tmpDirectory);

                final Path tmpIslandDir = tmpDirectory.resolve(localSaveMetadata.island());

                // Update metadata with new date timestmap
                writeMetadataFile(config.emulatorSaveDirectory().resolve(AppPaths.SAVE_METADATA_FILE_NAME), localSaveMetadata.date(new Date()));

                logger.info("Moving local save directory contents to the temp island directory: " + tmpIslandDir.toAbsolutePath());
                PathUtils.copyDirectory(config.emulatorSaveDirectory(), tmpIslandDir);
                PathUtils.deleteDirectory(config.emulatorSaveDirectory());

                logger.info("Copy all contents of requested saved island: " + nhsmIslandDirectory + ", to the local save directory: " + config.emulatorSaveDirectory().toAbsolutePath());
                PathUtils.copyDirectory(nhsmIslandDirectory, config.emulatorSaveDirectory());

                try {
                    logger.info("Create an emulator lock file for the requested save data's island directory: " + nhsmIslandDirectory);
                    final Path emulatorLockFile = islandMetadata.lockFile(appProperties);
                    Files.createFile(emulatorLockFile);
                } catch (final IOException e) {
                    throw new IOException("Something went wrong, the emulator lock file could not be made.", e);
                }

                logger.info("Delete all contents of the island directory the local save file was from: " + localSaveNhsmIslandDirectory.toAbsolutePath());
                if (Files.exists(localSaveNhsmIslandDirectory)) {
                    PathUtils.deleteDirectory(localSaveNhsmIslandDirectory);
                }

                logger.info("Move all contents from the temp island directory to the island directory the local save file was from: " + localSaveNhsmIslandDirectory.toAbsolutePath());
                PathUtils.copyDirectory(tmpIslandDir, localSaveNhsmIslandDirectory);

                logger.info("Delete the temp directory: " + tmpDirectory.toAbsolutePath());
                PathUtils.deleteDirectory(tmpDirectory);

                this.emulatorSaveMetadata = islandMetadata;
                extractIslands();
                logger.info("Successfully applied '" + islandMetadata.island() + "' to the local save directory");
            }
        }

        return true;
    }

    public void extractIslands() throws IOException {
        islandsMetadata.clear();
        logger.info("Extracting island data from acnh_islands directory.");

        if (Files.exists(appProperties.islandsDirectory())) {
            final Set<Path> directories = new HashSet<>();

            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(appProperties.islandsDirectory())) {
                for (final Path islandDirectory : stream) {
                    try (final Stream<Path> subFiles = Files.list(islandDirectory)) {
                        final List<String> fileNames = subFiles.map(Path::getFileName).map(Object::toString).toList();

                        final boolean isValidIsland = fileNames.contains(AppPaths.SAVE_METADATA_FILE_NAME) || fileNames.contains(AppPaths.MAIN_DAT);
                        if (isValidIsland && !islandDirectory.getFileName().toString().equals(AppPaths.TMP_DIR_NAME)) {
                            directories.add(islandDirectory);
                        }
                    }
                }
            }

            for (final Path islandDirectory : directories) {
                final Path metadata = islandDirectory.resolve(AppPaths.SAVE_METADATA_FILE_NAME);
                final Path emulatorLockFile = islandDirectory.resolve(AppPaths.EMULATOR_LOCK_FILE_NAME);

                final SaveMetadata islandSaveMetadata = readMetdataFile(metadata);

                if (emulatorSaveMetadata != null && emulatorSaveMetadata.equals(islandSaveMetadata)) {
                    continue;
                }

                if (islandSaveMetadata != null) {
                    islandsMetadata.add(islandSaveMetadata.lock(Files.exists(emulatorLockFile)));
                } else {
                    final String islandName = islandDirectory.getFileName().toString();
                    try {
                        final BasicFileAttributes attributes = Files.readAttributes(islandDirectory, BasicFileAttributes.class);
                        final Date creationDate = new Date(attributes.creationTime().to(TimeUnit.MILLISECONDS));
                        final SaveMetadata saveMetadata = new SaveMetadata(islandName, "", creationDate, Files.exists(emulatorLockFile));
                        writeMetadataFile(metadata, saveMetadata);
                        islandsMetadata.add(saveMetadata);
                    } catch (final IOException e) {
                        throw new IOException("Could not read basic file attributes at save location: " + islandDirectory.toAbsolutePath().toString() + ", for island: " + islandName, e);
                    }
                }
            }
        }
    }

    public boolean updateIslandDetails(final SaveMetadata oldSaveMetadata, final SaveMetadata newSaveMetadata, final OnNamingConflict onNamingConflict) throws IOException {
        logger.info("Updating island details from: '" + oldSaveMetadata.island() + "' into: '" + newSaveMetadata.island() + "'");
        final Path oldSaveDirectory = oldSaveMetadata.islandDirectory(appProperties);

        if (!Files.exists(oldSaveDirectory)) {
            return false;
        }

        final Path newSaveDirectory = newSaveMetadata.islandDirectory(appProperties);
        final Path newMetadataFile = newSaveMetadata.metadataFile(appProperties);

        try {
            if (!oldSaveMetadata.island().equals(newSaveMetadata.island())) {
                if (Files.exists(newSaveDirectory)) {
                    onNamingConflict.apply(newSaveMetadata);
                    logger.info("Could not edit '" + oldSaveMetadata.island() + "' island since the new name '" + newSaveMetadata.island() + "' already exists");
                    return false;
                }

                Files.createDirectories(newSaveDirectory);
                PathUtils.copyDirectory(oldSaveDirectory, newSaveDirectory);
                PathUtils.deleteDirectory(oldSaveDirectory);
            }

            writeMetadataFile(newMetadataFile, newSaveMetadata);
            extractIslands();
        } catch (final IOException e) {
            throw new IOException("Could not update details of save location: " + oldSaveDirectory.toAbsolutePath().toString() + ", for island: " + oldSaveMetadata.island() + ", with new island name: " + newSaveMetadata.island(), e);
        }
        return true;
    }

    public Optional<SaveMetadata> createNewIsland(final String islandName, final String islandDescription, final OnNamingConflict onNamingConflict) throws IOException {
        final Path newIslandDirectory = appProperties.islandsDirectory().resolve(islandName);
        logger.info("Creating new island: " + islandName + ", with description: " + islandDescription);

        try {
            final Path newIslandMetadata = newIslandDirectory.resolve(AppPaths.SAVE_METADATA_FILE_NAME);
            final SaveMetadata metadata = new SaveMetadata(islandName, islandDescription, new Date(), false);

            if (!verifyHasNoNameConflict(metadata)) {
                onNamingConflict.apply(metadata);
                return Optional.empty();
            }

            Files.createDirectories(newIslandDirectory);
            writeMetadataFile(newIslandMetadata, metadata);
            extractIslands();

            return Optional.of(metadata);
        } catch (final IOException e) {
            throw new IOException("Could not create new island and save to: " + newIslandDirectory.toAbsolutePath().toString() + ", for island: " + islandName, e);
        }
    }

    @FunctionalInterface
    public interface OnNamingConflict {
        void apply(final SaveMetadata conflictingMetadata);
    }

    public Optional<SaveMetadata> duplicateIsland(final SaveMetadata islandToDuplicate, final OnNamingConflict onNamingConflict) throws IOException {
        final String copiedName = islandToDuplicate.island().replaceFirst("(\\.[^.]*)?$", "-copy$0");
        logger.info("Duplicating island: " + islandToDuplicate.island() + ", duplicated island name will be: " + copiedName);

        final Path oldIslandDirectory = islandToDuplicate.islandDirectory(appProperties);

        if (!Files.exists(oldIslandDirectory)) {
            return Optional.empty();
        }

        final SaveMetadata duplicateIslandMetadata = new SaveMetadata(copiedName, islandToDuplicate.description(), new Date(), false);
        final Path duplicateIslandDirectory = duplicateIslandMetadata.islandDirectory(appProperties);
        final Path duplicateMetadataFile = duplicateIslandMetadata.metadataFile(appProperties);

        try {
            if (!verifyHasNoNameConflict(duplicateIslandMetadata)) {
                logger.info("Could not duplicate island: " + islandToDuplicate.island() + ", duplicated name already exists: " + copiedName);
                onNamingConflict.apply(duplicateIslandMetadata);
                return Optional.empty();
            }

            Files.createDirectories(duplicateIslandDirectory);
            PathUtils.copyDirectory(oldIslandDirectory, duplicateIslandDirectory);

            writeMetadataFile(duplicateMetadataFile, duplicateIslandMetadata);
            extractIslands();
            return Optional.of(duplicateIslandMetadata);
        } catch (final IOException e) {
            throw new IOException("Could not create duplicate island and save to: " + duplicateIslandDirectory.toAbsolutePath() + ", for new duplicate island: " + copiedName, e);
        }
    }

    public boolean deleteIsland(final SaveMetadata saveMetadata) throws IOException {
        final Path islandDirectory = saveMetadata.islandDirectory(appProperties);

        if (!Files.exists(islandDirectory)) {
            return false;
        }

        try {
            logger.info("Deleting island: " + saveMetadata.island());
            PathUtils.deleteDirectory(islandDirectory);
            extractIslands();
            return true;
        } catch (final IOException e) {
            throw new IOException("Could not delete island at save location: " + islandDirectory.toAbsolutePath() + ", for island: " + saveMetadata.island(), e);
        }
    }

    @FunctionalInterface
    public interface TrySelectExecutable {
        boolean trySelect() throws IOException;
    }

    @FunctionalInterface
    public interface MainDatMissing {
        void apply(final String islandName);
    }

    public void openSaveEditorFor(final Path islandSaveDirectory, final String islandName, final TrySelectExecutable onExecutableMissing, final TrySelectExecutable onExecutableNotSelectedYet, final MainDatMissing onMainDatMissing) throws IOException {
        if (appProperties.nhsExecutable() == null && !onExecutableNotSelectedYet.trySelect()) {
            return;
        } else if (!Files.exists(appProperties.nhsExecutable()) && !onExecutableMissing.trySelect()) {
            return;
        }

        try {
            final Path mainDat = islandSaveDirectory.resolve(AppPaths.MAIN_DAT);

            if (Files.exists(mainDat)) {
                Application.ANCHOR_PANE.setDisable(true);
                final Process process = new ProcessBuilder(appProperties.nhsExecutable().toAbsolutePath().toString(), mainDat.toAbsolutePath().toString()).start();
                process.onExit().thenAccept(p -> Application.ANCHOR_PANE.setDisable(false));
            } else {
                onMainDatMissing.apply(islandName);
            }
        } catch (final IOException e) {
            throw new IOException("Something went wrong when starting the NHSE process.", e);
        }
    }

    public SaveMetadata writeMetadataFile(final Path path, final SaveMetadata saveMetadata) throws IOException {
        if (!Files.exists(path) || Files.deleteIfExists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            try (final FileWriter fileWriter = new FileWriter(path.toFile())) {
                final Gson gson = Main.GSON.create();
                final JsonElement jsonElement = gson.toJsonTree(saveMetadata);
                jsonElement.getAsJsonObject().addProperty("version", Main.DATA_VERSION.toString());
                jsonElement.getAsJsonObject().remove("emulatorLocked");
                gson.toJson(jsonElement, fileWriter);
            } catch (final IOException | JsonIOException e) {
                throw new IOException("Could not write metadata to file: " + path.toAbsolutePath() + ", for island: " + saveMetadata.island(), e);
            }
        }
        return saveMetadata;
    }

    public SaveMetadata readMetdataFile(final Path file) throws IOException {
        if (Files.exists(file)) {
            try (final FileReader fileReader = new FileReader(file.toFile())) {
                final Type type = new TypeToken<SaveMetadata>(){}.getType();
                final Gson gson = Main.GSON.create();
                return gson.fromJson(fileReader, type);
            } catch (final IOException | JsonIOException e) {
                throw new IOException("Could not read metadata from file: " + file.toAbsolutePath().toString(), e);
            }
        }

        return null;
    }
}