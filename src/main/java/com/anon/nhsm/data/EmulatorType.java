package com.anon.nhsm.data;

public enum EmulatorType {
    RYUJINX("ryujinx"),
    YUZU("yuzu");

    private final String name;

    EmulatorType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}