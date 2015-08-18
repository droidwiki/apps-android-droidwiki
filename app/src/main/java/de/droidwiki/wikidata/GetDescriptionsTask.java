package de.droidwiki.wikidata;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import de.droidwiki.page.PageQueryTask;
import de.droidwiki.page.PageTitle;
import de.droidwiki.Site;
import de.droidwiki.Utils;

import java.util.List;

/**
 * Populates a list of PageTitles with Wikidata descriptions for each item.
 * This task doesn't "return" anything; it simply modifies the PageTitle objects in place.
 */
public class GetDescriptionsTask extends PageQueryTask<Void> {
    private List<PageTitle> titles;

    public GetDescriptionsTask(Api api, Site site, List<PageTitle> titles) {
        super(LOW_CONCURRENCY, api, site, titles);
        this.titles = titles;
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        builder.param("prop", "pageterms")
                .param("wbptterm", "description");
    }

    @Override
    public Void processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        JSONObject terms = pageData.optJSONObject("terms");
        if (terms != null) {
            final JSONArray array = terms.optJSONArray("description");
            if (array != null && array.length() > 0) {
                for (PageTitle title : titles) {
                    if (title.getPrefixedText().equals(pageTitle.getPrefixedText())
                            || title.getDisplayText().equals(pageTitle.getDisplayText())) {
                        title.setDescription(Utils.capitalizeFirstChar(array.getString(0)));
                        break;
                    }
                }
            }
        }
        return null;
    }
}
