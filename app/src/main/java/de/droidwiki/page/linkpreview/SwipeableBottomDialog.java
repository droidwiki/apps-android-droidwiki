package de.droidwiki.page.linkpreview;

import de.droidwiki.R;
import de.droidwiki.Utils;

import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/*
 A few notes about the geometry of this special dialog class:
 It works by using a ListView to allow the user to smoothly scroll the dialog contents vertically
 along the screen. The ListView contains three items, two of which are transparent spaces, and the
 third being the actual dialog layout. Here's a breakdown of the items:

 |--------------------------|
 |  0 dismiss trigger view  | <-- 1px tall. When the user scrolls this item on screen, the dialog is
 |--------------------------|     dismissed.
 |                          |
 |                          | <-- Window tall. Allows the the dialog contents to be scrolled on or
 | 1 transparent space view |     off the screen.
 |                          |
 |                          |
 |--------------------------|
 | //////////////////////// |
 |[2 actual dialog contents]|
 | //////////////////////// |
 |--------------------------|

 The dialog window, the second transparent view, the dialog contents view are resized on
 configuration change.
 */
public abstract class SwipeableBottomDialog extends DialogFragment {
    private static final int DISMISS_TRIGGER_VIEW_HEIGHT_PX = 1;
    private static final int DISMISS_TRIGGER_VIEW_POS = 0;
    private static final int SPACE_VIEW_POS = 1;

    private ListView listView;
    private int contentPeekHeight;
    private View spaceView;

    private final AbsListView.OnScrollListener onScrollListener
            = new AbsListView.OnScrollListener() {
        @Override public void onScrollStateChanged(AbsListView view, int scrollState) { }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                             int totalItemCount) {
            if (firstVisibleItem == DISMISS_TRIGGER_VIEW_POS) {
                dismiss();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.dialog_bottom_swipe, container);

        List<View> views = new ArrayList<>();
        views.add(makeDismissTriggerView());
        spaceView = makeSpaceView();
        views.add(spaceView);
        views.add(inflateDialogView(inflater, container));

        listView = (ListView) rootView.findViewById(R.id.bottom_swipe_container_list);
        listView.setOnScrollListener(onScrollListener);
        listView.setAdapter(new SwipeableAdapter(views));
        setContentPeekHeight();

        return rootView;
    }

    protected abstract View inflateDialogView(LayoutInflater inflater, ViewGroup container);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, de.droidwiki.R.style.BottomDialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }
    @Override
    public void onStart() {
        super.onStart();
        setWindowLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setWindowLayout();
        spaceView.getLayoutParams().height = dialogHeightPx();
        listView.getLayoutParams().width = dialogWidthPx();

        // Post new scroll offset after the layout has been resized.
        listView.post(new Runnable() {
            @Override
            public void run() {
                setContentPeekHeight();
            }
        });
    }

    @Override
    public void dismiss() {
        // Since we call dismiss() from some AsyncTasks, make sure that this fragment
        // is actually active, since calling dismiss() after onSaveInstanceState() produces
        // an exception.
        if (isResumed()) {
            super.dismiss();
        }
    }

    public void setContentPeekHeight(int height) {
        contentPeekHeight = height;

        // The client may call this call this code prior to onCreateView().
        if (listView != null) {
            listView.setSelectionFromTop(SPACE_VIEW_POS, -contentPeekHeight);
        }
    }

    protected void setContentPeekHeight() {
        setContentPeekHeight(contentPeekHeight);
    }

    private void setWindowLayout() {
        // HACK: height _should_ be ViewGroup.LayoutParams.MATCH_PARENT but it doesn't work after
        //       two orientation changes. i.e., turn the device 90 degrees and the height is
        //       correct. Then turn it back -90 degrees, the height is incorrect.
        getDialog().getWindow().setLayout(dialogWidthPx(), dialogHeightPx());
    }

    private View makeDismissTriggerView() {
        return makeDummyView(ViewGroup.LayoutParams.MATCH_PARENT, DISMISS_TRIGGER_VIEW_HEIGHT_PX);
    }

    private View makeSpaceView() {
        return makeDummyView(ViewGroup.LayoutParams.MATCH_PARENT, dialogHeightPx());
    }

    private View makeDummyView(int width, int height) {
        View view = new View(getActivity());
        view.setLayoutParams(new ListView.LayoutParams(width, height));
        view.setClickable(true);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    dismiss();
                }
                return true;
            }
        });
        return view;
    }

    protected int dialogWidthPx() {
        return Math.min(getDisplayMetrics().widthPixels,
                (int) getResources().getDimension(R.dimen.swipeableDialogMaxWidth));
    }

    private int dialogHeightPx() {
        return getDisplayMetrics().heightPixels - Utils.getStatusBarHeightPx(getActivity());
    }

    private DisplayMetrics getDisplayMetrics() {
        return getResources().getDisplayMetrics();
    }

    private static final class SwipeableAdapter extends BaseAdapter {
        @NonNull private final List<View> views;

        public SwipeableAdapter(@NonNull List<View> views) {
            this.views = views;
        }

        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public Object getItem(int position) {
            return views.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return views.get(position);
        }
    }
}
