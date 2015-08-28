package de.droidwiki.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.Log;
import android.widget.RemoteViews;

import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;

import de.droidwiki.R;
import de.droidwiki.Utils;
import de.droidwiki.WikipediaApp;
import de.droidwiki.page.Page;
import de.droidwiki.page.PageActivity;
import de.droidwiki.page.PageTitle;
import de.droidwiki.page.Section;
import de.droidwiki.page.SectionsFetchTask;
import de.droidwiki.staticdata.MainPageNameData;

import java.util.List;

public class WidgetProviderFeaturedPage extends AppWidgetProvider {
    private static final String TAG = "WidgetFeatured";

    @Override
    public void onUpdate(Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ComponentName thisWidget = new ComponentName(context, WidgetProviderFeaturedPage.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (final int widgetId : allWidgetIds) {
            Log.d(TAG, "updating widget...");
            final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), de.droidwiki.R.layout.widget_featured_page);

            (new FetchMainPageTask(WikipediaApp.getInstance()) {
                @Override
                public void onFinish(List<Section> result) {
                    if (result.size() == 0) {
                        return;
                    }
                    // Extract the actual link to the featured page in a hacky way (until we
                    // have the correct API for it):
                    // Parse the HTML, and look for the first link, which should be the
                    // article of the day.
                    Spanned text = Html.fromHtml(result.get(0).getContent());
                    URLSpan[] spans = text.getSpans(0, text.length(), URLSpan.class);
                    String titleText = "";
                    for (URLSpan span : spans) {
                        if (!span.getURL().startsWith("/wiki/")
                            || (text.getSpanEnd(span) - text.getSpanStart(span) <= 1)) {
                            continue;
                        }
                        PageTitle title = WikipediaApp.getInstance().getPrimarySite()
                            .titleForInternalLink(Utils.decodeURL(span.getURL()));
                        if (!title.isFilePage() && !title.isSpecial()) {
                            titleText = title.getDisplayText();
                            break;
                        }
                    }
                    if (!TextUtils.isEmpty(titleText)) {
                        remoteViews.setTextViewText(de.droidwiki.R.id.widget_content_text, titleText);
                    }
                    appWidgetManager.updateAppWidget(widgetId, remoteViews);
                }

                @Override
                public void onCatch(Throwable caught) {
                    Log.e(TAG, "Error while updating widget", caught);
                }
            }).execute();

            // Create a PendingIntent to act as the onClickListener
            Intent intent = new Intent(context, PageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(PageActivity.EXTRA_FEATURED_ARTICLE_FROM_WIDGET, true);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // If we want to update the widget itself from the click event, then do something like this:
            //Intent intent = new Intent(context, WidgetProviderFeaturedPage.class);
            //intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            //intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            //PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(de.droidwiki.R.id.widget_container, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    private class FetchMainPageTask extends SectionsFetchTask {
        public FetchMainPageTask(WikipediaApp app) {
            super(app,
                    new PageTitle(MainPageNameData.valueFor(app.getAppOrSystemLanguageCode()),
                            app.getPrimarySite()), "all");
        }

        @Override
        public RequestBuilder buildRequest(Api api) {
            RequestBuilder builder =  super.buildRequest(api);
            builder.param("prop", builder.getParams().get("prop") + "|" + Page.API_REQUEST_PROPS);
            return builder;
        }
    }
}
