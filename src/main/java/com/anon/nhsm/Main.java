package com.anon.nhsm;

import com.anon.nhsm.app.Application;
import com.anon.nhsm.data.json.FileToAbsolutePathAdapter;
import com.anon.nhsm.data.AppPaths;
import com.anon.nhsm.data.SaveManager;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semver4j.Semver;

import java.io.File;
import java.io.IOException;

public class Main {
    public static final Semver DATA_VERSION = new Semver("0.0.1");
    public static final String APPLICATION_NAME = "NHSM";
    public static GsonBuilder GSON = new GsonBuilder()
            .registerTypeAdapter(File.class, new FileToAbsolutePathAdapter())
            .setPrettyPrinting();
    public static SaveManager SAVE_MANAGER;
    static Logger LOGGER;

    public static void main(final String[] args) {
        LOGGER = LogManager.getLogger(Application.class);
        LOGGER.info("Starting application");

        IOException setupExceptionThrown = null;

        try {
            AppPaths.bootStrap();
            final SaveManager.Config saveManagerConfig = new SaveManager.Config(AppPaths.createYuzuSaveDirectory());
            SAVE_MANAGER = new SaveManager(AppProperties.IO.loadAndValidateAppProperties(), saveManagerConfig);
            SAVE_MANAGER.setup();
        } catch (final IOException e) {
            setupExceptionThrown = e;
        }

        new Application(setupExceptionThrown);
    }

    public static AppProperties writeAppProperties(final AppProperties properties) throws IOException {
        AppProperties.IO.writeAppPropertiesFile(AppPaths.APP_PROPERTIES_FILE, properties);
        return properties;
    }
}
