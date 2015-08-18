package de.droidwiki.pageimages;

import de.droidwiki.WikipediaApp;
import de.droidwiki.data.DBOpenHelper;
import de.droidwiki.data.SQLiteContentProvider;

public class PageImageContentProvider extends SQLiteContentProvider<PageImage> {
    public PageImageContentProvider() {
        super(PageImage.PERSISTENCE_HELPER);
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }
}
