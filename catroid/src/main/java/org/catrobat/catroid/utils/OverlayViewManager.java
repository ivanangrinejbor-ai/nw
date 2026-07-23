package org.catrobat.catroid.utils;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.catrobat.catroid.stage.StageActivity;

import java.util.HashMap;
import java.util.Map;

public class OverlayViewManager {

    private static final String TAG = "OverlayViewManager";
    private static final Map<String, View.OnTouchListener> dragListeners = new HashMap<>();
    private static final Map<String, View> dragHandles = new HashMap<>();

    public static void setViewAsOverlay(String viewId, boolean asOverlay) {
        StageActivity activity = StageActivity.activeStageActivity.get();
        if (activity == null) {
            Log.w(TAG, "StageActivity not available");
            return;
        }
        View view = activity.getViewFromStage(viewId);
        if (view == null) {
            Log.w(TAG, "View not found: " + viewId);
            return;
        }
        if (asOverlay) {
            view.bringToFront();
            view.setZ(Float.MAX_VALUE);
        } else {
            view.setZ(0f);
        }
        view.invalidate();
    }

    public static void setViewDraggable(final String viewId, boolean draggable) {
        StageActivity activity = StageActivity.activeStageActivity.get();
        if (activity == null) {
            Log.w(TAG, "StageActivity not available");
            return;
        }
        final View view = activity.getViewFromStage(viewId);
        if (view == null) {
            Log.w(TAG, "View not found: " + viewId);
            return;
        }
        if (draggable) {
            View.OnTouchListener listener = new View.OnTouchListener() {
                private float lastTouchX, lastTouchY;
                private boolean dragging = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    View handle = dragHandles.get(viewId);
                    View target = handle != null ? handle : v;

                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN: {
                            dragging = true;
                            lastTouchX = event.getRawX();
                            lastTouchY = event.getRawY();
                            target.setPressed(true);
                            return true;
                        }
                        case MotionEvent.ACTION_MOVE: {
                            if (!dragging) break;
                            float dx = event.getRawX() - lastTouchX;
                            float dy = event.getRawY() - lastTouchY;
                            lastTouchX = event.getRawX();
                            lastTouchY = event.getRawY();

                            ViewGroup.MarginLayoutParams params =
                                    (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                            params.leftMargin += (int) dx;
                            params.topMargin += (int) dy;
                            v.setLayoutParams(params);
                            return true;
                        }
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                            dragging = false;
                            target.setPressed(false);
                            return true;
                        }
                    }
                    return false;
                }
            };
            dragListeners.put(viewId, listener);
            view.setOnTouchListener(listener);
        } else {
            View.OnTouchListener listener = dragListeners.remove(viewId);
            if (listener != null) {
                view.setOnTouchListener(null);
            }
            dragHandles.remove(viewId);
        }
    }

    public static void setDragHandle(String viewId, String handleId) {
        StageActivity activity = StageActivity.activeStageActivity.get();
        if (activity == null) {
            Log.w(TAG, "StageActivity not available");
            return;
        }
        View parent = activity.getViewFromStage(viewId);
        if (parent == null) {
            Log.w(TAG, "Parent view not found: " + viewId);
            return;
        }
        View handle = null;
        if (handleId != null && !handleId.isEmpty()) {
            if (parent instanceof ViewGroup) {
                handle = findChildById((ViewGroup) parent, handleId);
            }
            if (handle == null) {
                Log.w(TAG, "Handle view not found: " + handleId + " in " + viewId);
                return;
            }
        }
        if (handle != null) {
            dragHandles.put(viewId, handle);
        } else {
            dragHandles.remove(viewId);
        }
    }

    private static View findChildById(ViewGroup parent, String id) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (id.equals(child.getTag())) {
                return child;
            }
            if (child instanceof ViewGroup) {
                View found = findChildById((ViewGroup) child, id);
                if (found != null) return found;
            }
        }
        return null;
    }
}
