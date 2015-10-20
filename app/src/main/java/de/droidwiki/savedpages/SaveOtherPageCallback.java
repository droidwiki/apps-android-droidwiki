package de.droidwiki.savedpages;

import de.droidwiki.WikipediaApp;
import de.droidwiki.page.Page;
import de.droidwiki.page.PageTitle;
import de.droidwiki.server.PageCombo;
import de.droidwiki.util.log.L;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Common code for saving a page which is not the current page.
 * Useful for refreshing a saved page, or for saving a page from a link, search result, disambig
 * entry or similar.
 */
public abstract class SaveOtherPageCallback implements PageCombo.Callback {
    private final PageTitle title;

    public SaveOtherPageCallback(PageTitle title) {
        this.title = title;
    }

    @Override
    public void success(PageCombo pageCombo, Response response) {
        L.v(response.getUrl());

        if (pageCombo.hasError()) {
            onError();
            return;
        }

        final Page page = pageCombo.toPage(title);
        new SavePageTask(WikipediaApp.getInstance(), page.getTitle(), page) {
            @Override
            public void onFinish(Boolean result) {
                L.d("Downloaded page " + title.getDisplayText());
                onComplete();
            }
        }.execute();
    }

    @Override
    public void failure(RetrofitError error) {
        L.e("Download page error: " + error);
        onError();
    }

    protected abstract void onComplete();

    protected abstract void onError();
}