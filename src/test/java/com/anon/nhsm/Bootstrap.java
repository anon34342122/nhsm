package com.anon.nhsm;

import com.anon.nhsm.data.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Bootstrap {
    static Logger LOGGER;
    static boolean initialized;

    public static void bootStrap() {
        if (initialized) {
            return;
        }

        System.setProperty("log4j.configurationFile", "log4j2-test.xml");
        LOGGER = LogManager.getLogger(Bootstrap.class);
        LOGGER.info("Starting tests");

        try {
            AppPaths.bootStrap();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        initialized = true;
    }
}
