package de.droidwiki.page.bottomcontent;

import de.droidwiki.page.PageTitle;

public interface BottomContentInterface {

    void hide();
    void beginLayout();
    PageTitle getTitle();
    void setTitle(PageTitle newTitle);

}
