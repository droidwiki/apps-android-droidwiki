package de.droidwiki.settings;

import android.view.Menu;
import android.view.MenuItem;

/**
 * Settings activity that is specifically intended for API 10.
 * It's functionally identical to the real SettingsActivity, except that this one inherits from
 * PreferenceActivity, which was deprecated after API 10. The new SettingsActivity inherits from
 * ActionBarActivity, and uses a PreferenceFragment, all of which are necessary for all the
 * components to render properly (specifically checkboxes).
 */
public class SettingsActivityGB extends LegacyPreferenceActivity {
    @Override
    public void loadPreferences() {
        SettingsPreferenceLoader preferenceLoader = new SettingsPreferenceLoader(this);
        preferenceLoader.loadPreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(de.droidwiki.R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        prepareDeveloperSettingsMenuItem(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case de.droidwiki.R.id.developer_settings:
                launchDeveloperSettingsActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchDeveloperSettingsActivity() {
        startActivity(DeveloperSettingsActivityGB.newIntent(this));
    }

    private void prepareDeveloperSettingsMenuItem(Menu menu) {
        menu.findItem(de.droidwiki.R.id.developer_settings).setVisible(Prefs.isShowDeveloperSettingsEnabled());
    }
}
