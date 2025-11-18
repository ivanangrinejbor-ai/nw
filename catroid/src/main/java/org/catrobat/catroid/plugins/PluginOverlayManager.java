
package org.catrobat.catroid.plugins;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Map;

public class PluginOverlayManager {
    private static volatile PluginOverlayManager instance;

    private final Map<String, View> pluginViews = new HashMap<>();
    private Activity currentActivity;
    private WindowManager windowManager;

    private PluginOverlayManager() {}

    public static PluginOverlayManager getInstance() {
        if (instance == null) {
            synchronized (PluginOverlayManager.class) {
                if (instance == null) {
                    instance = new PluginOverlayManager();
                }
            }
        }
        return instance;
    }


    public void attach(Activity activity) {
        this.currentActivity = activity;
        this.windowManager = activity.getWindowManager();
        Log.d("PluginOverlayManager", "Attached to activity: " + activity.getLocalClassName());
    }



    public void detach(Activity activity) {
        if (this.currentActivity == activity) {
            this.currentActivity = null;
            this.windowManager = null;
            Log.d("PluginOverlayManager", "Detached from activity: " + activity.getLocalClassName());
        }
    }

    public void addView(String viewId, View view, int x, int y) {

        addView(viewId, view, x, y, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public void addAsRenderEffect(String viewId, View view) {
        if (windowManager == null) {
            Log.e("PluginOverlayManager", "Cannot add render effect, WindowManager is not available.");
            return;
        }

        if (pluginViews.containsKey(viewId)) {
            removeView(viewId);
        }

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,

                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);


        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        try {
            windowManager.addView(view, params);
            pluginViews.put(viewId, view);
            Log.d("PluginOverlayManager", "Render effect added with id: " + viewId);
        } catch (Exception e) {
            Log.e("PluginOverlayManager", "Failed to add render effect to WindowManager", e);
        }
    }


    public void addView(String viewId, View view, int x, int y, int width, int height) {
        if (windowManager == null) {
            Log.e("PluginOverlayManager", "Cannot add view, WindowManager is not available.");
            return;
        }

        if (pluginViews.containsKey(viewId)) {
            removeView(viewId);
        }

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width,
                height,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x;
        params.y = y;

        try {
            windowManager.addView(view, params);
            pluginViews.put(viewId, view);
            Log.d("PluginOverlayManager", "View added with id: " + viewId + " at (" + x + "," + y + ") size (" + width + "," + height + ")");
        } catch (Exception e) {
            Log.e("PluginOverlayManager", "Failed to add view to WindowManager", e);
        }
    }

    public void removeView(String viewId) {



        View viewToRemove = pluginViews.remove(viewId);
        if (viewToRemove != null) {

            if (windowManager != null && viewToRemove.isAttachedToWindow()) {
                try {
                    windowManager.removeView(viewToRemove);
                    Log.d("PluginOverlayManager", "View removed with id: " + viewId);
                } catch (Exception e) {
                    Log.e("PluginOverlayManager", "Failed to remove view from WindowManager", e);
                }
            } else if (windowManager == null) {

                Log.w("PluginOverlayManager", "Attempted to remove view but WindowManager was null.");
            }
        }
    }






}