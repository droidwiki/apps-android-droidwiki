package org.wikipedia.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.settings.languages.WikipediaLanguagesActivity;
import org.wikipedia.theme.ThemeFittingRoomActivity;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.StringUtil;

/** UI code for app settings used by PreferenceFragment. */
class SettingsPreferenceLoader extends BasePreferenceLoader {

    /*package*/ SettingsPreferenceLoader(@NonNull PreferenceFragmentCompat fragment) {
        super(fragment);
    }

    @Override
    public void loadPreferences() {
        loadPreferences(R.xml.preferences);

        if (ReadingListSyncAdapter.isDisabledByRemoteConfig()) {
            findPreference(R.string.preference_category_sync).setVisible(false);
            findPreference(R.string.preference_key_sync_reading_lists).setVisible(false);
        }

        if (!Prefs.isZeroTutorialEnabled()) {
            loadPreferences(R.xml.preferences_zero);
        }

        findPreference(R.string.preference_key_sync_reading_lists)
                .setOnPreferenceChangeListener(new SyncReadingListsListener());

        if (ReleaseUtil.isPreBetaRelease()) {
            loadPreferences(R.xml.preferences_experimental);
            Preference offlineLibPref = findPreference(R.string.preference_key_enable_offline_library);
            offlineLibPref.setOnPreferenceChangeListener(new OfflineLibraryEnableListener());
            offlineLibPref.setSummary(StringUtil.fromHtml(getPreferenceHost().getString(R.string.preference_summary_enable_offline_library)));
        }

        loadPreferences(R.xml.preferences_about);

        updateLanguagePrefSummary();

        Preference contentLanguagePref = findPreference(R.string.preference_key_language);
        contentLanguagePref.setOnPreferenceClickListener(preference -> {
            getActivity().startActivity(new Intent(getActivity(), WikipediaLanguagesActivity.class));
            return true;
        });

        Preference themePref = findPreference(R.string.preference_key_color_theme);
        themePref.setSummary(WikipediaApp.getInstance().getCurrentTheme().getNameId());
        themePref.setOnPreferenceClickListener(preference -> {
            getActivity().startActivity(ThemeFittingRoomActivity.newIntent(getActivity()));
            return true;
        });

        findPreference(R.string.preference_key_about_wikipedia_app)
                .setOnPreferenceClickListener((preference) -> {
                    getActivity().startActivity(new Intent(getActivity(), AboutActivity.class));
                    return true;
                });
    }

    void updateLanguagePrefSummary() {
        Preference languagePref = findPreference(R.string.preference_key_language);
        // TODO: resolve RTL vs LTR with multiple languages (e.g. list contains English and Hebrew)
        languagePref.setSummary(WikipediaApp.getInstance().language().getAppLanguageLocalizedNames());
    }

    private final class SyncReadingListsListener implements Preference.OnPreferenceChangeListener {
        @Override public boolean onPreferenceChange(final Preference preference, Object newValue) {
            if (AccountUtil.isLoggedIn()) {
                if (newValue == Boolean.TRUE) {
                    ((SwitchPreferenceCompat) preference).setChecked(true);
                    ReadingListSyncAdapter.setSyncEnabledWithSetup();
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getActivity().getString(R.string.preference_dialog_of_turning_off_reading_list_sync_title, AccountUtil.getUserName()))
                            .setMessage(getActivity().getString(R.string.preference_dialog_of_turning_off_reading_list_sync_text, AccountUtil.getUserName()))
                            .setPositiveButton(R.string.reading_lists_confirm_remote_delete_yes, new DeleteRemoteListsYesListener(preference))
                            .setNegativeButton(R.string.reading_lists_confirm_remote_delete_no, null)
                            .show();
                }
            } else {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.reading_list_preference_login_to_enable_sync_dialog_title)
                        .setMessage(R.string.reading_list_preference_login_to_enable_sync_dialog_text)
                        .setPositiveButton(R.string.reading_list_preference_login_to_enable_sync_dialog_login,
                                (dialogInterface, i) -> {
                                    Intent loginIntent = LoginActivity.newIntent(getActivity(),
                                            LoginFunnel.SOURCE_SETTINGS);

                                    getActivity().startActivity(loginIntent);
                                })
                        .setNegativeButton(R.string.reading_list_preference_login_to_enable_sync_dialog_cancel, null)
                        .show();
            }
            // clicks are handled and preferences updated accordingly; don't pass the result through
            return false;
        }
    }

    void updateSyncReadingListsPrefSummary() {
        // no-op
    }

    private final class OfflineLibraryEnableListener implements Preference.OnPreferenceChangeListener {
        @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (((Boolean) newValue)) {
                ((BaseActivity) getActivity()).searchOfflineCompilationsWithPermission(true);
            }
            return true;
        }
    }

    private final class DeleteRemoteListsYesListener implements DialogInterface.OnClickListener {
        private Preference preference;

        private DeleteRemoteListsYesListener(Preference preference) {
            this.preference = preference;
        }

        @Override public void onClick(DialogInterface dialog, int which) {
            ((SwitchPreferenceCompat) preference).setChecked(false);
            Prefs.setReadingListSyncEnabled(false);
            Prefs.setReadingListsRemoteSetupPending(false);
            Prefs.setReadingListsRemoteDeletePending(true);
            ReadingListSyncAdapter.manualSync();
        }
    }
}
