package de.droidwiki.savedpages;

import de.droidwiki.page.PageTitle;
import de.droidwiki.concurrency.SaneAsyncTask;
import de.droidwiki.page.Page;

public class LoadSavedPageTask extends SaneAsyncTask<Page> {
    private final PageTitle title;

    public LoadSavedPageTask(PageTitle title) {
        super(SINGLE_THREAD);
        this.title = title;
    }

    @Override
    public Page performTask() throws Throwable {
        SavedPage savedPage = new SavedPage(title);
        return savedPage.readFromFileSystem();
    }
}
