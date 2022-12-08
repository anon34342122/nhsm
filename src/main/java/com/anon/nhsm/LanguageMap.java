package com.anon.nhsm;

import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.*;

public class LanguageMap {
    public static final String DEFAULT_LANG_ID = "en_US";
    public static Map<String, Locale> ID_TO_LOCALE = new HashMap<>();

    static {
        ID_TO_LOCALE.put(DEFAULT_LANG_ID, Locale.US);
        ID_TO_LOCALE.put("ja", Locale.JAPANESE);
    }
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
