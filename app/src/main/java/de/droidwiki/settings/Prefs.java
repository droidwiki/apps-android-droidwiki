package de.droidwiki.settings;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.droidwiki.WikipediaApp;
import de.droidwiki.data.GsonMarshaller;
import de.droidwiki.data.TabUnmarshaller;
import de.droidwiki.page.tabs.Tab;
import de.droidwiki.theme.Theme;

import java.util.Collections;
import java.util.List;

import static de.droidwiki.settings.PrefsIoUtil.contains;
import static de.droidwiki.settings.PrefsIoUtil.getBoolean;
import static de.droidwiki.settings.PrefsIoUtil.getInt;
import static de.droidwiki.settings.PrefsIoUtil.getKey;
import static de.droidwiki.settings.PrefsIoUtil.getLong;
import static de.droidwiki.settings.PrefsIoUtil.getString;
import static de.droidwiki.settings.PrefsIoUtil.remove;
import static de.droidwiki.settings.PrefsIoUtil.setBoolean;
import static de.droidwiki.settings.PrefsIoUtil.setInt;
import static de.droidwiki.settings.PrefsIoUtil.setLong;
import static de.droidwiki.settings.PrefsIoUtil.setString;

/** Shared preferences utility for convenient POJO access. */
public final class Prefs {
    @Nullable
    public static String getAppChannel() {
        return getString(de.droidwiki.R.string.preference_key_app_channel, null);
    }

    public static void setAppChannel(@Nullable String channel) {
        setString(de.droidwiki.R.string.preference_key_app_channel, channel);
    }

    @NonNull
    public static String getAppChannelKey() {
        return getKey(de.droidwiki.R.string.preference_key_app_channel);
    }

    @Nullable
    public static String getAppInstallId() {
        return getString(de.droidwiki.R.string.preference_key_reading_app_install_id, null);
    }

    public static void setAppInstallId(@Nullable String id) {
        // The app install ID uses readingAppInstallID for backwards compatibility with analytics.
        setString(de.droidwiki.R.string.preference_key_reading_app_install_id, id);
    }

    @Nullable
    public static String getAppLanguageCode() {
        return getString(de.droidwiki.R.string.preference_key_language, null);
    }

    public static void setAppLanguageCode(@Nullable String code) {
        setString(de.droidwiki.R.string.preference_key_language, code);
    }

    public static int getThemeId() {
        return getInt(de.droidwiki.R.string.preference_key_color_theme, Theme.getFallback().getMarshallingId());
    }

    public static void setThemeId(int theme) {
        setInt(de.droidwiki.R.string.preference_key_color_theme, theme);
    }

    @NonNull
    public static String getCookieDomains() {
        return getString(de.droidwiki.R.string.preference_key_cookie_domains, "");
    }

    public static void setCookieDomains(@Nullable String domains) {
        setString(de.droidwiki.R.string.preference_key_cookie_domains, domains);
    }

    @NonNull
    public static String getCookiesForDomain(@NonNull String domain) {
        return getString(getCookiesForDomainKey(domain), "");
    }

    public static void setCookiesForDomain(@NonNull String domain, @Nullable String cookies) {
        setString(getCookiesForDomainKey(domain), cookies);
    }

    public static void removeCookiesForDomain(@NonNull String domain) {
        remove(getCookiesForDomainKey(domain));
    }

    public static boolean isShowDeveloperSettingsEnabled() {
        return getBoolean(de.droidwiki.R.string.preference_key_show_developer_settings,
                WikipediaApp.getInstance().isDevRelease());
    }

    public static void setShowDeveloperSettingsEnabled(boolean enabled) {
        setBoolean(de.droidwiki.R.string.preference_key_show_developer_settings, enabled);
    }

    @NonNull
    public static String getEditTokenWikis() {
        return getString(de.droidwiki.R.string.preference_key_edittoken_wikis, "");
    }

    public static void setEditTokenWikis(@Nullable String wikis) {
        setString(de.droidwiki.R.string.preference_key_edittoken_wikis, wikis);
    }

    @Nullable
    public static String getEditTokenForWiki(@NonNull String wiki) {
        return getString(getEditTokenForWikiKey(wiki), null);
    }

    public static void setEditTokenForWiki(@NonNull String wiki, @Nullable String token) {
        setString(getEditTokenForWikiKey(wiki), token);
    }

    public static void removeEditTokenForWiki(@NonNull String wiki) {
        remove(getEditTokenForWikiKey(wiki));
    }

    public static int getLinkPreviewVersion() {
        return getInt(de.droidwiki.R.string.preference_key_link_preview_version, 0);
    }

    public static void setLinkPreviewVersion(int version) {
        setInt(de.droidwiki.R.string.preference_key_link_preview_version, version);
    }

    public static boolean hasLinkPreviewVersion() {
        return contains(de.droidwiki.R.string.preference_key_link_preview_version);
    }

    public static void removeLoginUsername() {
        remove(de.droidwiki.R.string.preference_key_login_username);
    }

    @Nullable
    public static String getLoginPassword() {
        return getString(de.droidwiki.R.string.preference_key_login_password, null);
    }

    public static void setLoginPassword(@Nullable String password) {
        setString(de.droidwiki.R.string.preference_key_login_password, password);
    }

    public static boolean hasLoginPassword() {
        return contains(de.droidwiki.R.string.preference_key_login_password);
    }

    public static void removeLoginPassword() {
        remove(de.droidwiki.R.string.preference_key_login_password);
    }

    public static int getLoginUserId() {
        return getInt(de.droidwiki.R.string.preference_key_login_user_id, 0);
    }

