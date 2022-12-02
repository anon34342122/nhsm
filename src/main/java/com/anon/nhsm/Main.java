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
    static Logger LOGGER;

    public static void main(final String[] args) {
        LOGGER = LogManager.getLogger(Application.class);
        LOGGER.info("Starting application");

        IOException setupExceptionThrown = null;
        SaveManager saveManager = null;

        try {
            AppPaths.bootStrap();
            final SaveManager.Config saveManagerConfig = new SaveManager.Config(AppPaths.createYuzuSaveDirectory());
            saveManager = new SaveManager(AppProperties.IO.loadAndValidateAppProperties(), saveManagerConfig);
            saveManager.setup();
        } catch (final IOException e) {
            setupExceptionThrown = e;
        }

        new Application(setupExceptionThrown, saveManager);
    }
}
