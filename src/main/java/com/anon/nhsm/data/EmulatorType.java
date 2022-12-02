package com.anon.nhsm.data;

import com.anon.nhsm.AppProperties;

import java.io.File;
import java.util.function.Function;

public enum EmulatorType {
    RYUJINX("ryujinx", AppProperties::ryujinxSaveDirectory),
    YUZU("yuzu", AppProperties::yuzuSaveDirectory);

    private final String name;
    private final Function<AppProperties, File> saveDirectoryFunction;

    EmulatorType(final String name, final Function<AppProperties, File> saveDirectoryFunction) {
        this.name = name;
        this.saveDirectoryFunction = saveDirectoryFunction;
    }

    public String getName() {
        return name;
    }

    public File getSaveDirectory(final AppProperties appProperties) {
        return saveDirectoryFunction.apply(appProperties);
    }
}