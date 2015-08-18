package de.droidwiki.editing.summaries;

import de.droidwiki.WikipediaApp;
import de.droidwiki.data.DBOpenHelper;
import de.droidwiki.data.SQLiteContentProvider;

public class EditSummaryContentProvider extends SQLiteContentProvider<EditSummary> {
    public EditSummaryContentProvider() {
        super(EditSummary.PERSISTENCE_HELPER);
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }
}
