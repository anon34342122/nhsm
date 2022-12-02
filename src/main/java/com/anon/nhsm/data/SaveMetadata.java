package com.anon.nhsm.data;

import com.anon.nhsm.AppProperties;

import java.io.File;
import java.util.Date;

public record SaveMetadata(String island, String description, Date date, boolean emulatorLocked) {
    public File nhsmIslandDirectory(final AppProperties appProperties) {
        return new File(appProperties.islandsDirectory(), island());
    }

    public SaveMetadata date(final Date date) {
        return new SaveMetadata(island, description, date, emulatorLocked);
    }

    public SaveMetadata lock(final boolean lock) {
        return new SaveMetadata(island, description, date, lock);
    }
}
