package de.droidwiki.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import de.droidwiki.WikipediaApp;
import de.droidwiki.settings.Prefs;

public class LinkPreviewFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLinkPreview";
    private static final int REV_ID = 14095177;
    private static final int PROD_LINK_PREVIEW_VERSION = 3;
    private final int version;

    private final PageTitle title;

    public LinkPreviewFunnel(WikipediaApp app, PageTitle title) {
        super(app, SCHEMA_NAME, REV_ID, app.isProdRelease() ? Funnel.SAMPLE_LOG_100 : Funnel.SAMPLE_LOG_ALL);
        this.title = title;
        version = app.isProdRelease() ? PROD_LINK_PREVIEW_VERSION : app.getLinkPreviewVersion();
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "version", version);
        return super.preprocessData(eventData);
    }

    @NonNull
    @Override
    protected String getSessionTokenField() {
        return "previewSessionToken";
    }

    public void logLinkClick() {
        log(
                "action", "linkclick"
        );
    }

    public void logNavigate() {
        log(
                "action", Prefs.isLinkPreviewEnabled() ? "navigate" : "disabled"
        );
    }

    public void logCancel() {
        log(
                "action", "cancel"
        );
    }
}
