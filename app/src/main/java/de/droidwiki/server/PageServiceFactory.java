package de.droidwiki.server;

import de.droidwiki.Site;
import de.droidwiki.server.mwapi.MwPageService;
import de.droidwiki.server.restbase.RbPageService;
import de.droidwiki.settings.RbSwitch;

/**
 * This redirection exists because we want to be able to switch between the traditional
 * MediaWiki PHP API and the new Nodejs Mobile Content Service hosted in the RESTBase
 * infrastructure.
 */
public final class PageServiceFactory {
    public static PageService create(Site site) {
        if (RbSwitch.INSTANCE.isRestBaseEnabled()) {
            return new RbPageService(site);
        } else {
            return new MwPageService(site);
        }
    }

    private PageServiceFactory() {
    }
}