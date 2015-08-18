package de.droidwiki.test;

import android.test.AndroidTestCase;
import de.droidwiki.page.Section;

public class SectionTests extends AndroidTestCase {
    public void testSectionLead() {
        // Section 0 is the lead
        Section section = new Section(0, 0, "Heading", "Heading", "Content");
        assertTrue(section.isLead());

        // Section 1 is not
        section = new Section(1, 1, "Heading", "Heading", "Content");
        assertFalse(section.isLead());

        // Section 1 is not, even if it's somehow at level 0
        section = new Section(1, 0, "Heading", "Heading", "Content");
        assertFalse(section.isLead());
    }

    public void testJSONSerialization() throws Exception {
        Section parentSection = new Section(1, 1, null, null, "Hi there!");

        assertEquals(parentSection, new Section(parentSection.toJSON()));
    }
}