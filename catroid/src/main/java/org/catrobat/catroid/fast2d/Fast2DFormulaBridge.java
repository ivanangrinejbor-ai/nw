package org.catrobat.catroid.fast2d;

import org.catrobat.catroid.stage.StageActivity;

import java.lang.ref.WeakReference;

public class Fast2DFormulaBridge {
    private static FastTwoDManager getMgr() {
        WeakReference<StageActivity> ref = StageActivity.activeStageActivity;
        if (ref == null) return null;

        StageActivity activity = ref.get();
        if (activity == null || activity.stageListener == null) return null;

        return activity.stageListener.fastTwoDManager;
    }

    public static Double getX(String id) {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getPositionX(id) : 0.0;
    }

    public static Double getY(String id) {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getPositionY(id) : 0.0;
    }

    public static Double getRotation(String id) {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getRotation(id) : 0.0;
    }

    public static Double getScaleX(String id) {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getScaleX(id) : 1.0;
    }

    public static Double getScaleY(String id) {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getScaleY(id) : 1.0;
    }


    public static Double getColorR(String id) {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getColorR(id) : 255.0;
    }

    public static Double getColorG(String id) {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getColorG(id) : 255.0;
    }

    public static Double getColorB(String id) {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getColorB(id) : 255.0;
    }

    public static Double getAlpha(String id) {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getAlpha(id) : 100.0;
    }

    public static String getTexture(String id) {
        FastTwoDManager m = getMgr();
        return m != null ? m.getTextureName(id) : "";
    }


    public static Double getCamX() {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getCamX() : 0.0;
    }

    public static Double getCamY() {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getCamY() : 0.0;
    }

    public static Double getCamZoom() {
        FastTwoDManager m = getMgr();
        return m != null ? (double) m.getCamZoom() : 1.0;
    }


    public static Double isTouched(String id) {
        FastTwoDManager m = getMgr();
        return (m != null && m.isEntityTouched(id, 0)) ? 1.0 : 0.0;
    }

    public static Double isTouched(String id, int pointer) {
        FastTwoDManager m = getMgr();
        return (m != null && m.isEntityTouched(id, pointer)) ? 1.0 : 0.0;
    }
}