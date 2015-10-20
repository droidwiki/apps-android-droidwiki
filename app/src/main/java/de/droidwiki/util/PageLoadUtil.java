package de.droidwiki.util;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import de.droidwiki.R;
import de.droidwiki.Site;
import de.droidwiki.WikipediaApp;
import de.droidwiki.server.PageService;
import de.droidwiki.server.PageServiceFactory;

public final class PageLoadUtil {

    @NonNull
    public static PageService getApiService(Site site) {
        return PageServiceFactory.create(site);
    }

    // TODO: use getResources().getDimensionPixelSize()?  Define leadImageWidth with px, not dp?
    public static int calculateLeadImageWidth() {
        Resources res = WikipediaApp.getInstance().getResources();
        return (int) (res.getDimension(R.dimen.leadImageWidth) / res.getDisplayMetrics().density);
    }

    private PageLoadUtil() { }
}
