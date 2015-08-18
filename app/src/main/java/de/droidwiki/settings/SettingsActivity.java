package de.droidwiki.settings;

import de.droidwiki.activity.PlatformSingleFragmentActivity;

public class SettingsActivity extends PlatformSingleFragmentActivity<SettingsFragment> {
    public static final int ACTIVITY_REQUEST_SHOW_SETTINGS = 1;
    public static final int ACTIVITY_RESULT_LANGUAGE_CHANGED = 1;
    public static final int ACTIVITY_RESULT_LOGOUT = 2;

    @Override
    public SettingsFragment createFragment() {
        return SettingsFragment.newInstance();
    }
}
