package com.anon.nhsm.data;

import com.anon.nhsm.AppProperties;

import java.nio.file.Path;
import java.util.function.Function;

public enum EmulatorType {
    RYUJINX(AppProperties::ryujinxSaveDirectory),
    YUZU(AppProperties::yuzuSaveDirectory);
    private final Function<AppProperties, Path> saveDirectoryFunction;

    EmulatorType(final Function<AppProperties, Path> saveDirectoryFunction) {
        this.saveDirectoryFunction = saveDirectoryFunction;
    }

    public Path getSaveDirectory(final AppProperties appProperties) {
        return saveDirectoryFunction.apply(appProperties);
    }
}