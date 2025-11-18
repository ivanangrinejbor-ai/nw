// Файл: org/catrobat/catroid/ui/ClickThroughWebView.java
package org.catrobat.catroid.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * Кастомный WebView, который по умолчанию "пропускает" все касания сквозь себя,
 * позволяя нажимать на элементы управления, находящиеся за ним.
 */
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

    /**
     * Позволяет плагину программно включать или отключать режим "пропускания" касаний.
     * @param isClickThrough true, если касания должны проходить насквозь (по умолчанию).
     *                       false, если WebView должен обрабатывать касания как обычно.
     */
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