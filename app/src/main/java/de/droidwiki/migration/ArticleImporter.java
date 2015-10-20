package de.droidwiki.migration;

import android.content.Context;
import org.json.JSONObject;
import de.droidwiki.page.PageTitle;
import de.droidwiki.Site;
import de.droidwiki.WikipediaApp;
import de.droidwiki.analytics.SavedPagesFunnel;
import de.droidwiki.savedpages.SavedPage;
import de.droidwiki.savedpages.SavedPagePersister;

import java.util.List;

public class ArticleImporter {
    private final WikipediaApp app;

    public ArticleImporter(Context context) {
        app = (WikipediaApp) context.getApplicationContext();
    }

    public void importArticles(List<JSONObject> articles) {
        SavedPagePersister persister = (SavedPagePersister) app.getPersister(SavedPage.class);

        for (JSONObject item : articles) {
            PageTitle title = titleForItem(item);
            SavedPage savedPage = new SavedPage(title);
            SavedPagesFunnel funnel = app.getFunnelManager().getSavedPagesFunnel(title.getSite());
            funnel.logImport();
            persister.upsert(savedPage, SavedPage.PERSISTENCE_HELPER.SELECTION_KEYS);
        }
    }

    private PageTitle titleForItem(JSONObject item) {
        return new PageTitle(null, item.optString("title"), Site.forLanguage(item.optString("lang")));
    }
}
