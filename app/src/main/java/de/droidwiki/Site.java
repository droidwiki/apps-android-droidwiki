package de.droidwiki;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import de.droidwiki.interlanguage.AppLanguageLookUpTable;
import de.droidwiki.page.PageTitle;

import java.util.Locale;

/**
 * Represents a particular wiki.
 */
public class Site implements Parcelable {
    private final String domain;

    private final String languageCode;

    public Site(String domain) {
        this(domain, urlToLanguage(domain));
    }

    public Site(String domain, String languageCode) {
        this.domain = urlToDesktopSite(domain);
        this.languageCode = languageCode;
    }

    public Site(Parcel in) {
        this(in.readString(), in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(domain);
        dest.writeString(languageCode);
    }

    public String getScriptPath(String script) {
        return "/" + script;
    }

    public String getApiDomain() {
        return WikipediaApp.getInstance().getSslFallback() ? domain : urlToMobileSite(domain);
    }

    public boolean getUseSecure() {
        return true;
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Site> CREATOR
            = new Parcelable.Creator<Site>() {
        public Site createFromParcel(Parcel in) {
            return new Site(in);
        }

        public Site[] newArray(int size) {
            return new Site[size];
        }
    };

    // Auto-generated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Site site = (Site) o;

        if (domain != null ? !domain.equals(site.domain) : site.domain != null) {
            return false;
        }
        return !(languageCode != null ? !languageCode.equals(site.languageCode) : site.languageCode != null);

    }

    // Auto-generated
    @Override
    public int hashCode() {
        int result = domain != null ? domain.hashCode() : 0;
        result = 31 * result + (languageCode != null ? languageCode.hashCode() : 0);
        return result;
    }

    // Auto-generated
    @Override
    public String toString() {
        return "Site{"
                + "domain='" + domain + '\''
                + ", languageCode='" + languageCode + '\''
                + '}';
    }

    public String getFullUrl(String script) {
        return WikipediaApp.getInstance().getNetworkProtocol() + "://" + getDomain() + getScriptPath(script);
    }

    /**
     * Create a PageTitle object from an internal link string.
     *
     * @param internalLink Internal link target text (eg. /wiki/Target).
     *                     Should be URL decoded before passing in
     * @return A {@link PageTitle} object representing the internalLink passed in.
     */
    public PageTitle titleForInternalLink(String internalLink) {
        // FIXME: Handle language variant links properly
        // Strip the /wiki/ from the href
        return new PageTitle(internalLink.replaceFirst("/", ""), this);
    }

    /**
     * Create a PageTitle object from a Uri, taking into account any fragment (section title) in the link.
     * @param uri Uri object to be turned into a PageTitle.
     * @return {@link PageTitle} object that corresponds to the given Uri.
     */
    public PageTitle titleForUri(Uri uri) {
        String path = uri.getPath();
        if (!TextUtils.isEmpty(uri.getFragment())) {
            path += "#" + uri.getFragment();
        }
        return titleForInternalLink(path);
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public static Site forLanguage(String language) {
        return new Site("www.droidwiki.de", language);
    }

    /**
     * Returns if the site is supported
     * @param domain the site domain
     * @return boolean
     */
    public static boolean isSupportedSite(String domain) {
        return domain.matches("[a-z\\-]+\\.?droidwiki\\.de");
    }

    private static String urlToLanguage(String url) {
        return "de";
    }

    private String urlToDesktopSite(String url) {
        return url.replaceFirst("\\.m\\.", ".");
    }

    private String urlToMobileSite(String url) {
        return url;
    }

    private static String languageToWikiSubdomain(String language) {
        switch (language) {
            case AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE:
            case AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE:
                return Locale.CHINA.getLanguage();
            default:
                return language;
        }
    }
}