    public static void setLoginUserId(int id) {
        setInt(de.droidwiki.R.string.preference_key_login_user_id, id);
    }

    public static void removeLoginUserId() {
        remove(de.droidwiki.R.string.preference_key_login_user_id);
    }

    @Nullable
    public static String getLoginUsername() {
        return getString(de.droidwiki.R.string.preference_key_login_username, null);
    }

    public static void setLoginUsername(@Nullable String username) {
        setString(de.droidwiki.R.string.preference_key_login_username, username);
    }

    public static boolean hasLoginUsername() {
        return contains(de.droidwiki.R.string.preference_key_login_username);
    }

    @Nullable
    public static String getMruLanguageCodeCsv() {
        return getString(de.droidwiki.R.string.preference_key_language_mru, null);
    }

    public static void setMruLanguageCodeCsv(@Nullable String csv) {
        setString(de.droidwiki.R.string.preference_key_language_mru, csv);
    }

    @NonNull
    public static String getRemoteConfigJson() {
        return getString(de.droidwiki.R.string.preference_key_remote_config, "{}");
    }

    public static void setRemoteConfigJson(@Nullable String json) {
        setString(de.droidwiki.R.string.preference_key_remote_config, json);
    }

    public static void setTabs(@NonNull List<Tab> tabs) {
        setString(de.droidwiki.R.string.preference_key_tabs, GsonMarshaller.marshal(tabs));
    }

    @NonNull
    public static List<Tab> getTabs() {
        return hasTabs()
                ? TabUnmarshaller.unmarshal(getString(de.droidwiki.R.string.preference_key_tabs, null))
                : Collections.<Tab>emptyList();
    }

    public static boolean hasTabs() {
        return contains(de.droidwiki.R.string.preference_key_tabs);
    }

    public static int getTextSizeMultiplier() {
        return getInt(de.droidwiki.R.string.preference_key_text_size_multiplier, 0);
    }

    public static void setTextSizeMultiplier(int multiplier) {
        setInt(de.droidwiki.R.string.preference_key_text_size_multiplier, multiplier);
    }

    public static boolean isEventLoggingEnabled() {
        return getBoolean(de.droidwiki.R.string.preference_key_eventlogging_opt_in, true);
    }

    public static boolean isExperimentalHtmlPageLoadEnabled() {
        return getBoolean(de.droidwiki.R.string.preference_key_exp_html_page_load, false);
    }

    public static void setExperimentalHtmlPageLoadEnabled(boolean enabled) {
        setBoolean(de.droidwiki.R.string.preference_key_exp_html_page_load, enabled);
    }

    public static boolean isRESTBaseJsonPageLoadEnabled() {
        return getBoolean(de.droidwiki.R.string.preference_key_exp_json_page_load, false);
    }

    public static void setExperimentalJsonPageLoadEnabled(boolean enabled) {
        setBoolean(de.droidwiki.R.string.preference_key_exp_json_page_load, enabled);
    }

    public static long getLastRunTime(@NonNull String task) {
        return getLong(getLastRunTimeKey(task), 0);
    }

    public static void setLastRunTime(@NonNull String task, long time) {
        setLong(getLastRunTimeKey(task), time);
    }

    public static boolean isShowZeroInterstitialEnabled() {
        return false;
    }

    public static boolean isSelectTextTutorialEnabled() {
        return getBoolean(de.droidwiki.R.string.preference_key_select_text_tutorial_enabled, true);
    }

    public static void setSelectTextTutorialEnabled(boolean enabled) {
        setBoolean(de.droidwiki.R.string.preference_key_select_text_tutorial_enabled, enabled);
    }

    public static boolean isShareTutorialEnabled() {
        return getBoolean(de.droidwiki.R.string.preference_key_share_tutorial_enabled, true);
    }

    public static void setShareTutorialEnabled(boolean enabled) {
        setBoolean(de.droidwiki.R.string.preference_key_share_tutorial_enabled, enabled);
    }

    public static boolean isFeatureSelectTextAndShareTutorialEnabled() {
        return getBoolean(de.droidwiki.R.string.preference_key_feature_select_text_and_share_tutorials_enabled, true);
    }

    public static void setFeatureSelectTextAndShareTutorialEnabled(boolean enabled) {
        setBoolean(de.droidwiki.R.string.preference_key_feature_select_text_and_share_tutorials_enabled, enabled);
    }

    public static boolean hasFeatureSelectTextAndShareTutorial() {
        return contains(de.droidwiki.R.string.preference_key_feature_select_text_and_share_tutorials_enabled);
    }

    public static boolean isTocTutorialEnabled() {
        return getBoolean(de.droidwiki.R.string.preference_key_toc_tutorial_enabled, true);
    }

    public static void setTocTutorialEnabled(boolean enabled) {
        setBoolean(de.droidwiki.R.string.preference_key_toc_tutorial_enabled, enabled);
    }

    public static boolean isImageDownloadEnabled() {
        return getBoolean(de.droidwiki.R.string.preference_key_show_images, true);
    }

    private static String getCookiesForDomainKey(@NonNull String domain) {
        return getKey(de.droidwiki.R.string.preference_key_cookies_for_domain_format, domain);
    }

    private static String getLastRunTimeKey(@NonNull String task) {
        return getKey(de.droidwiki.R.string.preference_key_last_run_time_format, task);
    }

    private static String getEditTokenForWikiKey(String wiki) {
        return getKey(de.droidwiki.R.string.preference_key_edittoken_for_wiki_format, wiki);
    }

    private Prefs() { }
}
