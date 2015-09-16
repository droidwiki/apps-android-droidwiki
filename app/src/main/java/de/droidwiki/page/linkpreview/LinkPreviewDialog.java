package de.droidwiki.page.linkpreview;

import org.mediawiki.api.json.Api;
import de.droidwiki.analytics.GalleryFunnel;
import de.droidwiki.history.HistoryEntry;
import de.droidwiki.page.PageActivity;
import de.droidwiki.page.PageActivityLongPressHandler;
import de.droidwiki.page.PageTitle;
import de.droidwiki.R;
import de.droidwiki.WikipediaApp;
import de.droidwiki.analytics.LinkPreviewFunnel;
import de.droidwiki.page.gallery.GalleryActivity;
import de.droidwiki.page.gallery.GalleryCollection;
import de.droidwiki.page.gallery.GalleryCollectionFetchTask;
import de.droidwiki.page.gallery.GalleryThumbnailScrollView;
import de.droidwiki.util.ApiUtil;
import de.droidwiki.util.FeedbackUtil;
import de.droidwiki.views.ViewUtil;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Map;

public class LinkPreviewDialog extends SwipeableBottomDialog implements DialogInterface.OnDismissListener {
    private static final String TAG = "LinkPreviewDialog";

    private boolean navigateSuccess = false;

    private ProgressBar progressBar;
    private TextView extractText;
    private GalleryThumbnailScrollView thumbnailGallery;

    private PageTitle pageTitle;
    private int entrySource;

    private LinkPreviewFunnel funnel;
    private LinkPreviewContents contents;
    private OnNavigateListener onNavigateListener;
    private LongPressHandler overflowMenuHandler;

    private GalleryThumbnailScrollView.GalleryViewListener galleryViewListener
            = new GalleryThumbnailScrollView.GalleryViewListener() {
        @Override
        public void onGalleryItemClicked(String imageName) {
            PageTitle imageTitle = new PageTitle(imageName, pageTitle.getSite());
            GalleryActivity.showGallery(getActivity(), pageTitle, imageTitle,
                    GalleryFunnel.SOURCE_LINK_PREVIEW);
        }
    };

