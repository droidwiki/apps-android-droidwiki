package de.droidwiki.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;

import de.droidwiki.WikipediaApp;
import de.droidwiki.page.PageTitle;
import de.droidwiki.Site;
import de.droidwiki.page.Section;
import de.droidwiki.page.fetch.OldSectionsFetchTask;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PageFetchTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public PageFetchTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testPageFetch() throws Throwable {
        final String title = "Test_page_for_app_testing/Section1";
        final int expectedNumberOfSections = 4;
        getAllSections(expectedNumberOfSections, title);
    }

    /** Inspired by https://bugzilla.wikimedia.org/show_bug.cgi?id=66152 */
    public void testPageFetchWithAmpersand() throws Throwable {
        final String title = "Ampersand & title";
        final int expectedNumberOfSections = 1;
        getAllSections(expectedNumberOfSections, title);
    }

    private void getAllSections(final int expectedNumberOfSections, final String title) throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        startActivity(new Intent(), null, null);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WikipediaApp app = WikipediaApp.getInstance();
                new OldSectionsFetchTask(app, new PageTitle(null, title, new Site("test.wikipedia.org")), "all") {
                    @Override
                    public void onFinish(List<Section> result) {
                        assertNotNull(result);
                        assertEquals(expectedNumberOfSections, result.size());
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
