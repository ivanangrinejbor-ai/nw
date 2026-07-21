package org.catrobat.catroid.utils;

/**
 * Stub — manages native Android Views overlaid on the GL surface.
 * TODO: implement actual View attachment to StageActivity's FrameLayout.
 */
public class OverlayViewManager {

    public static void setViewAsOverlay(String viewId, boolean asOverlay) {
        // TODO: toggle Z-order / overlay flag for the given native view
    }

    public static void setViewDraggable(String viewId, boolean draggable) {
        // TODO: attach/detach drag listener
    }

    public static void setDragHandle(String viewId, String handleId) {
        // TODO: set a child view as the drag handle
    }
}
