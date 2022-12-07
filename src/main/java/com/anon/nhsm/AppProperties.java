package com.anon.nhsm;

import com.anon.nhsm.data.AppPaths;
import com.anon.nhsm.data.EmulatorType;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

public record AppProperties(Path islandsDirectory, Path nhsExecutable, Path ryujinxSaveDirectory, Path yuzuSaveDirectory, EmulatorType emulatorTarget) {
    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder(this);
    }

    public static class Builder {
        private Path islandsDirectory;
        private Path nhsExecutable;
        private Path ryujinxSaveDirectory;
        private Path yuzuSaveDirectory;
        private EmulatorType emulatorTarget;

        private Builder() {

        }

        private Builder(final AppProperties copyFrom) {
            islandsDirectory = copyFrom.islandsDirectory;
            nhsExecutable = copyFrom.nhsExecutable;
            ryujinxSaveDirectory = copyFrom.ryujinxSaveDirectory;
            yuzuSaveDirectory = copyFrom.yuzuSaveDirectory;
            emulatorTarget = copyFrom.emulatorTarget;
        }

        public Builder islandsDirectory(final Path path) {
            islandsDirectory = path;
            return this;
        }

        public Builder nhsExecutable(final Path path) {
            nhsExecutable = path;
            return this;
        }

        public Builder ryujinxSaveDirectory(final Path path) {
            ryujinxSaveDirectory = path;
            return this;
        }

        public Builder yuzuSaveDirectory(final Path path) {
            yuzuSaveDirectory = path;
            return this;
        }

        public Builder emulatorTarget(final EmulatorType emulator) {
            emulatorTarget = emulator;
            return this;
        }

        public AppProperties build() {
            return new AppProperties(islandsDirectory, nhsExecutable, ryujinxSaveDirectory, yuzuSaveDirectory, emulatorTarget);
        }
    }

    public static class IO {
        private static final AppProperties DEFAULT_PROPERTIES = AppProperties.builder().islandsDirectory(AppPaths.createIslandsDirectory()).build();

        public static AppProperties loadAndValidateAppProperties() throws IOException {
            final AppProperties properties = readAppPropertiesFile(AppPaths.APP_PROPERTIES_FILE);

            if (properties != null) { // Validate file paths and remove if no longer exist
                final AppProperties.Builder validationBuilder = properties.copy();

                if (properties.nhsExecutable() != null && !Files.exists(properties.nhsExecutable())) {
                    validationBuilder.nhsExecutable(null);
                }

                if (properties.ryujinxSaveDirectory() != null && !Files.exists(properties.ryujinxSaveDirectory())) {
                    validationBuilder.ryujinxSaveDirectory(null);
                }

                if (properties.yuzuSaveDirectory() != null && !Files.exists(properties.yuzuSaveDirectory())) {
                    validationBuilder.yuzuSaveDirectory(null);
                }

                final AppProperties validated = validationBuilder.build();
                if (!validated.equals(properties)) {
                    return Main.writeAppProperties(validated);
                }
            } else { // Write empty properties file
                return Main.writeAppProperties(DEFAULT_PROPERTIES);
            }

            return properties;
        }

        public static AppProperties readAppPropertiesFile(final Path file) throws IOException {
            if (Files.exists(file)) {
                try (final FileReader fileReader = new FileReader(file.toFile())) {
                    final Type type = new TypeToken<AppProperties>(){}.getType();
                    final Gson gson = Main.GSON.create();
                    return gson.fromJson(fileReader, type);
                } catch (final IOException e) {
                    throw new IOException("Could not read Save Manager properties from file: " + file.toAbsolutePath(), e);
                }
            }

            return null;
        }

        public static void writeAppPropertiesFile(final Path path, final AppProperties properties) throws IOException {
            try {
                Files.deleteIfExists(path);
            } catch (final IOException e) {
                throw new IOException("Could not delete old Save Manager properties file: " + path.toAbsolutePath(), e);
            }

            Files.createDirectories(path.getParent());
            Files.createFile(path);
            try (final FileWriter fileWriter = new FileWriter(path.toFile())) {
                final Gson gson = Main.GSON.create();
                final JsonElement jsonElement = gson.toJsonTree(properties);
                jsonElement.getAsJsonObject().addProperty("version", Main.DATA_VERSION.toString());
                gson.toJson(jsonElement, fileWriter);
            } catch (final IOException e) {
                throw new IOException("Could not write Save Manager properties to file: " + path.toAbsolutePath(), e);
            }
        }
    }
}
