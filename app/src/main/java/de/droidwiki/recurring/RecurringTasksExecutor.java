package de.droidwiki.recurring;

import de.droidwiki.RemoteConfigRefreshTask;
import de.droidwiki.WikipediaApp;
import de.droidwiki.alphaupdater.AlphaUpdateChecker;
import de.droidwiki.concurrency.ExecutorService;
import de.droidwiki.concurrency.SaneAsyncTask;
import de.droidwiki.page.snippet.SharedImageCleanupTask;

import java.util.concurrent.Executor;

public class RecurringTasksExecutor {
    private final WikipediaApp app;

    public RecurringTasksExecutor(WikipediaApp app) {
        this.app = app;
    }

    public void run() {
        Executor executor = ExecutorService.getSingleton().getExecutor(RecurringTasksExecutor.class, 1);
        SaneAsyncTask<Void> task = new SaneAsyncTask<Void>(executor) {
            @Override
            public Void performTask() throws Throwable {
                RecurringTask[] allTasks = new RecurringTask[] {
                        // Has list of all rotating tasks that need to be run
                        new RemoteConfigRefreshTask(app),
                        new SharedImageCleanupTask(app),
                        new DailyEventTask(app)
                };
                for (RecurringTask task: allTasks) {
                    task.runIfNecessary();
                }
                if (WikipediaApp.getInstance().getReleaseType() == WikipediaApp.RELEASE_ALPHA) {
                    new AlphaUpdateChecker(app).runIfNecessary();
                }
                return null;
            }
        };
        task.execute();
    }
}
