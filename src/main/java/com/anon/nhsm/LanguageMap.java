package com.anon.nhsm;

import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LanguageMap {
    private final ResourceBundle resourceBundle;

    public LanguageMap(final Locale locale) {
        resourceBundle = getLanguages(locale);
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    public String get(final String key) {
        try {
            return resourceBundle.getString(key);
        } catch (final MissingResourceException e) {
            return "[" + key + "]";
        }
    }

    public String get(final String key, final Object... params) {
        try {
            return MessageFormat.format(resourceBundle.getString(key), params);
        } catch (final MissingResourceException e) {
            return "[" + key + "]";
        }
    }

    private static ResourceBundle getLanguages(final Locale locale) {
        final ClassLoader loader = new URLClassLoader(new URL[]{ Stages.class.getResource("language")});
        return ResourceBundle.getBundle("", locale, loader);
    }
}
