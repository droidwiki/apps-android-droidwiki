package de.droidwiki.savedpages;

import android.content.Context;
import de.droidwiki.data.ContentPersister;

public class SavedPagePersister extends ContentPersister<SavedPage> {
    public SavedPagePersister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        SavedPage.PERSISTENCE_HELPER.getBaseContentURI()
                ),
                SavedPage.PERSISTENCE_HELPER
        );
    }
}
