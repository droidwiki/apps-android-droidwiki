package de.droidwiki.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;

import org.mediawiki.api.json.ApiException;
import de.droidwiki.page.PageTitle;
import de.droidwiki.Site;
import de.droidwiki.WikipediaApp;
import de.droidwiki.editing.DoEditTask;
import de.droidwiki.editing.EditTokenStorage;
import de.droidwiki.editing.EditingResult;
import de.droidwiki.editing.FetchSectionWikitextTask;
import de.droidwiki.login.LoginResult;
import de.droidwiki.login.LoginTask;
import de.droidwiki.login.User;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DoEditTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;
    private static final int SECTION_ID = 3;

    public DoEditTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testEdit() throws Throwable {
        startActivity(new Intent(), null, null);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                doSave(completionLatch);
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    private void doSave(final CountDownLatch completionLatch) {
        final PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", new Site("test.wikipedia.org"));
        final String wikitext = "== Section 2 ==\n\nEditing section INSERT RANDOM & HERE test at " + System.currentTimeMillis();
        final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
        app.getEditTokenStorage().get(title.getSite(), new EditTokenStorage.TokenRetrievedCallback() {
            @Override
            public void onTokenRetrieved(String token) {
                new DoEditTask(getInstrumentation().getTargetContext(), title, wikitext, SECTION_ID, token, "", false) {
                    @Override
                    public void onFinish(EditingResult result) {
                        assertNotNull(result);
                        assertEquals("Success", result.getResult());
                        new FetchSectionWikitextTask(getInstrumentation().getTargetContext(), title, SECTION_ID) {
                            @Override
                            public void onFinish(String result) {
                                assertNotNull(result);
                                assertEquals(wikitext, result);
                                completionLatch.countDown();
                            }
                        }.execute();
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        // borrowed mainly from EditSectionActivity:
                        final WikipediaApp app = WikipediaApp.getInstance();
                        if (caught instanceof ApiException) {
                            ApiException ee = (ApiException) caught;
                            if (app.getUserInfoStorage().isLoggedIn() && "badtoken".equals(ee.getCode())) {
                                // looks like our session expired.
                                app.getEditTokenStorage().clearAllTokens();
                                app.getCookieManager().clearAllCookies();

                                User user = app.getUserInfoStorage().getUser();
                                new LoginTask(app, app.getPrimarySite(), user.getUsername(), user.getPassword()) {
                                    @Override
                                    public void onFinish(LoginResult result) {
                                        assertEquals("Login failed!", "Success", result.getCode());
                                        try {
                                            doSave(completionLatch);
                                        } catch (Throwable throwable) {
                                            fail("Retry failed: " + throwable.getMessage());
                                        }
                                    }
                                }.execute();
                            }
                        } else {
                            throw new RuntimeException(caught);
                        }
                    }
                }.execute();
            }

            @Override
            public void onTokenFailed(Throwable caught) {
                fail("Fetching token failed: " + caught.getMessage());
            }
        });
    }
}

