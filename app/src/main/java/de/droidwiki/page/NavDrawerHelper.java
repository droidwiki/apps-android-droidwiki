package de.droidwiki.page;

import android.content.Intent;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import de.droidwiki.WikipediaApp;
import de.droidwiki.analytics.LoginFunnel;
import de.droidwiki.analytics.NavMenuFunnel;
import de.droidwiki.history.HistoryEntry;
import de.droidwiki.history.HistoryFragment;
import de.droidwiki.login.LoginActivity;
import de.droidwiki.nearby.NearbyFragment;
import de.droidwiki.random.RandomHandler;
import de.droidwiki.savedpages.SavedPagesFragment;
import de.droidwiki.settings.SettingsActivity;
import de.droidwiki.settings.SettingsActivityGB;
import de.droidwiki.util.ApiUtil;
import de.droidwiki.util.FeedbackUtil;

public class NavDrawerHelper {

    private static final int[] NAV_DRAWER_ACTION_ITEMS_LOGGED_IN_ONLY = {
            de.droidwiki.R.id.nav_item_username
    };

    private final WikipediaApp app = WikipediaApp.getInstance();
    private final PageActivity page;
    private MenuItem usernameContainer;
    private MenuItem loginContainer;
    private MenuItem[] loggedInOnyActionViews = new MenuItem[NAV_DRAWER_ACTION_ITEMS_LOGGED_IN_ONLY.length];
    private NavMenuFunnel funnel;

    public NavDrawerHelper(@NonNull PageActivity page) {
        this.funnel = new NavMenuFunnel();
        this.page = page;
        page.getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                updateItemSelection(NavDrawerHelper.this.page.getTopFragment());
            }
        });
    }

    public NavMenuFunnel getFunnel() {
        return funnel;
    }

    public void setupDynamicNavDrawerItems() {
        for (int i = 0; i < NAV_DRAWER_ACTION_ITEMS_LOGGED_IN_ONLY.length; i++) {
            loggedInOnyActionViews[i] = page.getNavMenu().findItem(NAV_DRAWER_ACTION_ITEMS_LOGGED_IN_ONLY[i]);
        }

        if (usernameContainer == null) {
            usernameContainer = page.getNavMenu().findItem(de.droidwiki.R.id.nav_item_username);
            loginContainer = page.getNavMenu().findItem(de.droidwiki.R.id.nav_item_login);
        }

        updateLoginButtonStatus();
        updateWikipediaZeroStatus();
    }

    public NavigationView.OnNavigationItemSelectedListener getNewListener() {
        return new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case de.droidwiki.R.id.nav_item_today:
                        page.displayMainPageInCurrentTab();
                        funnel.logToday();
                        break;
                    case de.droidwiki.R.id.nav_item_history:
                        page.pushFragment(new HistoryFragment());
                        funnel.logHistory();
                        break;
                    case de.droidwiki.R.id.nav_item_saved_pages:
                        page.pushFragment(new SavedPagesFragment());
                        funnel.logSavedPages();
                        break;
                    case de.droidwiki.R.id.nav_item_more:
                        launchSettingsActivity();
                        funnel.logMore();
                        break;
                    case de.droidwiki.R.id.nav_item_login:
                        launchLoginActivity();
                        funnel.logLogin();
                        break;
                    case de.droidwiki.R.id.nav_item_random:
                        page.getRandomHandler().doVisitRandomArticle();
                        funnel.logRandom();
                        break;
                    default:
                        return false;
                }
                clearItemHighlighting();
                menuItem.setChecked(true);
                page.setNavItemSelected(true);
                return true;
            }
        };
    }

    public RandomHandler getNewRandomHandler() {
        return new RandomHandler(page.getNavMenu().findItem(de.droidwiki.R.id.nav_item_random),
                new RandomHandler.RandomListener() {
                    @Override
                    public void onRandomPageReceived(@Nullable PageTitle title) {
                        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_RANDOM);
                        page.displayNewPage(title, historyEntry, PageActivity.TabPosition.CURRENT_TAB, true);
                    }

                    @Override
                    public void onRandomPageFailed(Throwable caught) {
                        FeedbackUtil.showError(page.getContentView(), caught);
                    }
                });
    }

    public void updateItemSelection(Fragment fragment) {
        @IdRes Integer id = fragmentToMenuId(fragment);
        if (id != null) {
            setMenuItemSelection(id);
        }
    }

    private void setMenuItemSelection(@IdRes int id) {
        clearItemHighlighting();

        // Special case: don't highlight today if it's not the main page.
        if (id != de.droidwiki.R.id.nav_item_today || isMainPage()) {
            MenuItem menuItem = page.getNavMenu().findItem(id);
            menuItem.setChecked(true);
        }
    }

    private boolean isMainPage() {
        return page.getCurPageFragment() != null
                && page.getCurPageFragment().getPage() != null
                && page.getCurPageFragment().getPage().isMainPage();
    }

    @Nullable @IdRes private Integer fragmentToMenuId(Fragment fragment) {
        if (fragment instanceof PageFragment) {
            return de.droidwiki.R.id.nav_item_today;
        } else if (fragment instanceof HistoryFragment) {
            return de.droidwiki.R.id.nav_item_history;
        } else if (fragment instanceof SavedPagesFragment) {
            return de.droidwiki.R.id.nav_item_saved_pages;
        }
        return null;
    }

    /**
     * Update login menu item to reflect login status.
     */
    private void updateLoginButtonStatus() {
        if (app.getUserInfoStorage().isLoggedIn()) {
            loginContainer.setVisible(false);
            for (MenuItem loggedInOnyActionView : loggedInOnyActionViews) {
                loggedInOnyActionView.setVisible(true);
            }
            usernameContainer.setTitle(app.getUserInfoStorage().getUser().getUsername());
        } else {
            loginContainer.setVisible(true);
            for (MenuItem loggedInOnyActionView : loggedInOnyActionViews) {
                loggedInOnyActionView.setVisible(false);
            }
        }
    }

    /**
     * Add Wikipedia Zero entry to nav menu if W0 is active.
     */
    private void updateWikipediaZeroStatus() {
        MenuItem wikipediaZeroText = page.getNavMenu().findItem(de.droidwiki.R.id.nav_item_zero);
        if (app.getWikipediaZeroHandler().isZeroEnabled()) {
            wikipediaZeroText.setTitle(app.getWikipediaZeroHandler().getCarrierMessage().getMsg());
            wikipediaZeroText.setVisible(true);
        } else {
            wikipediaZeroText.setVisible(false);
        }
    }

    /**
     * Un-highlight all nav menu entries.
     */
    private void clearItemHighlighting() {
        for (int i = 0; i < page.getNavMenu().size(); i++) {
            page.getNavMenu().getItem(i).setChecked(false);
        }
    }

    private void launchSettingsActivity() {
        page.closeNavDrawer();
        page.startActivityForResult(new Intent().setClass(app, ApiUtil.hasHoneyComb() ? SettingsActivity.class : SettingsActivityGB.class),
                SettingsActivity.ACTIVITY_REQUEST_SHOW_SETTINGS);
    }

    private void launchLoginActivity() {
        page.closeNavDrawer();
        page.startActivityForResult(new Intent(app, LoginActivity.class)
                .putExtra(LoginActivity.LOGIN_REQUEST_SOURCE, LoginFunnel.SOURCE_NAV),
                LoginActivity.REQUEST_LOGIN);
    }
}
