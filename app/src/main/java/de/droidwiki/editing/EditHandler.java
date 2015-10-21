package de.droidwiki.editing;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import org.json.JSONObject;
import de.droidwiki.R;
import de.droidwiki.WikipediaApp;
import de.droidwiki.analytics.ProtectedEditAttemptFunnel;
import de.droidwiki.analytics.SavedPagesFunnel;
import de.droidwiki.bridge.CommunicationBridge;
import de.droidwiki.history.HistoryEntry;
import de.droidwiki.page.Page;
import de.droidwiki.page.PageActivity;
import de.droidwiki.page.PageFragment;
import de.droidwiki.page.Section;

public class EditHandler implements CommunicationBridge.JSEventListener {
    public static final int RESULT_REFRESH_PAGE = 1;

    private final PageFragment fragment;
    private ProtectedEditAttemptFunnel funnel;
    private Page currentPage;

    public EditHandler(PageFragment fragment, CommunicationBridge bridge) {
        this.fragment = fragment;
        bridge.addListener("editSectionClicked", this);
    }

    public void setPage(Page page) {
        this.currentPage = page;
        this.funnel = new ProtectedEditAttemptFunnel(WikipediaApp.getInstance(), page.getTitle().getSite());
    }

    private void showUneditableDialog() {
        new AlertDialog.Builder(fragment.getActivity())
                .setCancelable(false)
                .setTitle(R.string.page_protected_can_not_edit_title)
                .setMessage(R.string.page_protected_can_not_edit)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
        funnel.log(currentPage.getPageProperties().getEditProtectionStatus());
    }

    /**
     * Variable indicating whether the current page was refreshed (by clicking on edit
     * when it was a saved page and choosing to refresh. Used for accurate event logging
     */
    private boolean wasRefreshed = false;
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        if (!fragment.isAdded()) {
            return;
        }
        if (messageType.equals("editSectionClicked")) {
            final SavedPagesFunnel savedPagesFunnel = WikipediaApp.getInstance().getFunnelManager().getSavedPagesFunnel(currentPage.getTitle().getSite());
            if (fragment.getHistoryEntry().getSource() == HistoryEntry.SOURCE_SAVED_PAGE) {
                savedPagesFunnel.logEditAttempt();
                new AlertDialog.Builder(fragment.getActivity())
                        .setCancelable(false)
                        .setMessage(R.string.edit_saved_page_refresh)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                fragment.refreshPage(true);
                                savedPagesFunnel.logEditRefresh();
                                wasRefreshed = true;
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
                return;
            }
            if (!currentPage.getPageProperties().canEdit()) {
                showUneditableDialog();
                return;
            }
            int id = messagePayload.optInt("sectionID");
            Section section = currentPage.getSections().get(id);
            Intent intent = new Intent(fragment.getActivity(), EditSectionActivity.class);
            intent.setAction(EditSectionActivity.ACTION_EDIT_SECTION);
            intent.putExtra(EditSectionActivity.EXTRA_SECTION_ID, section.getId());
            intent.putExtra(EditSectionActivity.EXTRA_SECTION_HEADING, section.getHeading());
            intent.putExtra(EditSectionActivity.EXTRA_TITLE, currentPage.getTitle());
            intent.putExtra(EditSectionActivity.EXTRA_PAGE_PROPS, currentPage.getPageProperties());
            fragment.startActivityForResult(intent, PageActivity.ACTIVITY_REQUEST_EDIT_SECTION);
            if (wasRefreshed) {
                savedPagesFunnel.logEditAfterRefresh();
            }
        }
    }
}
