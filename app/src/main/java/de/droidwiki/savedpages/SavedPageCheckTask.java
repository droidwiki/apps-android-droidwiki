package de.droidwiki.savedpages;

import de.droidwiki.WikipediaApp;
import de.droidwiki.concurrency.SaneAsyncTask;
import de.droidwiki.page.PageTitle;

import android.util.Log;

public class SavedPageCheckTask extends SaneAsyncTask<Boolean> {
    private final PageTitle title;
    private final WikipediaApp app;

    public SavedPageCheckTask(PageTitle title, WikipediaApp app) {
        super(SINGLE_THREAD);
        this.title = title;
        this.app = app;
    }

    @Override
    public Boolean performTask() throws Throwable {
        return SavedPage.PERSISTENCE_HELPER.savedPageExists(app, title);
    }

    @Override
    public void onCatch(Throwable caught) {
        Log.w("SavedPageCheckTask", "Caught " + caught.getMessage(), caught);
    }
}
