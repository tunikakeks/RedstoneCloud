package de.redstonecloud.node.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.node.RedstoneNode;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;

public class Translator {
    private static final JsonObject langFile;

    static {
        try {
            langFile = new Gson().fromJson(Files.readString(Paths.get(RedstoneNode.workingDir + "/language.json")), JsonObject.class);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load language files: " + e);
        }
    }

    public static String translate(String key) {
        return langFile.has(key) ? langFile.get(key).getAsString() : key;
    }

    public static String translate(String key, Object... replace) {
        if(!langFile.has(key)) return key;

        String value = langFile.get(key).getAsString();

        if (replace != null && replace.length > 0) {
            return MessageFormat.format(value, replace);
        }

        return value;
    }
}
