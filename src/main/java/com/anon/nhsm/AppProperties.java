package com.anon.nhsm;

import com.anon.nhsm.data.EmulatorType;
import com.anon.nhsm.data.AppPaths;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;

public record AppProperties(File islandsDirectory, File nhsExecutable, File ryujinxSaveDirectory, File yuzuSaveDirectory, EmulatorType emulatorTarget) {
    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder(this);
    }

    public static class Builder {
        private File islandsDirectory;
        private File nhsExecutable;
        private File ryujinxSaveDirectory;
        private File yuzuSaveDirectory;
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

        public Builder islandsDirectory(final File path) {
            islandsDirectory = path;
            return this;
        }

        public Builder nhsExecutable(final File path) {
            nhsExecutable = path;
            return this;
        }

        public Builder ryujinxSaveDirectory(final File path) {
            ryujinxSaveDirectory = path;
            return this;
        }

        public Builder yuzuSaveDirectory(final File path) {
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

                if (properties.nhsExecutable() != null && !properties.nhsExecutable().exists()) {
                    validationBuilder.nhsExecutable(null);
                }

                if (properties.ryujinxSaveDirectory() != null && !properties.ryujinxSaveDirectory().exists()) {
                    validationBuilder.ryujinxSaveDirectory(null);
                }

                if (properties.yuzuSaveDirectory() != null && !properties.yuzuSaveDirectory().exists()) {
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

        public static AppProperties readAppPropertiesFile(final File file) throws IOException {
            if (file.exists()) {
                try (final FileReader fileReader = new FileReader(file)) {
                    final Type type = new TypeToken<AppProperties>(){}.getType();
                    final Gson gson = Main.GSON.create();
                    return gson.fromJson(fileReader, type);
                } catch (final IOException e) {
                    throw new IOException("Could not read Save Manager properties from file: " + file.getAbsolutePath(), e);
                }
            }

            return null;
        }

        public static void writeAppPropertiesFile(final File file, final AppProperties properties) throws IOException {
            try {
                if (file.exists()) {
                    FileUtils.delete(file);
                }
            } catch (final IOException e) {
                throw new IOException("Could not delete old Save Manager properties file: " + file.getAbsolutePath(), e);
            }

            try (final FileWriter fileWriter = new FileWriter(file)) {
                final Gson gson = Main.GSON.create();
                final JsonElement jsonElement = gson.toJsonTree(properties);
                jsonElement.getAsJsonObject().addProperty("version", Main.DATA_VERSION.toString());
                gson.toJson(jsonElement, fileWriter);
            } catch (final IOException e) {
                throw new IOException("Could not write Save Manager properties to file: " + file.getAbsolutePath(), e);
            }
        }
    }
}
