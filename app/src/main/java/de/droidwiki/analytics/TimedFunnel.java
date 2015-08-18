package de.droidwiki.analytics;

import android.support.annotation.NonNull;

import de.droidwiki.Site;
import de.droidwiki.WikipediaApp;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

/*package*/ abstract class TimedFunnel extends Funnel {
    private final long startTime;

    /*package*/ TimedFunnel(WikipediaApp app, String schemaName, int revision, int sampleRate) {
        this(app, schemaName, revision, sampleRate, null);
    }

    /*package*/ TimedFunnel(WikipediaApp app, String schemaName, int revision, int sampleRate, Site site) {
        super(app, schemaName, revision, sampleRate, site);
        startTime = System.currentTimeMillis();
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, getDurationFieldName(), getDurationSeconds());
        return super.preprocessData(eventData);
    }

    /** Override me for deviant implementations. */
    protected String getDurationFieldName() {
        return "timeSpent";
    }

    private long getDuration() {
        return System.currentTimeMillis() - startTime;
    }

    private long getDurationSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(getDuration());
    }
}
