package de.droidwiki.editing.summaries;

import android.content.Context;
import de.droidwiki.data.ContentPersister;

public class EditSummaryPersister extends ContentPersister<EditSummary> {
    public EditSummaryPersister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        EditSummary.PERSISTENCE_HELPER.getBaseContentURI()
                ),
                EditSummary.PERSISTENCE_HELPER
        );
    }
}
