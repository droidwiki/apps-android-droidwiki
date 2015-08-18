/* THIS IS AN AUTOMATICALLY GENERATED FILE
   IF YOU MODIFY THIS BY HAND BE PREPARED!
   TO HAVE YOUR CHANGES OVERWRITTEN */
package de.droidwiki.staticdata;

import java.util.*;

public final class MainPageNameData {

    private static HashMap<String, String> DATA_MAP;

    @SuppressWarnings({"checkstyle:methodlength", "SpellCheckingInspection"})
    private static void setupData() {
        final int size = 291;
        DATA_MAP = new HashMap<>(size, 1.0f);
        DATA_MAP.put("de", "Hauptseite");
    }

    public static String valueFor(String key) {
        if (DATA_MAP == null) {
            setupData();
        }

        if (DATA_MAP.containsKey(key)) {
            return DATA_MAP.get(key);
        }
        return DATA_MAP.get("de");
    }

    private MainPageNameData() {
    }
}