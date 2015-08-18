package de.droidwiki.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import de.droidwiki.activity.PlatformSingleFragmentActivity;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DeveloperSettingsActivity extends PlatformSingleFragmentActivity<DeveloperSettingsFragment> {
    public static Intent newIntent(Context context) {
        return new Intent(context, DeveloperSettingsActivity.class);
    }

    @Override
    public DeveloperSettingsFragment createFragment() {
        return DeveloperSettingsFragment.newInstance();
    }
}