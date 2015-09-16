package de.droidwiki.server.restbase;

import de.droidwiki.Site;
import de.droidwiki.WikipediaApp;
import de.droidwiki.server.PageCombo;
import de.droidwiki.server.PageLead;
import de.droidwiki.server.PageRemaining;
import de.droidwiki.server.PageService;
import de.droidwiki.zero.WikipediaZeroHandler;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Retrofit web service client for RESTBase Nodejs API.
 */
public class RbPageService implements PageService {
    private final RbPageEndpoints webService;
    private WikipediaZeroHandler responseHeaderHandler;

    public RbPageService(final Site site) {
        responseHeaderHandler = WikipediaApp.getInstance().getWikipediaZeroHandler();
        webService = RbPageEndpointsCache.INSTANCE.getRbPageEndpoints(site);
    }

    @Override
    public void pageLead(String title, final int leadImageThumbWidth, boolean noImages,
                         final PageLead.Callback cb) {
        webService.pageLead(title, optional(noImages), new Callback<RbPageLead>() {
            @Override
            public void success(RbPageLead pageLead, Response response) {
                responseHeaderHandler.onHeaderCheck(response);
                pageLead.setLeadImageThumbWidth(leadImageThumbWidth);
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
        webService.pageRemaining(title, optional(noImages), new Callback<RbPageRemaining>() {
            @Override
            public void success(RbPageRemaining pageRemaining, Response response) {
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
        webService.pageCombo(title, optional(noImages), new Callback<RbPageCombo>() {
            @Override
            public void success(RbPageCombo pageCombo, Response response) {
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
    interface RbPageEndpoints {
        /**
         * Gets the lead section and initial metadata of a given title.
         *
         * @param title the page title with prefix if necessary
         * @param noImages add the noimages flag to the request if true
         * @param cb a Retrofit callback which provides the populated RbPageLead object in #success
         */
        @GET("/page/mobile-html-sections-lead/{title}")
        void pageLead(@Path("title") String title, @Query("noimages") Boolean noImages,
                      Callback<RbPageLead> cb);

        /**
         * Gets the remaining sections of a given title.
         *
         * @param title the page title to be used including prefix
         * @param noImages add the noimages flag to the request if true
         * @param cb a Retrofit callback which provides the populated RbPageRemaining object in #success
         */
        @GET("/page/mobile-html-sections-remaining/{title}")
        void pageRemaining(@Path("title") String title, @Query("noimages") Boolean noImages,
                           Callback<RbPageRemaining> cb);

        /**
         * Gets all page content of a given title -- for refreshing a saved page
         * Note: the only difference in the URL from #pageLead is the sections=all instead of 0.
         *
         * @param title the page title to be used including prefix
         * @param noImages add the noimages flag to the request if true
         * @param cb a Retrofit callback which provides the populated RbPageCombo object in #success
         */
        @GET("/page/mobile-html-sections/{title}")
        void pageCombo(@Path("title") String title, @Query("noimages") Boolean noImages,
                       Callback<RbPageCombo> cb);
    }
}
