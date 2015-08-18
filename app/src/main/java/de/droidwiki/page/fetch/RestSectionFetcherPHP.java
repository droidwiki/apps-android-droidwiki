package de.droidwiki.page.fetch;

import de.droidwiki.page.PageTitle;

/**
 * @see SectionsFetcherPHP
 */
public class RestSectionFetcherPHP extends SectionsFetcherPHP implements RestSectionFetcher {
    public RestSectionFetcherPHP(PageTitle title, String sectionsRequested, boolean downloadImages) {
        super(title, sectionsRequested, downloadImages);
    }
}
