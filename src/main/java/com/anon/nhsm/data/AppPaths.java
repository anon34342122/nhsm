package com.anon.nhsm.data;

import com.anon.nhsm.Main;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class AppPaths {
    public static File APPLICATION_DIRECTORY;
    public static File USER_HOME;
    public static File APP_PROPERTIES_FILE;
    public static final String ISLANDS_DIRECTORY_NAME = "islands";
    public static final String SAVE_METADATA_FILE_NAME = "metadata.json";
    public static final String EMULATOR_LOCK_FILE_NAME = "emulator.lock";
    public static final String YUZU_GAME_ID = "01006F8002326000";
    public static final String YUZU_AC_SAVE_PATH = "\\yuzu\\nand\\user\\save\\0000000000000000\\00000000000000000000000000000000\\" + YUZU_GAME_ID;
    public static final String RYUJINX_SAVES_PATH = "Ryujinx\\bis\\user\\save";
    public static final String MAIN_DAT = "main.dat";
    public static final String TMP_DIR_NAME = "tmp";
    public static final String NHSE_EXECUTABLE = "NHSE.WinForms.exe";

    public static void bootStrap() throws IOException {
        APPLICATION_DIRECTORY = createApplicationDirectory();
        APP_PROPERTIES_FILE = new File(APPLICATION_DIRECTORY, "nhsm_properties.json");
    }

    public static File createRyujinxSavesDirectory() {
        return new File(USER_HOME, AppPaths.RYUJINX_SAVES_PATH);
    }

    public static File createYuzuSaveDirectory() {
        return new File(USER_HOME, AppPaths.YUZU_AC_SAVE_PATH);
    }

    public static File createIslandsDirectory() {
        return new File(APPLICATION_DIRECTORY, AppPaths.ISLANDS_DIRECTORY_NAME);
    }

    public static File createApplicationDirectory() throws IOException {
        final SystemInfo.Platform platform = SystemInfo.getPlatform();
        USER_HOME = switch (platform) {
            case LINUX, SOLARIS, MAC -> getUnixHomeDirectory();
            case WINDOWS -> getWindowsHomeDirectory();
            default -> throw new IOException("OS not supported: " + platform.name());
        };

        final File applicationDirectory = new File(USER_HOME, Main.APPLICATION_NAME);

        if (!applicationDirectory.exists() && !applicationDirectory.mkdirs()) {
            throw new IOException("The application directory could not be created: " + applicationDirectory);
        }

        return applicationDirectory;
    }

    private static File getUnixHomeDirectory() {
        return new File(FileUtils.getUserDirectoryPath(), ".local" + File.separator + "share" + File.separator);
    }

    private static File getWindowsHomeDirectory() throws IOException {
        final String applicationData = System.getenv("APPDATA");

        if (applicationData == null) {
            throw new IOException("Appdata was not found on Windows device, aborting.");
        }

        return new File(applicationData);
    }
}