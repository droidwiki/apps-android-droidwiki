package de.droidwiki.history;

import android.content.Context;
import de.droidwiki.data.ContentPersister;
import de.droidwiki.data.SQLiteContentProvider;

public class HistoryEntryPersister extends ContentPersister<HistoryEntry> {
    public HistoryEntryPersister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        SQLiteContentProvider.getAuthorityForTable(
                                HistoryEntry.PERSISTENCE_HELPER.getTableName()
                        )
                ),
                HistoryEntry.PERSISTENCE_HELPER
        );
    }
}
