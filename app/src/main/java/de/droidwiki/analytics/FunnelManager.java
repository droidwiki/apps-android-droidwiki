package de.droidwiki.analytics;

import de.droidwiki.page.PageTitle;
import de.droidwiki.Site;
import de.droidwiki.WikipediaApp;

import java.util.Hashtable;

/**
 * Creates and stores analytics tracking funnels.
 */
public class FunnelManager {
    private final WikipediaApp app;
    private final Hashtable<PageTitle, EditFunnel> editFunnels = new Hashtable<>();
    private final Hashtable<Site, SavedPagesFunnel> savedPageFunnels = new Hashtable<>();

    public FunnelManager(WikipediaApp app) {
        this.app = app;
    }

    public EditFunnel getEditFunnel(PageTitle title) {
        if (!editFunnels.containsKey(title)) {
            editFunnels.put(title, new EditFunnel(app, title));
        }

        return editFunnels.get(title);
    }

    public SavedPagesFunnel getSavedPagesFunnel(Site site) {
        if (!savedPageFunnels.contains(site)) {
            savedPageFunnels.put(site, new SavedPagesFunnel(app, site));
        }

        return savedPageFunnels.get(site);
    }
}
