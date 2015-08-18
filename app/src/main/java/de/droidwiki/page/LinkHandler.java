package de.droidwiki.page;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import de.droidwiki.Site;
import de.droidwiki.Utils;
import de.droidwiki.WikipediaApp;
import de.droidwiki.bridge.CommunicationBridge;

/**
 * Handles any html links coming from a {@link PageFragment}
 */
public abstract class LinkHandler implements CommunicationBridge.JSEventListener, LinkMovementMethodExt.UrlHandler {
    private final Context context;

    public LinkHandler(Context context, CommunicationBridge bridge) {
        this(context);

        bridge.addListener("linkClicked", this);
    }

    public LinkHandler(Context context) {
        this.context = context;
    }

    public abstract void onPageLinkClicked(String anchor);

    public abstract void onInternalLinkClicked(PageTitle title);

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            String href = Utils.decodeURL(messagePayload.getString("href"));
            onUrlClick(href);
        } catch (IllegalArgumentException e) {
            // The URL is malformed and URL decoder can't understand it. Just do nothing.
            Log.d("Wikipedia", "A malformed URL was tapped.");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUrlClick(String href) {
        if (href.startsWith("//")) {
            // That's a protocol specific link! Make it https!
            href = "https:" + href;
        }
        Log.d("Wikipedia", "Link clicked was " + href);
        if (href.startsWith("/")) {
            Log.d("Wiikipedia", "Link recognized as internal");
            PageTitle title = getSite().titleForInternalLink(href);
            onInternalLinkClicked(title);
        } else if (href.startsWith("#")) {
            Log.d("Wikipedia", "Link recognized as fragment");
            onPageLinkClicked(href.substring(1));
        } else {
            Uri uri = Uri.parse(href);
            String authority = uri.getAuthority();
            Log.d("Wikipedia", authority);
            // FIXME: Make this more complete, only to not handle URIs that contain unsupported actions
            if (authority != null && Site.isSupportedSite(authority) && uri.getPath().startsWith("/")) {
                Site site = new Site(authority);
                PageTitle title = site.titleForUri(uri);
                onInternalLinkClicked(title);
            } else {
                // if it's a /w/ URI, turn it into a full URI and go external
                if (href.startsWith("/")) {
                    href = String.format("%1$s://%2$s", WikipediaApp.getInstance().getNetworkProtocol(), getSite().getDomain()) + href;
                }
                Utils.handleExternalLink(context, Uri.parse(href));
            }
        }
    }

    public abstract Site getSite();
}
