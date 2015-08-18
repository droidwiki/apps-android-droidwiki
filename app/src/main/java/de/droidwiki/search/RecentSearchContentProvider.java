package de.droidwiki.search;

import de.droidwiki.WikipediaApp;
import de.droidwiki.data.DBOpenHelper;
import de.droidwiki.data.SQLiteContentProvider;

public class RecentSearchContentProvider extends SQLiteContentProvider<RecentSearch> {
    public RecentSearchContentProvider() {
        super(RecentSearch.PERSISTENCE_HELPER);
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }
}
