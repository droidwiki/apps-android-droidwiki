package de.droidwiki.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.internal.view.ContextThemeWrapper;

import de.droidwiki.BuildConfig;
import de.droidwiki.WikipediaApp;
import de.droidwiki.util.StringUtil;

/**
 * UI code for app settings, shared between PreferenceActivity (GB) and PreferenceFragment (HC+).
 */
public class SettingsPreferenceLoader extends BasePreferenceLoader {
    private final Activity activity;

    /*package*/ SettingsPreferenceLoader(@NonNull PreferenceActivity activity) {
        super(activity);
        this.activity = activity;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    /*package*/ SettingsPreferenceLoader(@NonNull PreferenceFragment fragment) {
        super(fragment);
        this.activity = fragment.getActivity();
    }

    @Override
    public void loadPreferences() {
        loadPreferences(de.droidwiki.R.xml.preferences);

        Preference logoutPref = findPreference(de.droidwiki.R.string.preference_key_logout);
        if (!WikipediaApp.getInstance().getUserInfoStorage().isLoggedIn()) {
            logoutPref.setEnabled(false);
            logoutPref.setSummary(getString(de.droidwiki.R.string.preference_summary_notloggedin));
        }
        logoutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                activity.setResult(SettingsActivity.ACTIVITY_RESULT_LOGOUT);
                activity.finish();
                return true;
            }
        });

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

    private Preference findPreference(@StringRes int id) {
        return findPreference(getString(id));
    }

    private String getString(@StringRes int id) {
        return activity.getString(id);
    }
}
