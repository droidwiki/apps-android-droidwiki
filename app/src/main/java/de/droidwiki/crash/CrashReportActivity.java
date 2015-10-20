package de.droidwiki.crash;

import android.content.Intent;
import android.os.Bundle;

import de.droidwiki.WikipediaApp;
import de.droidwiki.activity.ActivityUtil;
import de.droidwiki.activity.CompatSingleFragmentActivity;

public class CrashReportActivity extends CompatSingleFragmentActivity<CrashReportFragment>
        implements CrashReportFragmentCallback {
    @Override
    protected CrashReportFragment createFragment() {
        return CrashReportFragment.newInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WikipediaApp.getInstance().checkCrashes(this);
    }

    @Override
    public void onStartOver() {
        int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK;
        Intent intent = ActivityUtil.getLaunchIntent(this).addFlags(flags);
        startActivity(intent);
        finish();
    }

    @Override
    public void onQuit() {
        finish();
    }
}
