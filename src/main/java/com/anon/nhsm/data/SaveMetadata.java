package com.anon.nhsm.data;

import java.util.Date;

public record SaveMetadata(String island, String folder, String description, Date date, boolean emulatorLocked) {
    public SaveMetadata date(final Date date) {
        return new SaveMetadata(island, folder, description, date, emulatorLocked);
    }

    public SaveMetadata lock(final boolean lock) {
        return new SaveMetadata(island, folder, description, date, lock);
    }
}
