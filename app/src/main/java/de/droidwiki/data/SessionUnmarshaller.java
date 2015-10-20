package de.droidwiki.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.droidwiki.analytics.SessionData;
import de.droidwiki.crash.RemoteLogException;
import de.droidwiki.util.log.L;

public final class SessionUnmarshaller {
    @NonNull public static SessionData unmarshal(@Nullable String json) {
        SessionData sessionData = null;
        try {
            sessionData = GsonUnmarshaller.unmarshal(SessionData.class, json);
        } catch (Exception e) {
            // Catch all. Any Exception can be thrown when unmarshalling.
            L.logRemoteErrorIfProd(new RemoteLogException(e).put("json", json));
        }
        if (sessionData == null) {
            sessionData = new SessionData();
        }
        return sessionData;
    }

    private SessionUnmarshaller() { }
}
