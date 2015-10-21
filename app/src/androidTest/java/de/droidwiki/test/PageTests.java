package de.droidwiki.test;

import junit.framework.TestCase;
import org.json.JSONObject;
import de.droidwiki.page.PageTitle;
import de.droidwiki.Site;
import de.droidwiki.page.Page;
import de.droidwiki.page.PageProperties;
import de.droidwiki.page.Section;

import java.util.ArrayList;

public class PageTests extends TestCase {

    private static final int NUM_SECTIONS = 10;

    public void testJSONSerialization() throws Exception {
        ArrayList<Section> sections = new ArrayList<>();
        Section headSection = new Section(0, 1, null, null, "Hi there!");
        sections.add(headSection);
        for (int i = 1; i <= NUM_SECTIONS; i++) {
            sections.add(new Section(i, 1, "Something " + i, "Something_" + i, "Content Something" + i));
        }
        PageTitle title = new PageTitle(null, "Test", new Site("en.wikipedia.org"));
        PageProperties props = new PageProperties(new JSONObject("{\"id\":15580374,\"displaytitle\":\"Test\",\"revision\":615503846,\"lastmodified\":\"2001-02-03T04:00:00Z\",\"editable\":true,\"mainpage\":true}"));
        Page page = new Page(title, sections, props);
        assertEquals(page, new Page(page.toJSON()));
    }
}
