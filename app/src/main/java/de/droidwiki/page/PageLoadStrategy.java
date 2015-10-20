package de.droidwiki.page;

import de.droidwiki.bridge.CommunicationBridge;
import de.droidwiki.editing.EditHandler;
import de.droidwiki.page.leadimages.LeadImagesHandler;
import de.droidwiki.search.SearchBarHideHandler;
import de.droidwiki.views.ObservableWebView;
import de.droidwiki.views.SwipeRefreshLayoutWithScroll;

import android.content.Intent;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * Defines interaction between PageFragment and an implementation that loads a page
 * for viewing.
 */
public interface PageLoadStrategy {
    void setup(PageViewModel model, PageFragment fragment,
               SwipeRefreshLayoutWithScroll refreshView, ObservableWebView webView,
               CommunicationBridge bridge, SearchBarHideHandler searchBarHideHandler,
               LeadImagesHandler leadImagesHandler);

    void onActivityCreated(@NonNull List<PageBackStackItem> backStack);

    void backFromEditing(Intent data);

    void onDisplayNewPage(boolean pushBackStack, boolean tryFromCache, int stagedScrollY);

    boolean isLoading();

    void onHidePageContent();

    boolean onBackPressed();

    void setEditHandler(EditHandler editHandler);

    void setBackStack(@NonNull List<PageBackStackItem> backStack);

    void updateCurrentBackStackItem();

    void loadPageFromBackStack();
}