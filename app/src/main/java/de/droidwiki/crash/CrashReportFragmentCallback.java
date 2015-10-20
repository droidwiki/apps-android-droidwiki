package de.droidwiki.crash;

import de.droidwiki.activity.FragmentCallback;

public interface CrashReportFragmentCallback extends FragmentCallback {
    void onStartOver();
    void onQuit();
}