package de.droidwiki.views;

import de.droidwiki.util.ThrowableUtil;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class WikiErrorView extends FrameLayout {

    private TextView errorTextView;
    private Button retryButton;
    private TextView messageTextView;

    public WikiErrorView(Context context) {
        this(context, null);
    }

    public WikiErrorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WikiErrorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inflate(context, de.droidwiki.R.layout.custom_error_view, this);

        errorTextView = (TextView) findViewById(de.droidwiki.R.id.error_text);
        retryButton = (Button) findViewById(de.droidwiki.R.id.retry_button);
        messageTextView = (TextView) findViewById(de.droidwiki.R.id.server_message_text);
    }

    public void setRetryButtonVisible(boolean visible) {
        retryButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setRetryClickListener(OnClickListener listener) {
        retryButton.setOnClickListener(listener);
    }

    public void setError(@NonNull Throwable e) {
        ThrowableUtil.AppError error = ThrowableUtil.getAppError(getContext(), e);
        errorTextView.setText(error.getError());
        messageTextView.setText(error.getDetail());
    }
}