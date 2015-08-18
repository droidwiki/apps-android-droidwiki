package de.droidwiki.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import de.droidwiki.WikipediaApp;
import de.droidwiki.activity.ActivityUtil;

/*package*/ abstract class LegacyPreferenceActivity extends PreferenceActivity
        implements PreferenceLoader {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(WikipediaApp.getInstance().getCurrentTheme().getResourceId());
        super.onCreate(savedInstanceState);
        loadPreferences();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return ActivityUtil.defaultOnOptionsItemSelected(this, item)
                || super.onOptionsItemSelected(item);
    }
}