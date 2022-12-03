package com.anon.nhsm.data;

import com.anon.nhsm.AppProperties;
import com.anon.nhsm.Bootstrap;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SaveManagerTest {
    static {
        Bootstrap.bootStrap();
    }

    private static final Logger logger = LogManager.getLogger(SaveManagerTest.class);
    private static final File SANDBOX_DIRECTORY = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString());

    private static final AppProperties APP_PROPERTIES = AppProperties.builder()
            .islandsDirectory(sandbox(AppPaths.createIslandsDirectory()))
            .ryujinxSaveDirectory(sandbox(new File(AppPaths.createRyujinxSavesDirectory(), "0000000000000001\\0")))
            .yuzuSaveDirectory(sandbox(AppPaths.createYuzuSaveDirectory()))
            .build();
    private static final AppProperties YUZU_APP_PROPERTIES = APP_PROPERTIES.copy().emulatorTarget(EmulatorType.YUZU).build();
    private static final AppProperties RYUJINX_APP_PROPERTIES = APP_PROPERTIES.copy().emulatorTarget(EmulatorType.RYUJINX).build();
    public static final String SETUP_TEST_METADATA_NAME = "Setup Test Metadata";
    public static final String SETUP_TEST_METADATA_DESCRIPTION = "This is a description for test metadata";
    private SaveManager saveManager;

    private static File sandbox(final File file) {
        final Path path = Paths.get(file.getAbsolutePath());
        return new File(SANDBOX_DIRECTORY, path.getRoot().relativize(path).toString());
    }

    SaveMetadata createEmulatorLocalSaveDirectory(final File file) throws IOException {
        final File localSaveMetadataFile = new File(file, AppPaths.SAVE_METADATA_FILE_NAME);
        final SaveMetadata localSaveMetadata = new SaveMetadata(SETUP_TEST_METADATA_NAME, SETUP_TEST_METADATA_DESCRIPTION, new Date(), false);
        saveManager.writeMetadataFile(localSaveMetadataFile, localSaveMetadata);
        return localSaveMetadata;
    }

    SaveMetadata readSaveMetadataInDirectory(final File file) throws IOException {
        final File localSaveMetadataFile = new File(file, AppPaths.SAVE_METADATA_FILE_NAME);
        return saveManager.readMetdataFile(localSaveMetadataFile);
    }

    @BeforeAll
    public static void init() {
        assertTrue(APP_PROPERTIES.yuzuSaveDirectory().mkdirs(), "Couldn't make the yuzu save directories");
        assertTrue(APP_PROPERTIES.ryujinxSaveDirectory().mkdirs(), "Couldn't make the ryujinx save directories");
        assertTrue(APP_PROPERTIES.islandsDirectory().mkdirs(), "Couldn't make the island storage directories");
    }

    @AfterAll
    public static void tearDown() {
        try {
            FileUtils.deleteDirectory(SANDBOX_DIRECTORY);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("provideSaveManagerConfigs")
    void setupWithExistingMetadata(final AppProperties appProperties) {
        logger.info("Starting 'setupWithExistingMetadata' test with emulator target: " + appProperties.emulatorTarget());
        final File emulatorSaveDirectory = appProperties.emulatorTarget().getSaveDirectory(appProperties);
        final SaveManager.Config config = new SaveManager.Config(emulatorSaveDirectory);
        logger.info("Creating SaveManager with emulator save directory: " + config.emulatorSaveDirectory());
        saveManager = new SaveManager(APP_PROPERTIES, config);

        try {
            final SaveMetadata writtenSaveMetadata = createEmulatorLocalSaveDirectory(config.emulatorSaveDirectory());

            logger.info("Written local save metadata: " + writtenSaveMetadata.toString());

            final SaveMetadata readSaveMetadata = readSaveMetadataInDirectory(config.emulatorSaveDirectory());
            assertNotNull(readSaveMetadata, "Written save metadata is null after reading");
            assertEquals(SETUP_TEST_METADATA_NAME, readSaveMetadata.island(), "Read save metadata name is different from what we wrote");
            assertEquals(SETUP_TEST_METADATA_DESCRIPTION, readSaveMetadata.description(), "Read save metadata description is different from what we wrote");

            assertNull(saveManager.getEmulatorSaveMetadata(), "Save Manager already has a loaded emulator save metdata for some reason");

            saveManager.setup();

            assertEquals(readSaveMetadata, saveManager.getEmulatorSaveMetadata(), "Save Manager did not have the same emulator save metadata after setup");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> provideSaveManagerConfigs() {
        return Stream.of(
                Arguments.of(YUZU_APP_PROPERTIES),
                Arguments.of(RYUJINX_APP_PROPERTIES)
        );
    }
}