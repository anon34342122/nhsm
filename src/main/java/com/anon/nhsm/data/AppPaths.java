package com.anon.nhsm.data;

import com.anon.nhsm.Main;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppPaths {
    public static Path APPLICATION_DIRECTORY;
    public static Path USER_HOME;
    public static Path APP_PROPERTIES_FILE;
    public static final String ISLANDS_DIRECTORY_NAME = "islands";
    public static final String SAVE_METADATA_FILE_NAME = "metadata.json";
    public static final String EMULATOR_LOCK_FILE_NAME = "emulator.lock";
    public static final String YUZU_GAME_ID = "01006F8002326000";
    public static final String YUZU_AC_SAVE_PATH = "yuzu\\nand\\user\\save\\0000000000000000\\00000000000000000000000000000000\\" + YUZU_GAME_ID;
    public static final String RYUJINX_SAVES_PATH = "Ryujinx\\bis\\user\\save";
    public static final String MAIN_DAT = "main.dat";
    public static final String TMP_DIR_NAME = "tmp";
    public static final String NHSE_EXECUTABLE = "NHSE.WinForms.exe";

    public static void bootStrap() throws IOException {
        APPLICATION_DIRECTORY = createApplicationDirectory();
        APP_PROPERTIES_FILE = APPLICATION_DIRECTORY.resolve("nhsm_properties.json");
    }

    public static Path createRyujinxSavesDirectory() {
        return USER_HOME.resolve(AppPaths.RYUJINX_SAVES_PATH);
    }

    public static Path createYuzuSaveDirectory() {
        return USER_HOME.resolve(AppPaths.YUZU_AC_SAVE_PATH);
    }

    public static Path createIslandsDirectory() {
        return APPLICATION_DIRECTORY.resolve(AppPaths.ISLANDS_DIRECTORY_NAME);
    }

    public static Path createApplicationDirectory() throws IOException {
        final SystemInfo.Platform platform = SystemInfo.getPlatform();
        USER_HOME = switch (platform) {
            case LINUX, SOLARIS, MAC -> getUnixHomeDirectory();
            case WINDOWS -> getWindowsHomeDirectory();
            default -> throw new IOException("OS not supported: " + platform.name());
        };

        final Path applicationDirectory = USER_HOME.resolve(Main.APPLICATION_NAME);

        if (!Files.exists(applicationDirectory)) {
            try {
                Files.createDirectories(applicationDirectory);
            } catch (final Exception e) {
                throw new IOException("The application directory could not be created: " + applicationDirectory, e);
            }
        }

        return applicationDirectory;
    }

    private static Path getUnixHomeDirectory() {
        return Paths.get(FileUtils.getUserDirectoryPath(), ".local", "share");
    }

    private static Path getWindowsHomeDirectory() throws IOException {
        final String applicationData = System.getenv("APPDATA");

        if (applicationData == null) {
            throw new IOException("Appdata was not found on Windows device, aborting.");
        }

        return Paths.get(applicationData);
    }
}