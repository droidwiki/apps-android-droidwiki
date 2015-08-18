package de.droidwiki.interlanguage;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import de.droidwiki.Utils;
import de.droidwiki.ViewAnimations;
import de.droidwiki.activity.ActivityUtil;
import de.droidwiki.activity.ThemedActionBarActivity;
import de.droidwiki.history.HistoryEntry;
import de.droidwiki.page.PageActivity;
import de.droidwiki.page.PageTitle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import de.droidwiki.Site;
import de.droidwiki.WikipediaApp;
import de.droidwiki.util.StringUtil;

import static de.droidwiki.util.StringUtil.emptyIfNull;

public class LangLinksActivity extends ThemedActionBarActivity {
    public static final int ACTIVITY_RESULT_LANGLINK_SELECT = 1;

    public static final String ACTION_LANGLINKS_FOR_TITLE = "de.droidwiki.langlinks_for_title";
    public static final String EXTRA_PAGETITLE = "de.droidwiki.pagetitle";

    private static final String LANGUAGE_ENTRIES_BUNDLE_KEY = "languageEntries";

    private static final String GOTHIC_LANGUAGE_CODE = "got";

    private ArrayList<PageTitle> languageEntries;
    private PageTitle title;

    private WikipediaApp app;

    private ListView langLinksList;
    private View langLinksProgress;
    private View langLinksContainer;
    private View langLinksEmpty;
    private View langLinksNoMatch;
    private View langLinksError;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();

        setContentView(de.droidwiki.R.layout.activity_langlinks);

        if (!getIntent().getAction().equals(ACTION_LANGLINKS_FOR_TITLE)) {
            throw new RuntimeException("Only ACTION_LANGLINKS_FOR_TITLE is supported");
        }

        langLinksList = (ListView) findViewById(de.droidwiki.R.id.langlinks_list);
        langLinksProgress = findViewById(de.droidwiki.R.id.langlinks_load_progress);
        langLinksContainer = findViewById(de.droidwiki.R.id.langlinks_list_container);
        langLinksEmpty = findViewById(de.droidwiki.R.id.langlinks_empty);
        langLinksNoMatch = findViewById(de.droidwiki.R.id.langlinks_no_match);
        langLinksError = findViewById(de.droidwiki.R.id.langlinks_error);
        EditText langLinksFilter = (EditText) findViewById(de.droidwiki.R.id.langlinks_filter);
        Button langLinksErrorRetry = (Button) findViewById(de.droidwiki.R.id.langlinks_error_retry);

        title = getIntent().getParcelableExtra(EXTRA_PAGETITLE);

        if (savedInstanceState != null && savedInstanceState.containsKey(LANGUAGE_ENTRIES_BUNDLE_KEY)) {
            languageEntries = savedInstanceState.getParcelableArrayList(LANGUAGE_ENTRIES_BUNDLE_KEY);
        }

        fetchLangLinks();

        langLinksErrorRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewAnimations.crossFade(langLinksError, langLinksProgress);
                fetchLangLinks();
            }
        });

        langLinksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle langLink = (PageTitle) parent.getAdapter().getItem(position);
                app.setMruLanguageCode(langLink.getSite().getLanguageCode());
                HistoryEntry historyEntry = new HistoryEntry(langLink, HistoryEntry.SOURCE_LANGUAGE_LINK);
                Intent intent = new Intent()
                        .setClass(LangLinksActivity.this, PageActivity.class)
                        .setAction(PageActivity.ACTION_PAGE_FOR_TITLE)
                        .putExtra(PageActivity.EXTRA_PAGETITLE, langLink)
                        .putExtra(PageActivity.EXTRA_HISTORYENTRY, historyEntry);
                setResult(ACTIVITY_RESULT_LANGLINK_SELECT, intent);
                Utils.hideSoftKeyboard(LangLinksActivity.this);
                finish();
            }
        });

        langLinksFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // the languages might not be loaded yet...
                if (langLinksList.getAdapter() == null) {
                    return;
                }
                ((LangLinksAdapter) langLinksList.getAdapter()).setFilterText(s.toString());

                //Check if there are no languages that match the filter
                if (langLinksList.getAdapter().getCount() == 0) {
                    langLinksNoMatch.setVisibility(View.VISIBLE);
                } else {
                    langLinksNoMatch.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return ActivityUtil.defaultOnOptionsItemSelected(this, item)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Utils.hideSoftKeyboard(this);
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (languageEntries != null) {
           outState.putParcelableArrayList(LANGUAGE_ENTRIES_BUNDLE_KEY, languageEntries);
        }
    }

    private void displayLangLinks() {
        if (languageEntries.size() == 0) {
            ViewAnimations.crossFade(langLinksProgress, langLinksEmpty);
        } else {
            langLinksList.setAdapter(new LangLinksAdapter(languageEntries, app));
            ViewAnimations.crossFade(langLinksProgress, langLinksContainer);
        }
    }


    private void fetchLangLinks() {
        if (languageEntries == null) {
            new LangLinksFetchTask(this, title) {
                @Override
                public void onFinish(ArrayList<PageTitle> result) {
                    languageEntries = result;

                    updateLanguageEntriesSupported(languageEntries);
                    sortLanguageEntriesByMru(languageEntries);

                    displayLangLinks();
                }

                @Override
                public void onCatch(Throwable caught) {
                    ViewAnimations.crossFade(langLinksProgress, langLinksError);
                    // Not sure why this is required, but without it tapping retry hides langLinksError
                    // FIXME: INVESTIGATE WHY THIS HAPPENS!
                    // Also happens in {@link PageFragment}
                    langLinksError.setVisibility(View.VISIBLE);
                }

                private void updateLanguageEntriesSupported(List<PageTitle> languageEntries) {
                    for (ListIterator<PageTitle> it = languageEntries.listIterator(); it.hasNext();) {
                        PageTitle link = it.next();
                        String languageCode = link.getSite().getLanguageCode();

                        if (GOTHIC_LANGUAGE_CODE.equals(languageCode)) {
                            // Remove Gothic since it causes Android to segfault.
                            it.remove();
                        } else if (Locale.CHINESE.getLanguage().equals(languageCode)) {
                            // Replace Chinese with Simplified and Traditional dialects.
                            it.remove();
                            for (String dialect : Arrays.asList(AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE,
                                    AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE)) {
                                it.add(new PageTitle(link.getText(), Site.forLanguage(dialect)));
                            }
                        }
                    }
                }

                private void sortLanguageEntriesByMru(List<PageTitle> entries) {
                    int addIndex = 0;
                    for (String language : app.getMruLanguageCodes()) {
                        for (int i = 0; i < entries.size(); i++) {
                            if (entries.get(i).getSite().getLanguageCode().equals(language)) {
                                PageTitle entry = entries.remove(i);
                                entries.add(addIndex++, entry);
                                break;
                            }
                        }
                    }
                }
            }.execute();
        } else {
            displayLangLinks();
        }
    }

    private static final class LangLinksAdapter extends BaseAdapter {
        private final List<PageTitle> originalLanguageEntries;
        private final List<PageTitle> languageEntries;
        private final WikipediaApp app;

        private LangLinksAdapter(List<PageTitle> languageEntries, WikipediaApp app) {
            this.originalLanguageEntries = languageEntries;
            this.languageEntries = new ArrayList<>(originalLanguageEntries);
            this.app = app;
        }

        public void setFilterText(String filter) {
            languageEntries.clear();
            filter = filter.toLowerCase();
            for (PageTitle entry : originalLanguageEntries) {
                String languageCode = entry.getSite().getLanguageCode();
                String canonicalName = StringUtil.emptyIfNull(app.getAppLanguageCanonicalName(languageCode));
                String localizedName = StringUtil.emptyIfNull(app.getAppLanguageLocalizedName(languageCode));
                if (canonicalName.toLowerCase().contains(filter)
                        || localizedName.toLowerCase().contains(filter)) {
                    languageEntries.add(entry);
                }
            }
            notifyDataSetInvalidated();
        }

        @Override
        public int getCount() {
            return languageEntries.size();
        }

        @Override
        public PageTitle getItem(int position) {
            return languageEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(de.droidwiki.R.layout.item_language_list_entry, parent, false);
            }

            PageTitle item = getItem(position);
            String languageCode = item.getSite().getLanguageCode();
            String localizedLanguageName = app.getAppLanguageLocalizedName(languageCode);
            if (localizedLanguageName == null && languageCode.equals(Locale.CHINA.getLanguage())) {
                localizedLanguageName = Locale.CHINA.getDisplayName(Locale.CHINA);
            }

            TextView localizedLanguageNameTextView = (TextView) convertView.findViewById(de.droidwiki.R.id.localized_language_name);
            TextView articleTitleTextView = (TextView) convertView.findViewById(de.droidwiki.R.id.article_title);

            localizedLanguageNameTextView.setText(localizedLanguageName);
            articleTitleTextView.setText(item.getText());

            return convertView;
        }
    }
}
