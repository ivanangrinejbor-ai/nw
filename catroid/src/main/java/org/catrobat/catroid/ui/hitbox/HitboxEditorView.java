package org.catrobat.catroid.ui.hitbox;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.catrobat.catroid.common.HitboxData;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom view for the Hitbox Editor.
 * Displays the sprite image and allows manipulating hitboxes via touch:
 * - Tap: select hitbox (shows 4 resize handles)
 * - Drag handle: resize from that side
 * - Long press + drag: move entire hitbox
 * - Double tap: enter rotation mode (ring appears, drag to rotate)
 */
public class HitboxEditorView extends View {

    private float density = 1f;
    private static final float HANDLE_RADIUS_DP = 12f;
    private static final float RING_PADDING_DP = 20f;
    private static final float DRAG_SLOP_DP = 8f;
    private static final int MAX_BITMAP_DIM = 2048;
    private static final long LONG_PRESS_MS = 400;

    // Interaction modes
    private static final int MODE_NONE = 0;
    private static final int MODE_MOVE = 1;
    private static final int MODE_RESIZE_TOP = 2;
    private static final int MODE_RESIZE_BOTTOM = 3;
    private static final int MODE_RESIZE_LEFT = 4;
    private static final int MODE_RESIZE_RIGHT = 5;
    private static final int MODE_ROTATE = 6;

    private Bitmap spriteBitmap;
    /** Reusable destination rect for drawing the (possibly downsampled) bitmap. */
    private final RectF dstRect = new RectF();
    /** Original (full-resolution) image dimensions — hitbox coordinates live in this space. */
    private int origW = 0;
    private int origH = 0;
    private float imageScale = 1f;
    private float imageOffsetX = 0f;
    private float imageOffsetY = 0f;

    private final List<HitboxData> hitboxes = new ArrayList<>();
    private int selectedIndex = -1;
    private int interactionMode = MODE_NONE;
    private boolean rotationMode = false;

    private float lastTouchX, lastTouchY;
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private boolean longPressTriggered = false;
    private boolean hasDragged = false;
    private float downX, downY;
    /** The single pointer we track. A second finger cancels the active gesture. */
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    // Paints
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hitboxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handleBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudValPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private OnHitboxChangeListener changeListener;

    public interface OnHitboxChangeListener {
        void onHitboxesChanged();
    }

    public HitboxEditorView(Context context) {
        super(context);
        init();
    }

