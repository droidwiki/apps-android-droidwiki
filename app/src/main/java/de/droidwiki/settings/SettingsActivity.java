package de.droidwiki.settings;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import de.droidwiki.WikipediaApp;
import de.droidwiki.activity.PlatformSingleFragmentActivity;

public class SettingsActivity extends PlatformSingleFragmentActivity<SettingsFragment> {
    public static final int ACTIVITY_REQUEST_SHOW_SETTINGS = 1;
    public static final int ACTIVITY_RESULT_LANGUAGE_CHANGED = 1;
    public static final int ACTIVITY_RESULT_LOGOUT = 2;

    private Tracker mTracker;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTracker = WikipediaApp.getInstance().getDefaultTracker();
        Log.i("PageFragment", "Setting screen name: Settings");
        mTracker.setScreenName("Activity~Settings");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public SettingsFragment createFragment() {
        return SettingsFragment.newInstance();
    }
}
