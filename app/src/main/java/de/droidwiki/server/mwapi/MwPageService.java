package de.droidwiki.server.mwapi;

import de.droidwiki.Site;
import de.droidwiki.WikipediaApp;
import de.droidwiki.server.PageCombo;
import de.droidwiki.server.PageLead;
import de.droidwiki.server.PageRemaining;
import de.droidwiki.server.PageService;
import de.droidwiki.settings.RbSwitch;
import de.droidwiki.zero.WikipediaZeroHandler;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Retrofit web service client for MediaWiki PHP API.
 */
public class MwPageService implements PageService {
    private final MwPageEndpoints webService;
    private WikipediaZeroHandler responseHeaderHandler;

    public MwPageService(final Site site) {
        responseHeaderHandler = WikipediaApp.getInstance().getWikipediaZeroHandler();
        webService = MwPageEndpointsCache.INSTANCE.getMwPageEndpoints(site);
    }

    @Override
    public void pageLead(String title, int leadImageThumbWidth, boolean noImages,
                         final PageLead.Callback cb) {
        webService.pageLead(title, leadImageThumbWidth, optional(noImages), new Callback<MwPageLead>() {
            @Override
            public void success(MwPageLead pageLead, Response response) {
                responseHeaderHandler.onHeaderCheck(response);
                cb.success(pageLead, response);
            }

            @Override
            public void failure(RetrofitError error) {
                cb.failure(error);
            }
        });
    }

    @Override
    public void pageRemaining(String title, boolean noImages, final PageRemaining.Callback cb) {
        webService.pageRemaining(title, optional(noImages), new Callback<MwPageRemaining>() {
            @Override
            public void success(MwPageRemaining pageRemaining, Response response) {
                RbSwitch.INSTANCE.onMwSuccess();
                cb.success(pageRemaining, response);
            }

            @Override
            public void failure(RetrofitError error) {
                cb.failure(error);
            }
        });
    }

    @Override
    public void pageCombo(String title, boolean noImages, final PageCombo.Callback cb) {
        webService.pageCombo(title, optional(noImages), new Callback<MwPageCombo>() {
            @Override
            public void success(MwPageCombo pageCombo, Response response) {
                cb.success(pageCombo, response);
            }

            @Override
            public void failure(RetrofitError error) {
                cb.failure(error);
            }
        });
    }

    /**
     * Optional boolean Retrofit parameter.
     * We don't want to send the query parameter at all when it's false since the presence of the
     * alone is enough to trigger the truthy behavior.
     */
    private Boolean optional(boolean param) {
        if (param) {
            return true;
        }
        return null;
    }

    /**
     * Retrofit endpoints for MW API endpoints.
     */
    interface MwPageEndpoints {
        /**
         * Gets the lead section and initial metadata of a given title.
         *
         * @param title the page title with prefix if necessary
         * @param leadImageThumbWidth one of the bucket widths for the lead image
         * @param noImages add the noimages flag to the request if true
         * @param cb a Retrofit callback which provides the populated MwPageLead object in #success
         */
        @GET("/api.php?action=mobileview&format=json&formatversion=2&prop="
                + "text%7Csections%7Clanguagecount%7Cthumb%7Cimage%7Cid%7Crevision%7Cdescription"
                + "%7Clastmodified%7Cnormalizedtitle%7Cdisplaytitle%7Cprotection%7Ceditable"
                + "&onlyrequestedsections=1&sections=0&sectionprop=toclevel%7Cline%7Canchor"
                + "&noheadings=true")
        void pageLead(@Query("page") String title, @Query("thumbsize") int leadImageThumbWidth,
                      @Query("noimages") Boolean noImages, Callback<MwPageLead> cb);

        /**
         * Gets the remaining sections of a given title.
         *
         * @param title the page title to be used including prefix
         * @param noImages add the noimages flag to the request if true
         * @param cb a Retrofit callback which provides the populated MwPageRemaining object in #success
         */
        @GET("/api.php?action=mobileview&format=json&prop="
                + "text%7Csections&onlyrequestedsections=1&sections=1-"
                + "&sectionprop=toclevel%7Cline%7Canchor&noheadings=true")
        void pageRemaining(@Query("page") String title, @Query("noimages") Boolean noImages,
                           Callback<MwPageRemaining> cb);

        /**
         * Gets all page content of a given title -- for refreshing a saved page
         * Note: the only difference in the URL from #pageLead is the sections=all instead of 0.
         *
         * @param title the page title to be used including prefix
         * @param noImages add the noimages flag to the request if true
         * @param cb a Retrofit callback which provides the populated MwPageCombo object in #success
         */
        @GET("/api.php?action=mobileview&format=json&formatversion=2&prop="
                + "text%7Csections%7Clanguagecount%7Cthumb%7Cimage%7Cid%7Crevision%7Cdescription"
                + "%7Clastmodified%7Cnormalizedtitle%7Cdisplaytitle%7Cprotection%7Ceditable"
                + "&onlyrequestedsections=1&sections=all&sectionprop=toclevel%7Cline%7Canchor"
                + "&noheadings=true")
        void pageCombo(@Query("page") String title, @Query("noimages") Boolean noImages,
                       Callback<MwPageCombo> cb);
    }
}