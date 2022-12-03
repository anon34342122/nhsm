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
        assert(APP_PROPERTIES.yuzuSaveDirectory().mkdirs());
        assert(APP_PROPERTIES.ryujinxSaveDirectory().mkdirs());
        assert(APP_PROPERTIES.islandsDirectory().mkdirs());
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
    void setupWithExistingMetadata(final SaveManager.Config config) {
        logger.info("Starting 'setup' test with emulator save directory: " + config.emulatorSaveDirectory().getAbsolutePath());
        saveManager = new SaveManager(APP_PROPERTIES, config);

        try {
            final SaveMetadata writtenSaveMetadata = createEmulatorLocalSaveDirectory(config.emulatorSaveDirectory());

            logger.info("Written local save metadata: " + writtenSaveMetadata.toString());

            final SaveMetadata readSaveMetadata = readSaveMetadataInDirectory(config.emulatorSaveDirectory());
            assert(readSaveMetadata != null);
            assert(SETUP_TEST_METADATA_NAME.equals(readSaveMetadata.island()));
            assert(SETUP_TEST_METADATA_DESCRIPTION.equals(readSaveMetadata.description()));

            assert(saveManager.getEmulatorSaveData() == null);

            saveManager.setup();

            assert(readSaveMetadata.equals(saveManager.getEmulatorSaveData()));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> provideSaveManagerConfigs() {
        return Stream.of(
                Arguments.of(new SaveManager.Config(APP_PROPERTIES.yuzuSaveDirectory())),
                Arguments.of(new SaveManager.Config(APP_PROPERTIES.ryujinxSaveDirectory()))
        );
    }
}