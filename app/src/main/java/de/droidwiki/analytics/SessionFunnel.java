package de.droidwiki.analytics;

import de.droidwiki.WikipediaApp;
import de.droidwiki.history.HistoryEntry;
import org.json.JSONObject;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import java.util.Date;

public class SessionFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppSessions";
    private static final int REVISION = 10375481;

    /**
     * Definition of a "session timeout", as agreed upon by the Apps and Analytics teams.
     * (currently 30 minutes)
     */
    private static final int SESSION_TIMEOUT_SECONDS = 30 * 60;

    private static final String SESSION_TIMESTAMP_PREF_NAME = "SESSION_TIMESTAMP_PREF";
    private static final String SESSION_PAGES_SEARCH_PREF_NAME = "SESSION_PAGES_SEARCH_PREF";
    private static final String SESSION_PAGES_RANDOM_PREF_NAME = "SESSION_PAGES_RANDOM_PREF";
    private static final String SESSION_PAGES_LANGLINK_PREF_NAME = "SESSION_PAGES_LANGLINK_PREF";
    private static final String SESSION_PAGES_INTERNAL_PREF_NAME = "SESSION_PAGES_INTERNAL_PREF";
    private static final String SESSION_PAGES_EXTERNAL_PREF_NAME = "SESSION_PAGES_EXTERNAL_PREF";
    private static final String SESSION_PAGES_HISTORY_PREF_NAME = "SESSION_PAGES_HISTORY_PREF";
    private static final String SESSION_PAGES_SAVED_PREF_NAME = "SESSION_PAGES_SAVED_PREF";
    private static final String SESSION_PAGES_BACK_PREF_NAME = "SESSION_PAGES_BACK_PREF";

    private final Date lastEventTime;
    private int pagesFromSearch;
    private int pagesFromRandom;
    private int pagesFromLanglink;
    private int pagesFromInternal;
    private int pagesFromExternal;
    private int pagesFromHistory;
    private int pagesFromSaved;
    private int pagesFromBack;

    public SessionFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION, Funnel.SAMPLE_LOG_DISABLE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);

        lastEventTime = new Date(prefs.getLong(SESSION_TIMESTAMP_PREF_NAME, 0));
        pagesFromSearch = prefs.getInt(SESSION_PAGES_SEARCH_PREF_NAME, 0);
        pagesFromRandom = prefs.getInt(SESSION_PAGES_RANDOM_PREF_NAME, 0);
        pagesFromLanglink = prefs.getInt(SESSION_PAGES_LANGLINK_PREF_NAME, 0);
        pagesFromInternal = prefs.getInt(SESSION_PAGES_INTERNAL_PREF_NAME, 0);
        pagesFromExternal = prefs.getInt(SESSION_PAGES_EXTERNAL_PREF_NAME, 0);
        pagesFromHistory = prefs.getInt(SESSION_PAGES_HISTORY_PREF_NAME, 0);
        pagesFromSaved = prefs.getInt(SESSION_PAGES_SAVED_PREF_NAME, 0);
        pagesFromBack = prefs.getInt(SESSION_PAGES_BACK_PREF_NAME, 0);

        touchSession();
    }

    /**
     * Save the state of the current session. To be called when the main Activity is stopped,
     * so that we don't have to save its state every time a single parameter is modified.
     */
    public void persistSession() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApp());
        prefs.edit().putLong(SESSION_TIMESTAMP_PREF_NAME, lastEventTime.getTime())
             .putInt(SESSION_PAGES_SEARCH_PREF_NAME, pagesFromSearch)
             .putInt(SESSION_PAGES_RANDOM_PREF_NAME, pagesFromRandom)
             .putInt(SESSION_PAGES_LANGLINK_PREF_NAME, pagesFromLanglink)
             .putInt(SESSION_PAGES_INTERNAL_PREF_NAME, pagesFromInternal)
             .putInt(SESSION_PAGES_EXTERNAL_PREF_NAME, pagesFromExternal)
             .putInt(SESSION_PAGES_HISTORY_PREF_NAME, pagesFromHistory)
             .putInt(SESSION_PAGES_SAVED_PREF_NAME, pagesFromSaved)
             .putInt(SESSION_PAGES_BACK_PREF_NAME, pagesFromBack).apply();
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    /**
     * Update the timestamp for the current session. If the last-updated time is older than the defined
     * timeout period, then consider the current session as over, and send the event!
     */
    private void touchSession() {
        Date now = new Date();
        if (lastEventTime.getTime() == 0) {
            //the app was just launched, and there's no record of a previous session,
            //so send a session "start" event
            log(
                    "action", "start"
            );
        } else if (now.getTime() - lastEventTime.getTime() > (SESSION_TIMEOUT_SECONDS * DateUtils.SECOND_IN_MILLIS)) {

            // the last session is over!
            log(
                    "action", "end",
                    "pagesViewedFromSearch", pagesFromSearch,
                    "pagesViewedFromRandom", pagesFromRandom,
                    "pagesViewedFromLanglink", pagesFromLanglink,
                    "pagesViewedFromExternal", pagesFromExternal,
                    "pagesViewedFromHistory", pagesFromHistory,
                    "pagesViewedFromSaved", pagesFromSaved,
                    "totalPagesViewed", pagesFromSearch + pagesFromRandom + pagesFromLanglink + pagesFromInternal
                            + pagesFromExternal + pagesFromHistory + pagesFromSaved,
                    "backPressed", pagesFromBack
            );

            // start a new session by clearing everything.
            // We don't actually need to send another "start" event, since the "end" event that we just sent
            // implies that a new session is starting.
            pagesFromSearch = 0;
            pagesFromRandom = 0;
            pagesFromLanglink = 0;
            pagesFromInternal = 0;
            pagesFromExternal = 0;
            pagesFromHistory = 0;
            pagesFromSaved = 0;
            pagesFromBack = 0;
        }
        lastEventTime.setTime(now.getTime());
    }

    public void pageViewed(HistoryEntry entry) {
        touchSession();
        switch (entry.getSource()) {
            case HistoryEntry.SOURCE_SEARCH:
                pagesFromSearch++;
                break;
            case HistoryEntry.SOURCE_RANDOM:
                pagesFromRandom++;
                break;
            case HistoryEntry.SOURCE_LANGUAGE_LINK:
                pagesFromLanglink++;
                break;
            case HistoryEntry.SOURCE_EXTERNAL_LINK:
                pagesFromExternal++;
                break;
            case HistoryEntry.SOURCE_HISTORY:
                pagesFromHistory++;
                break;
            case HistoryEntry.SOURCE_SAVED_PAGE:
                pagesFromSaved++;
                break;
            default:
                pagesFromInternal++;
        }
    }

    public void backPressed() {
        touchSession();
        pagesFromBack++;
    }
}
