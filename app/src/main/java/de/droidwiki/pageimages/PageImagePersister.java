package de.droidwiki.pageimages;

import android.content.Context;
import de.droidwiki.data.ContentPersister;

public class PageImagePersister extends ContentPersister<PageImage> {
    public PageImagePersister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        PageImage.PERSISTENCE_HELPER.getBaseContentURI()
                ),
                PageImage.PERSISTENCE_HELPER
        );
    }
}
