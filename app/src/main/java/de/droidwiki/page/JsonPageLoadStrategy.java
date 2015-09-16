package de.droidwiki.page;

import org.acra.ACRA;
import de.droidwiki.R;
import de.droidwiki.Utils;
import de.droidwiki.WikipediaApp;
import de.droidwiki.bridge.CommunicationBridge;
import de.droidwiki.editing.EditHandler;
import de.droidwiki.editing.EditSectionActivity;
import de.droidwiki.history.HistoryEntry;
import de.droidwiki.history.SaveHistoryTask;
import de.droidwiki.page.bottomcontent.BottomContentHandler;
import de.droidwiki.page.bottomcontent.BottomContentInterface;
import de.droidwiki.page.leadimages.LeadImagesHandler;
import de.droidwiki.server.PageService;
import de.droidwiki.server.PageServiceFactory;
import de.droidwiki.server.PageLead;
import de.droidwiki.server.PageRemaining;
import de.droidwiki.server.ServiceError;
import de.droidwiki.pageimages.PageImage;
import de.droidwiki.pageimages.PageImagesTask;
import de.droidwiki.savedpages.LoadSavedPageTask;
import de.droidwiki.search.SearchBarHideHandler;
import de.droidwiki.util.DimenUtil;
import de.droidwiki.views.ObservableWebView;
import de.droidwiki.views.SwipeRefreshLayoutWithScroll;

import org.mediawiki.api.json.ApiException;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.RetrofitError;
import retrofit.client.Response;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Our old page load strategy, which uses the JSON MW API directly and loads a page in multiple steps:
 * First it loads the lead section (sections=0).
 * Then it loads the remaining sections (sections=1-).
 * <p/>
 * This class tracks:
 * - the states the page loading goes through,
 * - a backstack of pages and page positions visited,
 * - and many handlers.
 */
public class JsonPageLoadStrategy implements PageLoadStrategy {
    private static final String TAG = "JsonPageLoad";
    private static final String BRIDGE_PAYLOAD_SAVED_PAGE = "savedPage";

    public static final int STATE_NO_FETCH = 1;
    public static final int STATE_INITIAL_FETCH = 2;
    public static final int STATE_COMPLETE_FETCH = 3;

    private int state = STATE_NO_FETCH;

    /**
     * List of lightweight history items to serve as the backstack for this fragment.
     * Since the list consists of Parcelable objects, it can be saved and restored from the
     * savedInstanceState of the fragment.
     */
    @NonNull private List<PageBackStackItem> backStack;

    /**
     * Sequence number to maintain synchronization when loading page content asynchronously
     * between the Java and Javascript layers, as well as between async tasks and the UI thread.
     */
    private int currentSequenceNum;

    /**
     * The y-offset position to which the page will be scrolled once it's fully loaded
     * (or loaded to the point where it can be scrolled to the correct position).
     */
    private int stagedScrollY;
    private int sectionTargetFromIntent;
    private String sectionTargetFromTitle;

    /**
     * Whether to write the page contents to cache as soon as it's loaded.
     */
    private boolean cacheOnComplete = true;

    // copied fields
    private PageViewModel model;
    private PageFragment fragment;
    private CommunicationBridge bridge;
    private PageActivity activity;
    private ObservableWebView webView;
    private SwipeRefreshLayoutWithScroll refreshView;
    private WikipediaApp app;
    private LeadImagesHandler leadImagesHandler;
    private SearchBarHideHandler searchBarHideHandler;
    private EditHandler editHandler;

    private BottomContentInterface bottomContentHandler;

    JsonPageLoadStrategy() {
        backStack = new ArrayList<>();
    }

    @Override
    public void setBackStack(@NonNull List<PageBackStackItem> backStack) {
        this.backStack = backStack;
    }

    @Override
    public void setup(PageViewModel model, PageFragment fragment,
                      SwipeRefreshLayoutWithScroll refreshView,
                      ObservableWebView webView, CommunicationBridge bridge,
                      SearchBarHideHandler searchBarHideHandler, LeadImagesHandler leadImagesHandler) {
        this.model = model;
        this.fragment = fragment;
        activity = (PageActivity) fragment.getActivity();
        this.app = (WikipediaApp) activity.getApplicationContext();
        this.refreshView = refreshView;
        this.webView = webView;
        this.bridge = bridge;
        this.searchBarHideHandler = searchBarHideHandler;
        this.leadImagesHandler = leadImagesHandler;
    }

