package de.droidwiki.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import de.droidwiki.Site;
import de.droidwiki.pagehistory.usercontributions.FetchUserContribsTask;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FetchUserContribsTaskTest extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;
    private static final int NUM_FETCH = 10;

    public FetchUserContribsTaskTest() {
        super(TestDummyActivity.class);
    }

    public void testUserContributionsFetch() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        startActivity(new Intent(), null, null);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new FetchUserContribsTask(getInstrumentation().getTargetContext(),  new Site("test.wikipedia.org"), "yuvipanda", NUM_FETCH, null) {
                    @Override
                    public void onFinish(FetchUserContribsTask.UserContributionsList result) {
                        assertNotNull(result);
                        assertNotNull(result.getQueryContinue());
                        assertFalse(result.getContribs().size() < NUM_FETCH);
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
