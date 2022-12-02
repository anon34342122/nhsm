package com.anon.nhsm.data.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.IOException;

public class FileToAbsolutePathAdapter extends TypeAdapter<File> {
    @Override
    public void write(final JsonWriter out, final File value) throws IOException {
        if (value != null) {
            out.value(value.getAbsolutePath());
        } else {
            out.nullValue();
        }
    }

    @Override
    public File read(final JsonReader in) throws IOException {
        final String absolutePath = in.nextString();

        if (absolutePath == null) {
            return null;
        }

        return new File(absolutePath);
    }
}
