package org.catrobat.catroid.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class ClickThroughWebView extends WebView {

    private boolean isClickThrough = true;


    public ClickThroughWebView(Context context) {
        super(context);
    }

    public ClickThroughWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClickThroughWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setClickThrough(boolean isClickThrough) {
        this.isClickThrough = isClickThrough;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (isClickThrough) {


            return false;
        }

        return super.onTouchEvent(event);
    }
}