package com.anon.nhsm;

import com.anon.nhsm.data.AppPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Bootstrap {
    static Logger LOGGER;

    public static void bootStrap() {
        LOGGER = LogManager.getLogger(Bootstrap.class);
        LOGGER.info("Starting application");

        try {
            AppPaths.bootStrap();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
