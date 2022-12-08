package com.anon.nhsm;

import com.anon.nhsm.data.AppPaths;
import com.anon.nhsm.data.EmulatorType;
import com.anon.nhsm.data.SaveManager;
import com.anon.nhsm.data.SaveMetadata;
import org.apache.commons.io.file.PathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTests {
    static {
        Bootstrap.bootStrap();
    }

    public static class IntegrationTest {
        private final SaveManager saveManager;

        public IntegrationTest(final Path testIntegrationDirectory, final EmulatorType emulatorTarget) {
            final Path relativeIslands = AppPaths.USER_HOME.relativize(AppPaths.createIslandsDirectory());
            final Path relativeRyujinx = AppPaths.USER_HOME.relativize(AppPaths.createRyujinxSavesDirectory().resolve("0000000000000001\\0"));
            final Path relativeYuzu = AppPaths.USER_HOME.relativize(AppPaths.createYuzuSaveDirectory());

            final AppProperties appProperties = AppProperties.builder()
                    .islandsDirectory(testIntegrationDirectory.resolve(relativeIslands))
                    .ryujinxSaveDirectory(testIntegrationDirectory.resolve(relativeRyujinx))
                    .yuzuSaveDirectory(testIntegrationDirectory.resolve(relativeYuzu))
                    .emulatorTarget(emulatorTarget)
                    .build();
            saveManager = createSaveManager(appProperties);
        }

        public SaveManager getSaveManager() {
            return saveManager;
        }

        private static SaveManager createSaveManager(final AppProperties appProperties) {
            final Path emulatorSaveDirectory = appProperties.emulatorTarget().getSaveDirectory(appProperties);
            final SaveManager.Config config = new SaveManager.Config(emulatorSaveDirectory);
            return new SaveManager(appProperties, config);
        }
    }

    private static final Logger logger = LogManager.getLogger(IntegrationTests.class);
    private static Path SANDBOX_DIRECTORY;
    public static final String SETUP_TEST_METADATA_NAME = "Setup Test Metadata";
    public static final String SETUP_TEST_METADATA_DESCRIPTION = "This is a description for test metadata";
    private static final List<IntegrationTest> INTEGRATION_TESTS = new ArrayList<>();

    private static String relativePath(final Path path) {
        return path.getRoot().relativize(path).toString();
    }

    private static Path sandbox(final String name) {
        return SANDBOX_DIRECTORY.resolve(name);
    }

    private static IntegrationTest createIntegrationTest(final Path copyFromDirectory, final EmulatorType emulatorTarget) throws IOException {
        final String directoryName = copyFromDirectory.getFileName().toString();
        final Path root = sandbox(directoryName).resolve(emulatorTarget.name());
        final Path userHome = root.resolve(relativePath(AppPaths.USER_HOME));

        Files.createDirectories(userHome);
        PathUtils.copyDirectory(copyFromDirectory, userHome);

        return new IntegrationTest(userHome, emulatorTarget);
    }

    @BeforeAll
    static void setup() {
        try {
            SANDBOX_DIRECTORY = Files.createTempDirectory(UUID.randomUUID().toString());
            assertNotNull(Files.createDirectories(SANDBOX_DIRECTORY), "Couldn't make the sandbox test save directories");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        final Path integrationTestsDirectory = Paths.get(".\\src\\test\\resources\\integration_tests");

        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(integrationTestsDirectory)) {
            boolean hasIterated = false;
            for (final Path directory : stream) {
                final IntegrationTest testYuzu = createIntegrationTest(directory, EmulatorType.YUZU);
                final IntegrationTest testRyujinx = createIntegrationTest(directory, EmulatorType.RYUJINX);

                INTEGRATION_TESTS.add(testYuzu);
                INTEGRATION_TESTS.add(testRyujinx);

                hasIterated = true;
            }

            assertTrue(hasIterated, "There are no test integration folders, aborting tests.");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    public static void tearDown() {
        try {
            PathUtils.deleteDirectory(SANDBOX_DIRECTORY);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Order(1)
    @ParameterizedTest
    @MethodSource("provideSaveManagers")
    void setupWithExistingMetadata(final SaveManager saveManager) {
        logger.info("Starting 'setupWithExistingMetadata' test with emulator target: " + saveManager.getAppProperties().emulatorTarget());
        logger.info("Setting up SaveManager with emulator save directory: " + saveManager.getConfig().emulatorSaveDirectory());

        try {
            final SaveManager.Config config = saveManager.getConfig();
            final Path localSaveMetadataFile = config.emulatorSaveDirectory().resolve(AppPaths.SAVE_METADATA_FILE_NAME);
            final SaveMetadata readSaveMetadata = saveManager.readMetdataFile(localSaveMetadataFile);

            assertNull(saveManager.getEmulatorSaveMetadata(), "Save Manager already has a loaded emulator save metdata for some reason");

            saveManager.setup();

            if (readSaveMetadata != null) {
                assertEquals(readSaveMetadata, saveManager.getEmulatorSaveMetadata(), "Save Manager did not have the same emulator save metadata after setup");
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Order(2)
    @ParameterizedTest
    @MethodSource("provideSaveManagers")
    void createNewIsland(final SaveManager saveManager) {
        final AppProperties appProperties = saveManager.getAppProperties();
        logger.info("Starting 'createNewIsland' test with emulator target: " + appProperties.emulatorTarget());

        try {
            final String islandName = "CreateNewIslandTest";
            final var firstIsland = new Object() {
                boolean hasConflicted = false;
            };

            final Optional<SaveMetadata> result = saveManager.createNewIsland(islandName, "", island -> firstIsland.hasConflicted = true);

            assertFalse(firstIsland.hasConflicted, "Creating this island '" + islandName + "' for the first time should not have conflicted");
            assertTrue(result.isPresent(), "New island was not created successfully");

            final SaveMetadata newIslandMetadata = result.get();
            final Path newIslandDirectory = newIslandMetadata.islandDirectory(appProperties);
            final Path newIslandMetadataFile = newIslandMetadata.metadataFile(appProperties);

            assertTrue(Files.exists(newIslandDirectory), "New island directory was not created when creating new island: " + islandName);
            assertTrue(Files.exists(newIslandMetadataFile), "New island metadata was not created when creating new island: " + islandName);

            final var secondIsland = new Object() {
                boolean hasHadNameConflict = false;
            };
            saveManager.createNewIsland(islandName, "", island -> secondIsland.hasHadNameConflict = true);

            assertTrue(secondIsland.hasHadNameConflict, "Name conflict did not work when trying to add a new island with the same existing name");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Order(2)
    @ParameterizedTest
    @MethodSource("provideSaveManagers")
    void verifyHasNoNameConflict(final SaveManager saveManager) {
        logger.info("Starting 'verifyHasNoNameConflict' test with emulator target: " + saveManager.getAppProperties().emulatorTarget());

        final String islandName = "Conflicting Island";

        final SaveMetadata shouldConflict = SaveMetadata.name(islandName);
        assertFalse(saveManager.verifyHasNoNameConflict(shouldConflict));

        final SaveMetadata shouldNotConflict = SaveMetadata.name(islandName + " 2");
        assertTrue(saveManager.verifyHasNoNameConflict(shouldNotConflict));
    }

    @Order(2)
    @ParameterizedTest
    @MethodSource("provideSaveManagers")
    void updateIslandDetails(final SaveManager saveManager) {
        logger.info("Starting 'updateIslandDetails' test with emulator target: " + saveManager.getAppProperties().emulatorTarget());

        try {
            final String oldIslandName = "Rename Island";

            final SaveMetadata oldIslandMetadata = SaveMetadata.name(oldIslandName);;
            final Path oldIslandDirectory = oldIslandMetadata.islandDirectory(saveManager.getAppProperties());
            final Path oldIslandMetadataFile = oldIslandMetadata.metadataFile(saveManager.getAppProperties());

            testForUpdateNameConflict(saveManager, oldIslandName, oldIslandMetadata, oldIslandDirectory);
            testForUpdateNameSuccess(saveManager, oldIslandName, oldIslandMetadata, oldIslandDirectory, oldIslandMetadataFile);
            // TODO: Test for changing description
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void testForUpdateNameSuccess(final SaveManager saveManager, final String oldIslandName, final SaveMetadata oldIslandMetadata, final Path oldIslandDirectory, final Path oldIslandMetadataFile) throws IOException {
        final String newIslandName = "Island Was Renamed";

        final SaveMetadata newIslandMetadata = SaveMetadata.name(newIslandName);
        final AppProperties appProperties = saveManager.getAppProperties();
        final Path renamedIslandDirectory = newIslandMetadata.islandDirectory(appProperties);
        final Path renamedIslandMetadataFile = newIslandMetadata.metadataFile(appProperties);

        assertTrue(Files.exists(oldIslandDirectory), "Could not find island directory of '" + oldIslandName + "'.");
        assertTrue(Files.exists(oldIslandMetadataFile), "Could not find metadata of island '" + oldIslandName + "'.");

        final var conflict = new Object() {
            boolean hasConflicted = false;
        };
        final boolean firstUpdate = saveManager.updateIslandDetails(oldIslandMetadata, newIslandMetadata, conflictingMetadata -> conflict.hasConflicted = true);

        assertTrue(firstUpdate, "Update details did not succeed");
        assertFalse(conflict.hasConflicted, "Renaming this island '" + oldIslandName + "' to '" + newIslandName + "' should not have conflicted");
        assertFalse(Files.exists(oldIslandDirectory), "Old island directory still persisted after renaming island: " + oldIslandName);
        assertTrue(Files.exists(renamedIslandDirectory), "Renamed island directory did not exist for some reason: " + newIslandName);
        assertTrue(Files.exists(renamedIslandMetadataFile), "Renamed island metadata file did not exist for some reason: " + newIslandName);

        final SaveMetadata doesNotExistMetadata = SaveMetadata.name("Does Not Exist");

        final boolean secondUpdate = saveManager.updateIslandDetails(doesNotExistMetadata, newIslandMetadata, conflictingMetadata -> conflict.hasConflicted = true);

        assertFalse(secondUpdate, "Should not be able to update an island that does not exist");
    }

    private static void testForUpdateNameConflict(final SaveManager saveManager, final String oldIslandName, final SaveMetadata oldIslandMetadata, final Path oldIslandDirectory) throws IOException {
        final String conflictingIslandName = "Conflicting Island";

        final SaveMetadata conflictingIslandMetadata = SaveMetadata.name(conflictingIslandName);
        final AppProperties appProperties = saveManager.getAppProperties();
        final Path conflictingIslandDirectory = conflictingIslandMetadata.islandDirectory(appProperties);
        final Path conflictingIslandMetadataFile = conflictingIslandMetadata.metadataFile(appProperties);

        assertTrue(Files.exists(conflictingIslandDirectory), "Could not find island directory for conflicting island '" + conflictingIslandName + "'.");
        assertTrue(Files.exists(conflictingIslandMetadataFile), "Could not find metadata of island '" + oldIslandName + "'.");

        final var conflict = new Object() {
            boolean hasConflicted = false;
        };
        saveManager.updateIslandDetails(oldIslandMetadata, conflictingIslandMetadata, conflictingMetadata -> conflict.hasConflicted = true);

        assertTrue(conflict.hasConflicted, "Did not conflict when trying to rename '" + oldIslandName + "' to an island that already exists: " + conflictingIslandName);
        assertTrue(Files.exists(oldIslandDirectory), "Old island directory did not persist after attempting to rename and conflicting: " + oldIslandName);
    }

    @Order(2)
    @ParameterizedTest
    @MethodSource("provideSaveManagers")
    void duplicateIsland(final SaveManager saveManager) {
        logger.info("Starting 'duplicateIsland' test with emulator target: " + saveManager.getAppProperties().emulatorTarget());

        try {
            final AppProperties appProperties = saveManager.getAppProperties();
            final String oldIslandName = "Duplicating Island";

            final SaveMetadata oldIslandMetadata = SaveMetadata.name(oldIslandName);;

            final Optional<SaveMetadata> result = saveManager.duplicateIsland(oldIslandMetadata, conflictingMetadata -> {});

            assertTrue(result.isPresent());

            final SaveMetadata duplicatedIsland = result.get();
            final Path duplicatedIslandDirectory = duplicatedIsland.islandDirectory(appProperties);
            final Path duplicatedIslandMetadataFile = duplicatedIsland.metadataFile(appProperties);

            assertTrue(Files.exists(duplicatedIslandDirectory), "Duplicated island directory does not exist: " + duplicatedIsland.island());
            assertTrue(Files.exists(duplicatedIslandMetadataFile), "Duplicated island metadata file does not exist: " + duplicatedIsland.island());

            final var conflict = new Object() {
                boolean hasConflicted = false;
            };
            final Optional<SaveMetadata> secondResult = saveManager.duplicateIsland(oldIslandMetadata, conflictingMetadata -> conflict.hasConflicted = true);

            assertTrue(conflict.hasConflicted, "Trying to duplicate the same island twice should conflict, but it did not.");
            assertTrue(secondResult.isEmpty(), "Trying to duplicate the same island twice should result in no new island, but it did.");

            final SaveMetadata doesNotExistIslandMetadata = SaveMetadata.name("Does Not Exist");

            final Optional<SaveMetadata> thirdResult = saveManager.duplicateIsland(doesNotExistIslandMetadata, conflictingMetadata -> {});

            assertTrue(thirdResult.isEmpty(), "We should not get duplicated island from an island that does not exist");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Order(2)
    @ParameterizedTest
    @MethodSource("provideSaveManagers")
    void deleteIsland(final SaveManager saveManager) {
        logger.info("Starting 'deleteIsland' test with emulator target: " + saveManager.getAppProperties().emulatorTarget());

        try {
            final AppProperties appProperties = saveManager.getAppProperties();
            final String deleteIslandName = "Delete Island";

            final SaveMetadata deleteIslandMetadata = SaveMetadata.name(deleteIslandName);
            final Path deleteIslandDirectory = deleteIslandMetadata.islandDirectory(appProperties);
            final Path deleteIslandMetadataFile = deleteIslandMetadata.metadataFile(appProperties);

            assertTrue(Files.exists(deleteIslandDirectory), "Island directory for island we're about to delete does not exist: " + deleteIslandMetadata.island());
            assertTrue(Files.exists(deleteIslandMetadataFile), "Metadata file for island we're about to delete does not exist: " + deleteIslandMetadata.island());

            final boolean firstDeleteAttempt = saveManager.deleteIsland(deleteIslandMetadata);

            assertTrue(firstDeleteAttempt, "Island could not be deleted");
            assertFalse(Files.exists(deleteIslandDirectory), "Island directory for island we deleted still exists: " + deleteIslandMetadata.island());
            assertFalse(Files.exists(deleteIslandMetadataFile), "Metadata file for island we deleted still exists: " + deleteIslandMetadata.island());

            final boolean secondDeleteAttempt = saveManager.deleteIsland(deleteIslandMetadata);

            assertFalse(secondDeleteAttempt, "We should not be able to delete the same island twice");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Order(2)
    @ParameterizedTest
    @MethodSource("provideSaveManagers")
    void swapWithLocalSave(final SaveManager saveManager) {
        logger.info("Starting 'swapWithLocalSave' test with emulator target: " + saveManager.getAppProperties().emulatorTarget());

        try {
            final AppProperties appProperties = saveManager.getAppProperties();
            final String swapIslandName = "Swap Island";

            final SaveMetadata swapIslandMetadata = SaveMetadata.name(swapIslandName);
            final Path swapIslandDirectory = swapIslandMetadata.islandDirectory(appProperties);
            final Path localSaveDirectory = saveManager.getConfig().emulatorSaveDirectory();

            final SaveMetadata localSaveMetadata = saveManager.readMetdataFile(localSaveDirectory.resolve(AppPaths.SAVE_METADATA_FILE_NAME));

            Path localLockFile = null;
            if (localSaveMetadata != null) {
                localLockFile = localSaveMetadata.lockFile(appProperties);
                assertTrue(Files.exists(localLockFile), "Local save has island metadata and is being used, but the nhsm directory has no lock file");
            }

            assertFalse(PathUtils.directoryAndFileContentEquals(swapIslandDirectory, localSaveDirectory), "Island contents should not be equal before swapping");

            final boolean swapped = saveManager.swapWithLocalSave(swapIslandMetadata, localSaveMetadataFile -> {
                final String conflictingName = "Conflicting Island";
                final String islandName = "Ryujinx Island";
                final String islandDescription = "";

                final var firstConflict = new Object() {
                    boolean hasConflicted;
                };
                final boolean firstConvert = saveManager.convertLocalSaveToIsland(localSaveMetadataFile, conflictingName, islandDescription, conflictingMetadata -> firstConflict.hasConflicted = true);

                assertTrue(firstConflict.hasConflicted, "First convert to local save should have conflicted");
                assertFalse(firstConvert, "First convert should have not succeeded");

                final var secondConflict = new Object() {
                    boolean hasConflicted;
                };
                final boolean secondConvert = saveManager.convertLocalSaveToIsland(localSaveMetadataFile, islandName, islandDescription, conflictingMetadata -> secondConflict.hasConflicted = true);

                assertFalse(secondConflict.hasConflicted, "Second convert to local save should have not conflicted");
                assertTrue(secondConvert, "Second convert should have succeeded");

                return true;
            });

            final Path lockFile = swapIslandMetadata.lockFile(appProperties);

            assertTrue(swapped, "Local save should have swapped successfully");
            assertTrue(Files.exists(lockFile), "Lock file was not created when swapping");

            if (localLockFile != null) {
                assertFalse(Files.exists(localLockFile), "Local save should have lost its lock file after being swapped with another island");
            }

            Files.deleteIfExists(lockFile); // Delete to check for directory equality
            assertTrue(PathUtils.directoryAndFileContentEquals(swapIslandDirectory, localSaveDirectory), "Island contents were not equal after swapping");
            Files.createFile(lockFile); // Recreate after checking for equality
            assertTrue(Files.exists(lockFile), "Lock file was not recreated after checking for equality");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> provideSaveManagers() {
        return INTEGRATION_TESTS.stream().map(IntegrationTests.IntegrationTest::getSaveManager).map(Arguments::of);
    }
}