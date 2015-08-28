package de.droidwiki.page.linkpreview;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import de.droidwiki.WikipediaApp;
import de.droidwiki.page.PageQueryTask;
import de.droidwiki.page.PageTitle;

public class PreviewFetchTask extends PageQueryTask<LinkPreviewContents> {
    private final PageTitle title;

    public PreviewFetchTask(Api api, PageTitle title) {
        super(LOW_CONCURRENCY, api, title.getSite(), title);
        this.title = title;
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "extracts|pageimages|pageterms")
               .param("redirects", "true")
               .param("exchars", "512")
               //.param("exsentences", "2")
               .param("explaintext", "true")
               .param("piprop", "thumbnail|name")
               .param("pithumbsize", Integer.toString(WikipediaApp.PREFERRED_THUMB_SIZE));
    }

    @Override
    public LinkPreviewContents processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        return new LinkPreviewContents(pageData, title.getSite());
    }
}
