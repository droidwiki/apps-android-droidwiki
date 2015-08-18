package de.droidwiki.savedpages;

import android.content.Context;
import de.droidwiki.Utils;
import de.droidwiki.WikipediaApp;
import de.droidwiki.concurrency.SaneAsyncTask;

import java.io.File;

public class DeleteAllSavedPagesTask extends SaneAsyncTask<Void> {
    private final WikipediaApp app;

    public DeleteAllSavedPagesTask(Context context) {
        super(SINGLE_THREAD);
        app = (WikipediaApp) context.getApplicationContext();
    }

    @Override
    public Void performTask() throws Throwable {
        // Clear Saved Pages!
        app.getPersister(SavedPage.class).deleteAll();
        // Purge all the contents in storage.
        Utils.delete(new File(SavedPage.getSavedPagesDir()), true);
        // TODO: don't we need to funnel around, too? ;)
        return null;
    }
}
