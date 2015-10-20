package de.droidwiki.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.reflect.TypeToken;

import de.droidwiki.crash.RemoteLogException;
import de.droidwiki.page.tabs.Tab;
import de.droidwiki.util.log.L;

import java.util.Collections;
import java.util.List;

public final class TabUnmarshaller {
    private static final TypeToken<List<Tab>> TYPE_TOKEN = new TypeToken<List<Tab>>() { };

    @NonNull public static List<Tab> unmarshal(@Nullable String json) {
        List<Tab> object = null;
        try {
            object = GsonUnmarshaller.unmarshal(TYPE_TOKEN, json);
        } catch (Exception e) {
            // Catch all. Any Exception can be thrown when unmarshalling.
            L.logRemoteErrorIfProd(new RemoteLogException(e).put("json", json));
        }
        if (object == null) {
            object = Collections.emptyList();
        }
        return object;
    }

    private TabUnmarshaller() { }
}