    public HitboxEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HitboxEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        hitboxPaint.setStyle(Paint.Style.STROKE);
        hitboxPaint.setStrokeWidth(3.5f);
        hitboxPaint.setColor(0xFF00E676); // emerald green

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0x2500E676);

        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(4.5f);
        selectedPaint.setColor(0xFFFFD600); // glowing amber/gold

        selectedFillPaint.setStyle(Paint.Style.FILL);
        selectedFillPaint.setColor(0x35FFD600);

        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setColor(0xFFFFD600);

        handleBorderPaint.setStyle(Paint.Style.STROKE);
        handleBorderPaint.setStrokeWidth(3f);
        handleBorderPaint.setColor(0xFFFFFFFF);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(3.5f);
        ringPaint.setColor(0xFF00E5FF); // electric cyan

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1.5f);
        gridPaint.setColor(0x2294A3B8);

        hudBgPaint.setStyle(Paint.Style.FILL);
        hudBgPaint.setColor(0xD90F172A); // dark slate glassmorphism

        hudTextPaint.setColor(0xFF94A3B8);
        hudTextPaint.setTextSize(32f);
        hudTextPaint.setFakeBoldText(true);

        hudValPaint.setColor(0xFFF8FAFC);
        hudValPaint.setTextSize(32f);
        hudValPaint.setFakeBoldText(true);

        imagePaint.setFilterBitmap(true);
    }

    private float dp(float dpValue) {
        return dpValue * density;
    }

    public void setOnHitboxChangeListener(OnHitboxChangeListener listener) {
        this.changeListener = listener;
    }

    public void setSpriteImage(String path) {
        spriteBitmap = null;
        origW = 0;
        origH = 0;
        if (path != null) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            origW = bounds.outWidth;
            origH = bounds.outHeight;
            int sample = 1;
            if (origW > 0 && origH > 0) {
                while (Math.max(origW, origH) / sample > MAX_BITMAP_DIM) {
                    sample *= 2;
                }
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            spriteBitmap = BitmapFactory.decodeFile(path, opts);
            if (spriteBitmap != null && (origW <= 0 || origH <= 0)) {
                origW = spriteBitmap.getWidth();
                origH = spriteBitmap.getHeight();
            }
        }
        computeImageTransform();
        requestLayout();
        invalidate();
    }

    public void setHitboxes(List<HitboxData> boxes) {
        hitboxes.clear();
        if (boxes != null) {
            for (HitboxData hb : boxes) {
                hitboxes.add(hb.copy());
            }
        }
        selectedIndex = hitboxes.isEmpty() ? -1 : 0;
        invalidate();
    }

    public List<HitboxData> getHitboxes() {
        return new ArrayList<>(hitboxes);
    }

    public void addHitbox() {
        float w = origW > 0 ? origW * 0.25f : 100f;
        float h = origH > 0 ? origH * 0.25f : 100f;
        hitboxes.add(new HitboxData(0, 0, w, h, 0));
        selectedIndex = hitboxes.size() - 1;
        rotationMode = false;
        notifyChange();
        invalidate();
    }

    public void deleteSelected() {
        if (selectedIndex >= 0 && selectedIndex < hitboxes.size()) {
            hitboxes.remove(selectedIndex);
            selectedIndex = hitboxes.isEmpty() ? -1 : Math.min(selectedIndex, hitboxes.size() - 1);
            rotationMode = false;
            notifyChange();
            invalidate();
        }
    }

    private void notifyChange() {
        if (changeListener != null) changeListener.onHitboxesChanged();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeImageTransform();
    }

    private void computeImageTransform() {
        if (origW <= 0 || origH <= 0) return;
        float viewW = getWidth();
        float viewH = getHeight();
        if (viewW <= 0 || viewH <= 0) return;
        imageScale = Math.min(viewW * 0.8f / origW, viewH * 0.8f / origH);
        imageOffsetX = (viewW - origW * imageScale) / 2f;
        imageOffsetY = (viewH - origH * imageScale) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        // Draw grid crosshair at center
        canvas.drawLine(cx, 0, cx, getHeight(), gridPaint);
        canvas.drawLine(0, cy, getWidth(), cy, gridPaint);

        // Draw sprite image
        if (spriteBitmap != null && origW > 0 && origH > 0) {
            dstRect.set(imageOffsetX, imageOffsetY,
                imageOffsetX + origW * imageScale,
                imageOffsetY + origH * imageScale);
            canvas.drawBitmap(spriteBitmap, null, dstRect, imagePaint);
        }

        // Draw hitboxes
        for (int i = 0; i < hitboxes.size(); i++) {
            HitboxData hb = hitboxes.get(i);
            boolean selected = (i == selectedIndex);
            drawHitbox(canvas, hb, selected, cx, cy);
        }

        // Draw HUD overlay badge
        drawHudOverlay(canvas);
    }

    private void drawHitbox(Canvas canvas, HitboxData hb, boolean selected, float cx, float cy) {
        canvas.save();
        canvas.translate(cx + hb.x * imageScale, cy + hb.y * imageScale);
        canvas.rotate(hb.rotation);

        float hw = hb.width * imageScale / 2f;
        float hh = hb.height * imageScale / 2f;
        RectF rect = new RectF(-hw, -hh, hw, hh);

        // Fill background translucently
        canvas.drawRect(rect, selected ? selectedFillPaint : fillPaint);
        // Draw stroke outline
        canvas.drawRect(rect, selected ? selectedPaint : hitboxPaint);

        if (selected) {
            if (rotationMode) {
                float ringRadius = Math.max(hw, hh) + dp(RING_PADDING_DP);
                ringPaint.setPathEffect(new DashPathEffect(new float[]{14, 8}, 0));
                canvas.drawCircle(0, 0, ringRadius, ringPaint);
                ringPaint.setPathEffect(null);

                // Handle at top of ring
                handlePaint.setColor(0xFF00E5FF);
                canvas.drawCircle(0, -ringRadius, dp(HANDLE_RADIUS_DP * 0.9f), handlePaint);
                canvas.drawCircle(0, -ringRadius, dp(HANDLE_RADIUS_DP * 0.9f), handleBorderPaint);
                handlePaint.setColor(0xFFFFD600);
            } else {
                drawHandle(canvas, 0, -hh); // top
                drawHandle(canvas, 0, hh);  // bottom
                drawHandle(canvas, -hw, 0); // left
                drawHandle(canvas, hw, 0);  // right
            }
        }

        canvas.restore();
    }

    private void drawHandle(Canvas canvas, float x, float y) {
        canvas.drawCircle(x, y, dp(HANDLE_RADIUS_DP), handlePaint);
        canvas.drawCircle(x, y, dp(HANDLE_RADIUS_DP), handleBorderPaint);
    }

    private void drawHudOverlay(Canvas canvas) {
        if (selectedIndex < 0 || selectedIndex >= hitboxes.size()) {
            return;
        }
        HitboxData hb = hitboxes.get(selectedIndex);
        String info = String.format("Box #%d  │  %dx%d px  │  X: %+d  Y: %+d  │  %.0f°",
                selectedIndex + 1, (int) hb.width, (int) hb.height, (int) hb.x, (int) hb.y, hb.rotation);

        float textWidth = hudValPaint.measureText(info);
        float padX = 24f;
        float padY = 16f;
        float left = 24f;
        float top = 24f;
        float right = left + textWidth + padX * 2f;
        float bottom = top + 44f + padY;

        RectF bg = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(bg, 16f, 16f, hudBgPaint);
        canvas.drawText(info, left + padX, top + 36f, hudValPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                activePointerId = event.getPointerId(0);
                float x = event.getX();
                float y = event.getY();
                downX = x;
                downY = y;
                lastTouchX = x;
                lastTouchY = y;
                longPressTriggered = false;
                hasDragged = false;
                longPressHandler.postDelayed(this::onLongPress, LONG_PRESS_MS);
                handleDown(x, y);
                return true;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                // A second finger landed — cancel the in-flight gesture so a resize/
                // rotate/move never jumps to the other finger's coordinates.
                longPressHandler.removeCallbacksAndMessages(null);
                interactionMode = MODE_NONE;
                longPressTriggered = false;
                hasDragged = true; // suppress tap/double-tap for this gesture
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex < 0) {
                    return true; // tracked finger is gone; ignore stray moves
                }
                float x = event.getX(pointerIndex);
                float y = event.getY(pointerIndex);
                if (!hasDragged && Math.hypot(x - downX, y - downY) > dp(DRAG_SLOP_DP)) {
                    hasDragged = true;
                    longPressHandler.removeCallbacksAndMessages(null);
                }
                if (hasDragged || longPressTriggered) {
                    handleMove(x, y);
                }
                lastTouchX = x;
                lastTouchY = y;
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                longPressHandler.removeCallbacksAndMessages(null);
                if (!longPressTriggered && !hasDragged) {
                    // Stationary release — treat as tap / double-tap
                    handleTap(event.getX(), event.getY());
                }
                interactionMode = MODE_NONE;
                longPressTriggered = false;
                hasDragged = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private long lastTapTime = 0;
    private float lastTapX = 0, lastTapY = 0;

    private void handleTap(float x, float y) {
        long now = System.currentTimeMillis();
        float dist = (float) Math.hypot(x - lastTapX, y - lastTapY);

        if (now - lastTapTime < 300 && dist < 40f) {
            // Double tap → toggle rotation mode
            if (selectedIndex >= 0) {
                rotationMode = !rotationMode;
                invalidate();
            }
            lastTapTime = 0;
            return;
        }

        lastTapTime = now;
        lastTapX = x;
        lastTapY = y;

        // Single tap: select hitbox or handle
        if (selectedIndex >= 0 && !rotationMode) {
            int handle = getHandleAt(x, y);
            if (handle != MODE_NONE) {
                interactionMode = handle;
                return;
            }
        }

        // Select hitbox under touch
        int idx = getHitboxAt(x, y);
        if (idx >= 0) {
            selectedIndex = idx;
            rotationMode = false;
        } else {
            selectedIndex = -1;
            rotationMode = false;
        }
        invalidate();
    }

    private void onLongPress() {
        if (selectedIndex >= 0) {
            longPressTriggered = true;
            interactionMode = MODE_MOVE;
            invalidate();
        }
    }

    private void handleDown(float x, float y) {
        if (selectedIndex >= 0) {
            if (rotationMode) {
                interactionMode = MODE_ROTATE;
            } else {
                int handle = getHandleAt(x, y);
                if (handle != MODE_NONE) {
                    interactionMode = handle;
                }
            }
        }
    }

    private void handleMove(float x, float y) {
        if (selectedIndex < 0 || selectedIndex >= hitboxes.size()) return;
        HitboxData hb = hitboxes.get(selectedIndex);
        float dx = (x - lastTouchX) / imageScale;
        float dy = (y - lastTouchY) / imageScale;

        switch (interactionMode) {
            case MODE_MOVE:
                hb.x += dx;
                hb.y += dy;
                break;
            case MODE_RESIZE_TOP:
                // Drag top edge: bottom edge stays fixed, center shifts by half
                hb.height -= dy;
                hb.y += dy / 2f;
                if (hb.height < 10) hb.height = 10;
                break;
            case MODE_RESIZE_BOTTOM:
                // Drag bottom edge: top edge stays fixed
                hb.height += dy;
                hb.y += dy / 2f;
                if (hb.height < 10) hb.height = 10;
                break;
            case MODE_RESIZE_LEFT:
                // Drag left edge: right edge stays fixed
                hb.width -= dx;
                hb.x += dx / 2f;
                if (hb.width < 10) hb.width = 10;
                break;
            case MODE_RESIZE_RIGHT:
                // Drag right edge: left edge stays fixed
                hb.width += dx;
                hb.x += dx / 2f;
                if (hb.width < 10) hb.width = 10;
                break;
            case MODE_ROTATE:
                float cx = getWidth() / 2f + hb.x * imageScale;
                float cy = getHeight() / 2f + hb.y * imageScale;
                float angle = (float) Math.toDegrees(Math.atan2(y - cy, x - cx));
                hb.rotation = angle + 90; // offset so top = 0
                break;
        }

        if (interactionMode != MODE_NONE) {
            notifyChange();
            invalidate();
        }
    }

    /**
     * Check if touch is on a resize handle of the selected hitbox.
     */
    private int getHandleAt(float x, float y) {
        if (selectedIndex < 0 || selectedIndex >= hitboxes.size()) return MODE_NONE;
        HitboxData hb = hitboxes.get(selectedIndex);
        float cx = getWidth() / 2f + hb.x * imageScale;
        float cy = getHeight() / 2f + hb.y * imageScale;

        // Transform touch into hitbox local space (accounting for rotation)
        float rad = (float) Math.toRadians(-hb.rotation);
        float localX = (x - cx) * (float) Math.cos(rad) - (y - cy) * (float) Math.sin(rad);
        float localY = (x - cx) * (float) Math.sin(rad) + (y - cy) * (float) Math.cos(rad);

        float hw = hb.width * imageScale / 2f;
        float hh = hb.height * imageScale / 2f;
        float threshold = dp(HANDLE_RADIUS_DP * 1.5f);

        if (Math.hypot(localX, localY + hh) < threshold) return MODE_RESIZE_TOP;
        if (Math.hypot(localX, localY - hh) < threshold) return MODE_RESIZE_BOTTOM;
        if (Math.hypot(localX + hw, localY) < threshold) return MODE_RESIZE_LEFT;
        if (Math.hypot(localX - hw, localY) < threshold) return MODE_RESIZE_RIGHT;

        return MODE_NONE;
    }

    /**
     * Find which hitbox contains the touch point.
     */
    private int getHitboxAt(float x, float y) {
        // Iterate in reverse (top-most first)
        for (int i = hitboxes.size() - 1; i >= 0; i--) {
            HitboxData hb = hitboxes.get(i);
            float cx = getWidth() / 2f + hb.x * imageScale;
            float cy = getHeight() / 2f + hb.y * imageScale;

            float rad = (float) Math.toRadians(-hb.rotation);
            float localX = (x - cx) * (float) Math.cos(rad) - (y - cy) * (float) Math.sin(rad);
            float localY = (x - cx) * (float) Math.sin(rad) + (y - cy) * (float) Math.cos(rad);

            float hw = hb.width * imageScale / 2f;
            float hh = hb.height * imageScale / 2f;

            if (Math.abs(localX) <= hw && Math.abs(localY) <= hh) {
                return i;
            }
        }
        return -1;
    }
}
