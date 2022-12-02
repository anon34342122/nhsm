package com.anon.nhsm.data;

import com.anon.nhsm.AppProperties;

import java.io.File;
import java.util.function.Function;

public enum EmulatorType {
    RYUJINX(AppProperties::ryujinxSaveDirectory),
    YUZU(AppProperties::yuzuSaveDirectory);
    private final Function<AppProperties, File> saveDirectoryFunction;

    EmulatorType(final Function<AppProperties, File> saveDirectoryFunction) {
        this.saveDirectoryFunction = saveDirectoryFunction;
    }

    public File getSaveDirectory(final AppProperties appProperties) {
        return saveDirectoryFunction.apply(appProperties);
    }
}