    private View.OnClickListener goToPageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            goToLinkedPage();
        }
    };

    public static LinkPreviewDialog newInstance(PageTitle title, int entrySource) {
        LinkPreviewDialog dialog = new LinkPreviewDialog();
        Bundle args = new Bundle();
        args.putParcelable("title", title);
        args.putInt("entrySource", entrySource);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.LinkPreviewDialog);
        setDialogPeekHeight((int) getResources().getDimension(R.dimen.linkPreviewPeekHeight));
    }

    @Override
    protected View inflateDialogView(LayoutInflater inflater, ViewGroup container) {
        WikipediaApp app = WikipediaApp.getInstance();
        pageTitle = getArguments().getParcelable("title");
        entrySource = getArguments().getInt("entrySource");

        View rootView = inflater.inflate(R.layout.dialog_link_preview, container);
        progressBar = (ProgressBar) rootView.findViewById(R.id.link_preview_progress);
        rootView.findViewById(R.id.link_preview_toolbar).setOnClickListener(goToPageListener);
        TextView titleText = (TextView) rootView.findViewById(R.id.link_preview_title);
        titleText.setText(pageTitle.getDisplayText());
        if (!ApiUtil.hasKitKat()) {
            // for oldish devices, reset line spacing to 1, since it truncates the descenders.
            titleText.setLineSpacing(0, 1.0f);
        } else if (!ApiUtil.hasLollipop()) {
            // for <5.0, give the title a bit more bottom padding, since these versions
            // incorrectly cut off the bottom of the text when line spacing is <1.
            final int bottomPadding = 8;
            ViewUtil.setBottomPaddingDp(titleText, bottomPadding);
        }

        onNavigateListener = new DefaultOnNavigateListener();
        extractText = (TextView) rootView.findViewById(R.id.link_preview_extract);

        thumbnailGallery = (GalleryThumbnailScrollView) rootView.findViewById(de.droidwiki.R.id.link_preview_thumbnail_gallery);
        if (app.isImageDownloadEnabled()) {
            new GalleryThumbnailFetchTask(pageTitle).execute();
            thumbnailGallery.setGalleryViewListener(galleryViewListener);
        }

        rootView.findViewById(R.id.link_preview_go_button).setOnClickListener(goToPageListener);

        final View overflowButton = rootView.findViewById(de.droidwiki.R.id.link_preview_overflow_button);
        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(getActivity(), overflowButton);
                popupMenu.inflate(de.droidwiki.R.menu.menu_link_preview);
                popupMenu.setOnMenuItemClickListener(menuListener);
                popupMenu.show();
            }
        });

        // show the progress bar while we load content...
        progressBar.setVisibility(View.VISIBLE);

        // and kick off the task to load all the things...
        new LinkPreviewFetchTask(app.getAPIForSite(pageTitle.getSite()), pageTitle).execute();

        funnel = new LinkPreviewFunnel(app);
        funnel.logLinkClick();

        return rootView;
    }

    public interface OnNavigateListener {
        void onNavigate(PageTitle title);
    }

    public void setOnNavigateListener(OnNavigateListener listener) {
        onNavigateListener = listener;
    }

    public void goToLinkedPage() {
        navigateSuccess = true;
        funnel.logNavigate();
        if (getDialog() != null) {
            getDialog().dismiss();
        }
        if (onNavigateListener != null) {
            onNavigateListener.onNavigate(pageTitle);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        overflowMenuHandler = new LongPressHandler(getPageActivity());
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        if (!navigateSuccess) {
            funnel.logCancel();
        }
    }

    private PageActivity getPageActivity() {
        return (PageActivity) getActivity();
    }

    private PopupMenu.OnMenuItemClickListener menuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_link_preview_save_page:
                    overflowMenuHandler.onSavePage(pageTitle);
                    dismiss();
                    return true;
                case R.id.menu_link_preview_share_page:
                    overflowMenuHandler.onShareLink(pageTitle);
                    return true;
                case R.id.menu_link_preview_copy_link:
                    overflowMenuHandler.onCopyLink(pageTitle);
                    dismiss();
                    return true;
                default:
                    break;
            }
            return false;
        }
    };

    private class DefaultOnNavigateListener implements OnNavigateListener {
        @Override
        public void onNavigate(PageTitle title) {
            HistoryEntry newEntry = new HistoryEntry(title, entrySource);
            getPageActivity().displayNewPage(title, newEntry);
        }
    }

    private class LinkPreviewFetchTask extends PreviewFetchTask {
        public LinkPreviewFetchTask(Api api, PageTitle title) {
            super(api, title);
        }

        @Override
        public void onFinish(Map<PageTitle, LinkPreviewContents> result) {
            if (!isAdded()) {
                return;
            }
            progressBar.setVisibility(View.GONE);
            if (result.size() > 0) {
                contents = (LinkPreviewContents) result.values().toArray()[0];
                layoutPreview();
            } else {
                FeedbackUtil.showMessage(getActivity(), de.droidwiki.R.string.error_network_error);
                dismiss();
            }
        }
        @Override
        public void onCatch(Throwable caught) {
            Log.e(TAG, "caught " + caught.getMessage());
            if (!isAdded()) {
                return;
            }
            progressBar.setVisibility(View.GONE);
            FeedbackUtil.showError(getActivity(), caught);
            dismiss();
        }
    }

    private void layoutPreview() {
        if (contents.getExtract().length() > 0) {
            extractText.setText(contents.getExtract());
        }

        LinearLayout.LayoutParams extractLayoutParams = (LinearLayout.LayoutParams) extractText.getLayoutParams();
        extractLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        extractText.setLayoutParams(extractLayoutParams);
    }

    private class GalleryThumbnailFetchTask extends GalleryCollectionFetchTask {
        public GalleryThumbnailFetchTask(PageTitle title) {
            super(WikipediaApp.getInstance().getAPIForSite(title.getSite()), title.getSite(), title,
                    true);
        }

        public void onGalleryResult(GalleryCollection result) {
            if (result.getItemList().size() > 2) {
                thumbnailGallery.setGalleryCollection(result);
                thumbnailGallery.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onCatch(Throwable caught) {
            // Don't worry about showing a notification to the user if this fails.
            Log.w(TAG, "Failed to fetch gallery collection.", caught);
        }
    }

    private class LongPressHandler extends PageActivityLongPressHandler {
        public LongPressHandler(@NonNull PageActivity activity) {
            super(activity);
        }
    }
}
