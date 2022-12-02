package com.anon.nhsm.data;

import java.util.Date;

public record SaveMetadata(String island, String folder, String description, Date date) {
    public static SaveMetadata copyWithNewDate(final SaveMetadata copyFrom, final Date date) {
        return new SaveMetadata(copyFrom.island, copyFrom.folder, copyFrom.description, date);
    }
}
