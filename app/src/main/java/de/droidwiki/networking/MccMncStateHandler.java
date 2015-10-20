package de.droidwiki.networking;

import android.content.Context;
import org.mediawiki.api.json.Api;
import de.droidwiki.Site;
import de.droidwiki.WikipediaApp;
import de.droidwiki.util.NetworkUtils;

import java.util.HashMap;

public class MccMncStateHandler {
    private boolean mccMncSent = false;
    private WikipediaApp app;

    /**
     * Enriches request to have a header with the MCC-MNC (mobile operator code) if
     * cellular data connection is the active one and it hasn't already been sent
     * and the user isn't currently opted out of event logging.
     * http://lists.wikimedia.org/pipermail/wikimedia-l/2014-April/071131.html
     * @param ctx Application context
     * @param site Currently active site
     * @param customHeaders Hashmap of custom headers
     * @return
     */
    public Api makeApiWithMccMncHeaderEnrichment(Context ctx, Site site, HashMap<String, String> customHeaders) {
        if (this.app == null) {
            this.app = (WikipediaApp)ctx;
        }
        // Forget about it if it was already sent or user opted out of logging or the API server isn't a mobile Wikipedia.
        if (this.mccMncSent
            || !app.isEventLoggingEnabled()
            || !(site.getApiDomain().contains("www.droidwiki.de"))) {
            return null;
        }
        String mccMnc = NetworkUtils.getMccMnc(ctx);
        if (mccMnc != null) {
            customHeaders.put("X-MCCMNC", mccMnc);
            this.mccMncSent = true;
            return new Api(site.getApiDomain(), customHeaders);
        }
        return null;
    }
}
