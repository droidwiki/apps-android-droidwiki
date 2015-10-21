package de.droidwiki.server;

import de.droidwiki.page.Page;
import de.droidwiki.page.PageTitle;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Gson POJI for loading the first stage of page content.
 */
public interface PageLead {

    boolean hasError();

    ServiceError getError();

    void logError(String message);

    /** Note: before using this check that #hasError is false */
    Page toPage(PageTitle title);

    String getLeadSectionContent();

    /** So we can have polymorphic Retrofit Callbacks */
    interface Callback {
        void success(PageLead pageLead, Response response);

        void failure(RetrofitError error);
    }
}
