package com.anon.nhsm.data;

import com.anon.nhsm.AppProperties;

import java.nio.file.Path;
import java.util.Date;

public record SaveMetadata(String island, String description, Date date, boolean emulatorLocked) {
    public static SaveMetadata name(final String name) {
        return nameAndDescription(name, "");
    }

    public static SaveMetadata nameAndDescription(final String name, final String description) {
        return new SaveMetadata(name, description, new Date(), false);
    }
    public SaveMetadata date(final Date date) {
        return new SaveMetadata(island, description, date, emulatorLocked);
    }

    public SaveMetadata lock(final boolean lock) {
        return new SaveMetadata(island, description, date, lock);
    }

    public Path islandDirectory(final AppProperties appProperties) {
        return islandDirectory(appProperties, island);
    }

    public Path metadataFile(final AppProperties appProperties) {
        return metadataFile(appProperties, island);
    }

    public Path lockFile(final AppProperties appProperties) {
        return lockFile(appProperties, island);
    }

    public static Path islandDirectory(final AppProperties appProperties, final String islandName) {
        return appProperties.islandsDirectory().resolve(islandName);
    }

    public static Path metadataFile(final AppProperties appProperties, final String islandName) {
        return islandDirectory(appProperties, islandName).resolve(AppPaths.SAVE_METADATA_FILE_NAME);
    }

    public static Path lockFile(final AppProperties appProperties, final String islandName) {
        return islandDirectory(appProperties, islandName).resolve(AppPaths.EMULATOR_LOCK_FILE_NAME);
    }
}
