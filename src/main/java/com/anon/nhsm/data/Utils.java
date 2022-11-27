package com.anon.nhsm.data;

import java.io.File;

public class Utils {
    public static final String YUZU_GAME_ID = "01006F8002326000";
    public static final String YUZU_SAVE_DIRECTORY = "\\yuzu\\nand\\user\\save\\0000000000000000\\00000000000000000000000000000000\\" + YUZU_GAME_ID;
    public static final String SAVE_METADATA_FILE = "metadata.json";

    public static final String MAIN_DAT = "main.dat";
    public static final String TMP_DIR_NAME = "tmp";
    public static final String NHSE_EXECUTABLE = "NHSE.WinForms.exe";
    public static final String PROPERTIES_FILE = "properties.json";
    public static final String ACNH_ISLANDS_DIR = "acnh_islands";
    private static final String APPDATA_ENV = "APPDATA";
    private static final File APP_DATA_DIRECTORY = new File(System.getenv(Utils.APPDATA_ENV));

    public static File createYuzuSaveDirectory() {
        return new File(APP_DATA_DIRECTORY, Utils.YUZU_SAVE_DIRECTORY);
    }

    public static File createIslandsDirectory() {
        return new File(APP_DATA_DIRECTORY, Utils.ACNH_ISLANDS_DIR);
    }
}