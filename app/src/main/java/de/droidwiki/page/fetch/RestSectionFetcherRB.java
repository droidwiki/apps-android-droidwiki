package de.droidwiki.page.fetch;

import de.droidwiki.page.PageTitle;

/**
 * @see SectionsFetcherRB
 */
public class RestSectionFetcherRB extends SectionsFetcherRB implements RestSectionFetcher {
    public RestSectionFetcherRB(PageTitle title, String sectionsRequested, boolean downloadImages) {
        super(title, sectionsRequested, downloadImages);
    }
}
