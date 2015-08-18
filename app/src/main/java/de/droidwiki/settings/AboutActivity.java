package de.droidwiki.settings;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import de.droidwiki.BuildConfig;
import de.droidwiki.activity.ActivityUtil;
import de.droidwiki.activity.ThemedActionBarActivity;
import de.droidwiki.Utils;
import de.droidwiki.WikipediaApp;
import de.droidwiki.util.FeedbackUtil;

public class AboutActivity extends ThemedActionBarActivity {
    private static final String KEY_SCROLL_X = "KEY_SCROLL_X";
    private static final String KEY_SCROLL_Y = "KEY_SCROLL_Y";

    private ScrollView mScrollView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(de.droidwiki.R.layout.activity_about);

        mScrollView = (ScrollView) findViewById(de.droidwiki.R.id.about_scrollview);
        ((TextView) findViewById(de.droidwiki.R.id.about_translators)).setText(Html.fromHtml(getString(de.droidwiki.R.string.about_translators_translatewiki)));
        // ((TextView) findViewById(de.droidwiki.R.id.about_wmf)).setText(Html.fromHtml(getString(de.droidwiki.R.string.about_wmf)));
        ((TextView) findViewById(de.droidwiki.R.id.about_version_text)).setText(BuildConfig.VERSION_NAME);
        ((TextView) findViewById(de.droidwiki.R.id.send_feedback_text)).setText(Html.fromHtml(
                "<a href=\"mailto:info@droidwiki.de?subject=Android App "
                + BuildConfig.VERSION_NAME
                + " Feedback\">"
                + getString(de.droidwiki.R.string.send_feedback)
                + "</a>"));

        findViewById(de.droidwiki.R.id.about_logo_image).setOnClickListener(new AboutLogoClickListener(this));

        //if there's no Email app, hide the Feedback link.
        if (!Utils.mailAppExists(this)) {
            findViewById(de.droidwiki.R.id.send_feedback_text).setVisibility(View.GONE);
        }

        WikipediaApp.getInstance().adjustDrawableToTheme(((ImageView) findViewById(de.droidwiki.R.id.about_logo_image)).getDrawable());

        makeEverythingClickable((ViewGroup) findViewById(de.droidwiki.R.id.about_container));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return ActivityUtil.defaultOnOptionsItemSelected(this, item)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SCROLL_X, mScrollView.getScrollX());
        outState.putInt(KEY_SCROLL_Y, mScrollView.getScrollY());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final int x = savedInstanceState.getInt(KEY_SCROLL_X);
        final int y = savedInstanceState.getInt(KEY_SCROLL_Y);
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.scrollTo(x, y);
            }
        });
    }

    private void makeEverythingClickable(ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            if (vg.getChildAt(i) instanceof ViewGroup) {
                makeEverythingClickable((ViewGroup)vg.getChildAt(i));
            } else if (vg.getChildAt(i) instanceof TextView) {
                TextView tv = (TextView) vg.getChildAt(i);
                tv.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private static class AboutLogoClickListener implements View.OnClickListener {
        private static final int SECRET_CLICK_LIMIT = 7;

        private final Activity mActivity;
        private int mSecretClickCount;

        public AboutLogoClickListener(Activity activity) {
            mActivity = activity;
        }

        @Override
        public void onClick(View v) {
            ++mSecretClickCount;
            if (isSecretClickLimitMet()) {
                if (Prefs.isShowDeveloperSettingsEnabled()) {
                    showSettingAlreadyEnabledMessage();
                } else {
                    Prefs.setShowDeveloperSettingsEnabled(true);
                    showSettingEnabledMessage();
                }
            }
        }

        private boolean isSecretClickLimitMet() {
            return mSecretClickCount == SECRET_CLICK_LIMIT;
        }

        private void showSettingEnabledMessage() {
            FeedbackUtil.showMessage(mActivity, de.droidwiki.R.string.show_developer_settings_enabled);
        }

        private void showSettingAlreadyEnabledMessage() {
            FeedbackUtil.showMessage(mActivity,
                    de.droidwiki.R.string.show_developer_settings_already_enabled);
        }
    }
}