    @Override
    public void onActivityCreated(@NonNull List<PageBackStackItem> backStack) {
        setupSpecificMessageHandlers();

        currentSequenceNum = 0;

        this.backStack = backStack;

        // if we already have pages in the backstack (whether it's from savedInstanceState, or
        // from being stored in the activity's fragment backstack), then load the topmost page
        // on the backstack.
        loadPageFromBackStack();
    }

    private void setupSpecificMessageHandlers() {
        bridge.addListener("onBeginNewPage", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                if (!fragment.isAdded()) {
                    return;
                }
                try {
                    if (messagePayload.getInt("sequence") != currentSequenceNum) {
                        return;
                    }
                    stagedScrollY = messagePayload.getInt("stagedScrollY");
                    loadPageOnWebViewReady(messagePayload.getBoolean("tryFromCache"));
                } catch (JSONException e) {
                    ACRA.getErrorReporter().handleException(e);
                }
            }
        });
        bridge.addListener("requestSection", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                if (!fragment.isAdded()) {
                    return;
                }
                try {
                    if (messagePayload.getInt("sequence") != currentSequenceNum) {
                        return;
                    }
                    displayNonLeadSection(messagePayload.getInt("index"),
                            messagePayload.optBoolean(BRIDGE_PAYLOAD_SAVED_PAGE, false));
                } catch (JSONException e) {
                    ACRA.getErrorReporter().handleException(e);
                }
            }
        });
        bridge.addListener("pageLoadComplete", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                if (!fragment.isAdded()) {
                    return;
                }
                try {
                    if (messagePayload.getInt("sequence") != currentSequenceNum) {
                        return;
                    }
                } catch (JSONException e) {
                    ACRA.getErrorReporter().handleException(e);
                }
                // Do any other stuff that should happen upon page load completion...
                activity.updateProgressBar(false, true, 0);

                // trigger layout of the bottom content
                // Check to see if the page title has changed (e.g. due to following a redirect),
                // because if it has then the handler needs the new title to make sure it doesn't
                // accidentally display the current article as a "read more" suggestion
                if (!bottomContentHandler.getTitle().equals(model.getTitle())) {
                    bottomContentHandler.setTitle(model.getTitle());
                }
                bottomContentHandler.beginLayout();
            }
        });

        bottomContentHandler = new BottomContentHandler(fragment, bridge, webView,
                fragment.getLinkHandler(),
                (ViewGroup) fragment.getView().findViewById(de.droidwiki.R.id.bottom_content_container));
    }

    @Override
    public void backFromEditing(Intent data) {
        //Retrieve section ID from intent, and find correct section, so where know where to scroll to
        sectionTargetFromIntent = data.getIntExtra(EditSectionActivity.EXTRA_SECTION_ID, 0);
        //reset our scroll offset, since we have a section scroll target
        stagedScrollY = 0;
    }

    @Override
    public void onDisplayNewPage(boolean pushBackStack, boolean tryFromCache, int stagedScrollY) {
        if (pushBackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem();
            pushBackStack();
        }

        state = STATE_NO_FETCH;

        // increment our sequence number, so that any async tasks that depend on the sequence
        // will invalidate themselves upon completion.
        currentSequenceNum++;

        // kick off an event to the WebView that will cause it to clear its contents,
        // and then report back to us when the clearing is complete, so that we can synchronize
        // the transitions of our native components to the new page content.
        // The callback event from the WebView will then call the loadPageOnWebViewReady()
        // function, which will continue the loading process.
        try {
            JSONObject wrapper = new JSONObject();
            // whatever we pass to this event will be passed back to us by the WebView!
            wrapper.put("sequence", currentSequenceNum);
            wrapper.put("tryFromCache", tryFromCache);
            wrapper.put("stagedScrollY", stagedScrollY);
            bridge.sendMessage("beginNewPage", wrapper);
        } catch (JSONException e) {
            ACRA.getErrorReporter().handleException(e);
        }
    }

    private void performActionForState(int forState) {
        if (!fragment.isAdded()) {
            return;
        }
        switch (forState) {
            case STATE_NO_FETCH:
                activity.updateProgressBar(true, true, 0);
                // hide the lead image...
                leadImagesHandler.hide();
                bottomContentHandler.hide();
                activity.getSearchBarHideHandler().setFadeEnabled(false);
                loadLeadSection(currentSequenceNum);
                break;
            case STATE_INITIAL_FETCH:
                loadRemainingSections(currentSequenceNum);
                break;
            case STATE_COMPLETE_FETCH:
                editHandler.setPage(model.getPage());
                // kick off the lead image layout
                leadImagesHandler.beginLayout(new LeadImagesHandler.OnLeadImageLayoutListener() {
                    @Override
                    public void onLayoutComplete(int sequence) {
                        if (!fragment.isAdded() || sequence != currentSequenceNum) {
                            return;
                        }
                        searchBarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());
                        // when the lead image layout is complete, load the lead section and
                        // the other sections into the webview.
                        displayLeadSection();
                        displayNonLeadSectionForUnsavedPage(1);
                    }
                }, currentSequenceNum);
                break;
            default:
                // This should never happen
                throw new RuntimeException("Unknown state encountered " + state);
        }
    }

    private void setState(int state) {
        if (!fragment.isAdded()) {
            return;
        }
        this.state = state;
        activity.supportInvalidateOptionsMenu();

        // FIXME: Move this out into a PageComplete event of sorts
        if (state == STATE_COMPLETE_FETCH) {
            fragment.setupToC(model, isFirstPage());

            //add the page to cache!
            if (cacheOnComplete) {
                app.getPageCache().put(model.getTitleOriginal(), model.getPage(),
                        new PageCache.CachePutListener() {
                            @Override
                            public void onPutComplete() {
                            }

                            @Override
                            public void onPutError(Throwable e) {
                                Log.e(TAG, "Failed to add page to cache.", e);
                            }
                        });
            }
        }
    }

    @Override
    public boolean isLoading() {
        return state != STATE_COMPLETE_FETCH;
    }

    private void loadPageOnWebViewReady(boolean tryFromCache) {
        // stage any section-specific link target from the title, since the title may be
        // replaced (normalized)
        sectionTargetFromTitle = model.getTitle().getFragment();

        Utils.setupDirectionality(model.getTitle().getSite().getLanguageCode(), Locale.getDefault().getLanguage(),
                bridge);

        // hide the native top and bottom components...
        leadImagesHandler.hide();
        bottomContentHandler.hide();
        bottomContentHandler.setTitle(model.getTitle());

        // Before attempting to load saved page upon return from SavedPagesFragment, check to ensure it wasn't just deleted!
        // TODO: Fix possible race condition when navigating from history fragment
        if (model.getCurEntry().getSource() == HistoryEntry.SOURCE_SAVED_PAGE && fragment.isPageSaved()) {
            state = STATE_NO_FETCH;
            loadSavedPage();
        } else if (tryFromCache) {
            loadPageFromCache();
        } else {
            loadPageFromNetwork();
        }
    }

    private void loadPageFromCache() {
        app.getPageCache()
                .get(model.getTitleOriginal(), currentSequenceNum, new PageCache.CacheGetListener() {
                    @Override
                    public void onGetComplete(Page page, int sequence) {
                        if (sequence != currentSequenceNum) {
                            return;
                        }
                        if (page != null) {
                            Log.d(TAG, "Using page from cache: "
                                    + model.getTitleOriginal().getDisplayText());
                            model.setPage(page);
                            model.setTitle(page.getTitle());
                            // Update our history entry, in case the Title was changed (i.e. normalized)
                            final HistoryEntry curEntry = model.getCurEntry();
                            model.setCurEntry(
                                    new HistoryEntry(model.getTitle(), curEntry.getSource()));
                            // load the current title's thumbnail from sqlite
                            updateThumbnail(PageImage.PERSISTENCE_HELPER.getImageUrlForTitle(app, model.getTitle()));
                            // Save history entry...
                            new SaveHistoryTask(model.getCurEntry(), app).execute();
                            // don't re-cache the page after loading.
                            cacheOnComplete = false;
                            state = STATE_COMPLETE_FETCH;
                            setState(state);
                            performActionForState(state);
                            if (fragment.isAdded()) {
                                fragment.onPageLoadComplete();
                            }
                        } else {
                            // page isn't in cache, so fetch it from the network...
                            loadPageFromNetwork();
                        }
                    }

                    @Override
                    public void onGetError(Throwable e, int sequence) {
                        Log.e(TAG, "Failed to get page from cache.", e);
                        if (sequence != currentSequenceNum) {
                            return;
                        }
                        // something failed when loading it from cache, so fetch it from network...
                        loadPageFromNetwork();
                    }
                });
    }

    private void loadPageFromNetwork() {
        state = STATE_NO_FETCH;
        // and make sure to write it to cache when it's loaded.
        cacheOnComplete = true;
        setState(state);
        performActionForState(state);
    }

    public void loadSavedPage() {
        new LoadSavedPageTask(model.getTitle()) {
            @Override
            public void onFinish(Page result) {
                // have we been unwittingly detached from our Activity?
                if (!fragment.isAdded()) {
                    Log.d("PageFragment", "Detached from activity, so stopping update.");
                    return;
                }

                // Save history entry and page image url
                new SaveHistoryTask(model.getCurEntry(), app).execute();

                model.setPage(result);
                editHandler.setPage(model.getPage());
                // kick off the lead image layout
                leadImagesHandler.beginLayout(new LeadImagesHandler.OnLeadImageLayoutListener() {
                    @Override
                    public void onLayoutComplete(int sequence) {
                        if (!fragment.isAdded() || sequence != currentSequenceNum) {
                            return;
                        }
                        searchBarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());
                        // when the lead image is laid out, load the lead section and the rest
                        // of the sections into the webview.
                        displayLeadSection();
                        displayNonLeadSectionForSavedPage(1);

                        setState(STATE_COMPLETE_FETCH);
                    }
                }, currentSequenceNum);
            }

            @Override
            public void onCatch(Throwable caught) {

                /*
                If anything bad happens during loading of a saved page, then simply bounce it
                back to the online version of the page, and re-save the page contents locally when it's done.
                 */

                Log.d("LoadSavedPageTask", "Error loading saved page: " + caught.getMessage());
                caught.printStackTrace();

                fragment.refreshPage(true);
            }
        }.execute();
    }

    private void updateThumbnail(String thumbUrl) {
        model.getTitle().setThumbUrl(thumbUrl);
        model.getTitleOriginal().setThumbUrl(thumbUrl);
        fragment.invalidateTabs();
    }

    private boolean isFirstPage() {
        return backStack.size() <= 1 && !webView.canGoBack();
    }

    /**
     * Pop the topmost entry from the backstack.
     * Does NOT automatically load the next topmost page on the backstack.
     */
    private void popBackStack() {
        if (backStack.isEmpty()) {
            return;
        }
        backStack.remove(backStack.size() - 1);
    }

    /**
     * Push the current page title onto the backstack.
     */
    private void pushBackStack() {
        PageBackStackItem item = new PageBackStackItem(model.getTitleOriginal(), model.getCurEntry());
        backStack.add(item);
    }

    /**
     * Update the current topmost backstack item, based on the currently displayed page.
     * (Things like the last y-offset position should be updated here)
     * Should be done right before loading a new page.
     */
    @Override
    public void updateCurrentBackStackItem() {
        if (backStack.isEmpty()) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        item.setScrollY(webView.getScrollY());
    }

    @Override
    public void loadPageFromBackStack() {
        if (backStack.isEmpty()) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        // display the page based on the backstack item, stage the scrollY position based on
        // the backstack item.
        fragment.displayNewPage(item.getTitle(), item.getHistoryEntry(), true, false,
                item.getScrollY());
        Log.d(TAG, "Loaded page " + item.getTitle().getDisplayText() + " from backstack");
    }

    private void displayLeadSection() {
        try {
            final Page page = model.getPage();
            final PageProperties pageProperties = page.getPageProperties();

            JSONObject marginPayload = new JSONObject();
            int margin = DimenUtil.roundedPxToDp(activity.getResources().getDimension(de.droidwiki.R.dimen.content_margin));
            marginPayload.put("marginLeft", margin);
            marginPayload.put("marginRight", margin);
            bridge.sendMessage("setMargins", marginPayload);

            JSONObject leadSectionPayload = new JSONObject();
            leadSectionPayload.put("sequence", currentSequenceNum);
            leadSectionPayload.put("title", page.getDisplayTitle());
            leadSectionPayload.put("section", page.getSections().get(0).toJSON());
            leadSectionPayload.put("string_page_similar_titles", activity.getString(de.droidwiki.R.string.page_similar_titles));
            leadSectionPayload.put("string_page_issues", activity.getString(de.droidwiki.R.string.button_page_issues));
            leadSectionPayload.put("string_table_infobox", activity.getString(de.droidwiki.R.string.table_infobox));
            leadSectionPayload.put("string_table_other", activity.getString(de.droidwiki.R.string.table_other));
            leadSectionPayload.put("string_table_close", activity.getString(de.droidwiki.R.string.table_close));
            leadSectionPayload.put("string_expand_refs", activity.getString(de.droidwiki.R.string.expand_refs));
            leadSectionPayload.put("isBeta", app.getReleaseType() != WikipediaApp.RELEASE_PROD);
            leadSectionPayload.put("siteLanguage", model.getTitle().getSite().getLanguageCode());
            leadSectionPayload.put("isMainPage", page.isMainPage());
            leadSectionPayload.put("apiLevel", Build.VERSION.SDK_INT);
            bridge.sendMessage("displayLeadSection", leadSectionPayload);
            Log.d(TAG, "Sent message 'displayLeadSection' for page: " + page.getDisplayTitle());

            // Hide edit pencils if anon editing is disabled by remote killswitch or if this is a file page
            JSONObject miscPayload = new JSONObject();
            boolean isAnonEditingDisabled = app.getRemoteConfig().getConfig()
                    .optBoolean("disableAnonEditing", false)
                    && !app.getUserInfoStorage().isLoggedIn();
            miscPayload.put("noedit", (isAnonEditingDisabled
                    || page.isFilePage()
                    || page.isMainPage()));
            miscPayload.put("protect", !pageProperties.canEdit());
            bridge.sendMessage("setPageProtected", miscPayload);
        } catch (JSONException e) {
            // This should never happen
            throw new RuntimeException(e);
        }

        if (webView.getVisibility() != View.VISIBLE) {
            webView.setVisibility(View.VISIBLE);
        }

        refreshView.setRefreshing(false);
        activity.updateProgressBar(true, true, 0);
    }

    private void displayNonLeadSectionForUnsavedPage(int index) {
        displayNonLeadSection(index, false);
    }

    private void displayNonLeadSectionForSavedPage(int index) {
        displayNonLeadSection(index, true);
    }

    private void displayNonLeadSection(int index, boolean savedPage) {
        activity.updateProgressBar(true, false,
                PageActivity.PROGRESS_BAR_MAX_VALUE / model.getPage()
                        .getSections().size() * index);

        try {
            final Page page = model.getPage();
            JSONObject wrapper = new JSONObject();
            wrapper.put("sequence", currentSequenceNum);
            wrapper.put(BRIDGE_PAYLOAD_SAVED_PAGE, savedPage);
            boolean lastSection = index == page.getSections().size();
            if (!lastSection) {
                JSONObject section = page.getSections().get(index).toJSON();
                wrapper.put("section", section);
                wrapper.put("index", index);
                if (sectionTargetFromIntent > 0 && sectionTargetFromIntent < page.getSections().size()) {
                    //if we have a section to scroll to (from our Intent):
                    wrapper.put("fragment",
                            page.getSections().get(sectionTargetFromIntent).getAnchor());
                } else if (sectionTargetFromTitle != null) {
                    //if we have a section to scroll to (from our PageTitle):
                    wrapper.put("fragment", sectionTargetFromTitle);
                } else if (model.getTitle().getFragment() != null) {
                    // It's possible, that the link was a redirect and the new title has a fragment
                    // scroll to it, if there was no fragment so far
                    wrapper.put("fragment", model.getTitle().getFragment());
                }
            } else {
                wrapper.put("noMore", true);
            }
            //give it our expected scroll position, in case we need the page to be pre-scrolled upon loading.
            wrapper.put("scrollY",
                    (int) (stagedScrollY / activity.getResources().getDisplayMetrics().density));
            bridge.sendMessage("displaySection", wrapper);

            if (savedPage && lastSection) {
                // rewrite the image URLs in the webview, so that they're loaded from
                // local storage after all the sections have been loaded.
                fragment.readUrlMappings();
            }
        } catch (JSONException e) {
            ACRA.getErrorReporter().handleException(e);
        }
    }

    @VisibleForTesting
    protected void loadLeadSection(final int startSequenceNum) {
        getApiService().pageLead(model.getTitle().getPrefixedText(), calculateLeadImageWidth(),
                !app.isImageDownloadEnabled(), new PageLead.Callback() {
                    @Override
                    public void success(PageLead pageLead, Response response) {
                        Log.v(TAG, response.getUrl());
                        onLeadSectionLoaded(pageLead, startSequenceNum);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "PageLead error: " + error);
                        commonSectionFetchOnCatch(error, startSequenceNum);
                    }
                });
    }

    private void onLeadSectionLoaded(PageLead pageLead, int startSequenceNum) {
        if (!fragment.isAdded() || startSequenceNum != currentSequenceNum) {
            return;
        }
        if (pageLead.hasError()) {
            ServiceError error = pageLead.getError();
            if (error != null) {
                ApiException apiException = new ApiException(error.getTitle(), error.getDetails());
                commonSectionFetchOnCatch(apiException, startSequenceNum);
            } else {
                ApiException apiException
                        = new ApiException("unknown", "unexpected pageLead response");
                commonSectionFetchOnCatch(apiException, startSequenceNum);
            }
            return;
        }

        final PageTitle title = model.getTitle();
        Page page = pageLead.toPage(title);
        model.setPage(page);

        editHandler.setPage(model.getPage());

        // kick off the lead image layout
        leadImagesHandler.beginLayout(new LeadImagesHandler.OnLeadImageLayoutListener() {
            @Override
            public void onLayoutComplete(int sequence) {
                if (sequence != currentSequenceNum) {
                    return;
                }
                searchBarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());
                // when the lead image is laid out, display the lead section in the webview,
                // and start loading the rest of the sections.
                displayLeadSection();
                setState(STATE_INITIAL_FETCH);
                performActionForState(state);
            }
        }, currentSequenceNum);

        // Update our history entry, in case the Title was changed (i.e. normalized)
        final HistoryEntry curEntry = model.getCurEntry();
        model.setCurEntry(
                new HistoryEntry(title, curEntry.getTimestamp(), curEntry.getSource()));

        // Save history entry and page image url
        new SaveHistoryTask(model.getCurEntry(), app).execute();

        // Fetch larger thumbnail URL from the network, and save it to our DB.
        (new PageImagesTask(app.getAPIForSite(model.getTitle().getSite()), model.getTitle().getSite(),
                Arrays.asList(new PageTitle[]{model.getTitle()}), WikipediaApp.PREFERRED_THUMB_SIZE) {
            @Override
            public void onFinish(Map<PageTitle, String> result) {
                if (result.containsKey(model.getTitle())) {
                    PageImage pi = new PageImage(model.getTitle(), result.get(model.getTitle()));
                    app.getPersister(PageImage.class).upsert(pi, PageImage.PERSISTENCE_HELPER.SELECTION_KEYS);
                    updateThumbnail(result.get(model.getTitle()));
                }
            }

            @Override
            public void onCatch(Throwable caught) {
                // Thumbnails are expendable
                Log.w("SaveThumbnailTask", "Caught " + caught.getMessage(), caught);
            }
        }).execute();
    }

    private int calculateLeadImageWidth() {
        Resources res = app.getResources();
        return (int) (res.getDimension(de.droidwiki.R.dimen.leadImageWidth) / res.getDisplayMetrics().density);
    }

    private void loadRemainingSections(final int startSequenceNum) {
        getApiService().pageRemaining(model.getTitle().getPrefixedText(),
                !app.isImageDownloadEnabled(), new PageRemaining.Callback() {
                    @Override
                    public void success(PageRemaining pageRemaining, Response response) {
                        Log.v(TAG, response.getUrl());
                        onRemainingSectionsLoaded(pageRemaining, startSequenceNum);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "PageRemaining error: " + error);
                        commonSectionFetchOnCatch(error, startSequenceNum);
                    }
                });
    }

    private void onRemainingSectionsLoaded(PageRemaining pageRemaining, int startSequenceNum) {
        if (!fragment.isAdded() || startSequenceNum != currentSequenceNum) {
            return;
        }

        pageRemaining.mergeInto(model.getPage());

        displayNonLeadSectionForUnsavedPage(1);
        setState(STATE_COMPLETE_FETCH);

        fragment.onPageLoadComplete();
    }

    @VisibleForTesting
    protected void commonSectionFetchOnCatch(Throwable caught, int startSequenceNum) {
        if (startSequenceNum != currentSequenceNum) {
            return;
        }
        fragment.commonSectionFetchOnCatch(caught);
    }

    private PageService getApiService() {
        return PageServiceFactory.create(model.getTitle().getSite());
    }

    /**
     * Convenience method for hiding all the content of a page.
     */
    @Override
    public void onHidePageContent() {
        bottomContentHandler.hide();
    }

    @Override
    public boolean onBackPressed() {
        popBackStack();
        if (!backStack.isEmpty()) {
            loadPageFromBackStack();
            return true;
        }
        return false;
    }

    @Override
    public void setEditHandler(EditHandler editHandler) {
        this.editHandler = editHandler;
    }
}
