package de.droidwiki.savedpages;

import org.json.JSONObject;
import de.droidwiki.page.PageTitle;
import de.droidwiki.concurrency.SaneAsyncTask;

/** To load the file with the image source URL mappings in the background. */
public class LoadSavedPageUrlMapTask extends SaneAsyncTask<JSONObject> {
    private final PageTitle title;

    public LoadSavedPageUrlMapTask(PageTitle title) {
        super(SINGLE_THREAD);
        this.title = title;
    }

    @Override
    public JSONObject performTask() throws Throwable {
        SavedPage savedPage = new SavedPage(title);
        return savedPage.readUrlMapFromFileSystem();
    }
}
