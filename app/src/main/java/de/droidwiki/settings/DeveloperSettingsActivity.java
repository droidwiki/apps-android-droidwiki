package de.droidwiki.settings;

import android.content.Context;
import android.content.Intent;

import de.droidwiki.activity.PlatformSingleFragmentActivity;

public class DeveloperSettingsActivity extends PlatformSingleFragmentActivity<DeveloperSettingsFragment> {
    public static Intent newIntent(Context context) {
        return new Intent(context, DeveloperSettingsActivity.class);
    }

    @Override
    public DeveloperSettingsFragment createFragment() {
        return DeveloperSettingsFragment.newInstance();
    }

    @Override
    protected void setTheme() {
        setActionBarTheme();
    }
}