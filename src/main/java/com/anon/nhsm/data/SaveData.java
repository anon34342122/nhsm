package com.anon.nhsm.data;

import java.util.Date;

public record SaveData(String island, String folder, String description, Date date) {
    public static SaveData copyWithNewDate(final SaveData copyFrom, final Date date) {
        return new SaveData(copyFrom.island, copyFrom.folder, copyFrom.description, date);
    }
}
