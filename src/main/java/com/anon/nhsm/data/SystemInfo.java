package com.anon.nhsm.data;

public class SystemInfo {
    public enum Platform {
        UNKNOWN,
        WINDOWS,
        MAC,
        SOLARIS,
        LINUX
    }

    public static Platform getPlatform() {
        final String operatingSystem = System.getProperty("os.name").toLowerCase();

        if (operatingSystem.contains("win")) {
            return Platform.WINDOWS;
        } else if (operatingSystem.contains("mac")) {
            return Platform.MAC;
        } else if (operatingSystem.contains("nix") || operatingSystem.contains("nux") || operatingSystem.contains("aix")) {
            return Platform.LINUX;
        } else if (operatingSystem.contains("sunos")) {
            return Platform.SOLARIS;
        }

        return Platform.UNKNOWN;
    }
}
