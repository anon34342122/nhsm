package com.anon.nhsm.data.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathToAbsolutePathStringAdapter extends TypeAdapter<Path> {
    @Override
    public void write(final JsonWriter out, final Path value) throws IOException {
        if (value != null) {
            out.value(value.toAbsolutePath().toString());
        } else {
            out.nullValue();
        }
    }

    @Override
    public Path read(final JsonReader in) throws IOException {
        final String absolutePath = in.nextString();

        if (absolutePath == null) {
            return null;
        }

        return Paths.get(absolutePath);
    }
}
