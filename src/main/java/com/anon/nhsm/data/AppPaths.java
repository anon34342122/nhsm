package com.anon.nhsm.data;

import com.anon.nhsm.app.Application;

import java.io.File;

public class AppPaths {
    public static final String YUZU_GAME_ID = "01006F8002326000";
    public static final String YUZU_AC_SAVE_PATH = "\\yuzu\\nand\\user\\save\\0000000000000000\\00000000000000000000000000000000\\" + YUZU_GAME_ID;

    public static final String RYUJINX_SAVES_PATH = "Ryujinx\\bis\\user\\save";
    public static final String SAVE_METADATA_FILE = "metadata.json";

    public static final String MAIN_DAT = "main.dat";
    public static final String TMP_DIR_NAME = "tmp";
    public static final String NHSE_EXECUTABLE = "NHSE.WinForms.exe";
    public static final File APP_PROPERTIES_FILE = new File(Application.APPLICATION_DIRECTORY, "nhsm_properties.json");
    public static final String ISLANDS_DIRECTORY_NAME = "islands";

    public static File createRyujinxSavesDirectory() {
        return new File(Application.USER_HOME, AppPaths.RYUJINX_SAVES_PATH);
    }

    public static File createYuzuSaveDirectory() {
        return new File(Application.USER_HOME, AppPaths.YUZU_AC_SAVE_PATH);
    }

    public static File createIslandsDirectory() {
        return new File(Application.APPLICATION_DIRECTORY, AppPaths.ISLANDS_DIRECTORY_NAME);
    }
}