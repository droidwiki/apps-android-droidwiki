package de.droidwiki.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import de.droidwiki.BuildConfig;
import de.droidwiki.R;
import de.droidwiki.WikipediaApp;
import de.droidwiki.util.StringUtil;

/** UI code for app settings used by PreferenceFragment. */
public class SettingsPreferenceLoader extends BasePreferenceLoader {
    private final Activity activity;

    /*package*/ SettingsPreferenceLoader(@NonNull PreferenceFragment fragment) {
        super(fragment);
        activity = fragment.getActivity();
    }

    @Override
    public void loadPreferences() {
        loadPreferences(R.xml.preferences);

        if (!BuildConfig.APPLICATION_ID.equals("de.droidwiki")) {
            overridePackageName();
        }
    }

    /**
     * Needed for beta release since the Gradle flavors applicationId changes don't get reflected
     * to the preferences.xml
     * See https://code.google.com/p/android/issues/detail?id=57460
     */
    private void overridePackageName() {
        Preference aboutPref = findPreference("about");
        aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(activity, AboutActivity.class);
                activity.startActivity(intent);
                return true;
            }
        });
    }

    private String getString(@StringRes int id) {
        return activity.getString(id);
    }
}
