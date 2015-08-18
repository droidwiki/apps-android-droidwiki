package de.droidwiki.search;

import de.droidwiki.data.ContentPersister;
import de.droidwiki.data.SQLiteContentProvider;
import android.content.Context;

public class RecentSearchPersister extends ContentPersister<RecentSearch> {
    public RecentSearchPersister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        SQLiteContentProvider.getAuthorityForTable(
                                RecentSearch.PERSISTENCE_HELPER.getTableName()
                        )
                ),
                RecentSearch.PERSISTENCE_HELPER
        );
    }
}
