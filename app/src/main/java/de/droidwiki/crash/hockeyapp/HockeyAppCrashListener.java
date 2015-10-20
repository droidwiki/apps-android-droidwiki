package de.droidwiki.crash.hockeyapp;

import net.hockeyapp.android.CrashManagerListener;

public abstract class HockeyAppCrashListener extends CrashManagerListener {
    public abstract void onCrash();
}