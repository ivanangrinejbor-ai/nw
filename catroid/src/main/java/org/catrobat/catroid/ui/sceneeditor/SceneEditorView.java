/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.ui.sceneeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.catrobat.catroid.content.Sprite;

import java.util.ArrayList;
import java.util.List;

public class SceneEditorView extends View {

	public static class SceneObject {
		public final Sprite sprite;
		public volatile Bitmap bitmap;
		public boolean isBackground;
		public float x;
		public float y;
		public float widthUnits;
		public float heightUnits;

		public SceneObject(Sprite sprite, float x, float y, float widthUnits, float heightUnits) {
			this.sprite = sprite;
			this.x = x;
			this.y = y;
			this.widthUnits = widthUnits;
			this.heightUnits = heightUnits;
		}
	}

	public interface Listener {
		void onObjectMoved(Sprite sprite, int x, int y);

		void onObjectResized(Sprite sprite, int width, int height);

		void onObjectTapped(Sprite sprite);

		void onObjectPaintRequested(Sprite sprite);
	}

	private static final float MIN_SCALE = 0.05f;
	private static final float MAX_SCALE = 20f;
	private static final float GRID_STEP_UNITS = 50f;
	private static final float TAP_SLOP_PX = 12f;
	private static final float PLACEHOLDER_HALF_PX = 40f;
	private static final long LONG_PRESS_MS = 400;

	private final List<SceneObject> objects = new ArrayList<>();
	private float virtualWidth = 480f;
	private float virtualHeight = 800f;

	private float scale = 1f;
	private float panX = 0f;
	private float panY = 0f;
	private float density = 1f;

	private int selectedIndex = -1;
	private int draggingIndex = -1;
	private int resizingIndex = -1;
	private int resizeHandle = 0;
	private boolean panning = false;
	private float lastX;
	private float lastY;
	private float downX;
	private float downY;

	private int eyeObjectIndex = -1;
	private boolean longPressFired = false;
	private final Handler longPressHandler = new Handler(Looper.getMainLooper());
	private final RectF eyeRect = new RectF();

	private final ScaleGestureDetector scaleDetector;

	private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint sceneBoundsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint eyeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint eyeGlyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint eyePupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint resizeHandlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final RectF rect = new RectF();

	private volatile Bitmap backgroundBitmap;

	private Listener listener;
	private boolean isPlayingMode = false;

	public void setPlayingMode(boolean playing) {
		this.isPlayingMode = playing;
		this.selectedIndex = -1;
		this.draggingIndex = -1;
		this.resizingIndex = -1;
		this.resizeHandle = 0;
		invalidate();
	}

	public boolean isPlayingMode() {
		return isPlayingMode;
	}

	public SceneEditorView(Context context) {
		super(context);
		scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		init();
	}

	public SceneEditorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		init();
	}

	public SceneEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		init();
	}

	private void init() {
		density = getResources().getDisplayMetrics().density;
		imagePaint.setFilterBitmap(true);

		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setStrokeWidth(1f);
		gridPaint.setColor(0x22B0BEC5);

		axisPaint.setStyle(Paint.Style.STROKE);
		axisPaint.setStrokeWidth(2f);
		axisPaint.setColor(0x5533E0FF);

		sceneBoundsPaint.setStyle(Paint.Style.STROKE);
		sceneBoundsPaint.setStrokeWidth(3f);
		sceneBoundsPaint.setColor(0xFF546E7A);

		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(2.5f);
		borderPaint.setColor(0xFF64748B);

		selectedPaint.setStyle(Paint.Style.STROKE);
		selectedPaint.setStrokeWidth(4.5f);
		selectedPaint.setColor(0xFFFFD600);

		placeholderPaint.setStyle(Paint.Style.FILL);
		placeholderPaint.setColor(0xFF15151A);

		eyeBgPaint.setStyle(Paint.Style.FILL);
		eyeBgPaint.setColor(0xF20F172A);

		eyeGlyphPaint.setStyle(Paint.Style.STROKE);
		eyeGlyphPaint.setStrokeWidth(3f);
		eyeGlyphPaint.setColor(0xFFF8FAFC);

		eyePupilPaint.setStyle(Paint.Style.FILL);
		eyePupilPaint.setColor(0xFFF8FAFC);
		resizeHandlePaint.setStyle(Paint.Style.FILL);
		resizeHandlePaint.setColor(0xFFFFD600);
	}

	private float dp(float value) {
		return value * density;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public void setVirtualSize(float width, float height) {
		if (width > 0f) {
			virtualWidth = width;
		}
		if (height > 0f) {
			virtualHeight = height;
		}
		invalidate();
	}

	public void setObjects(List<SceneObject> newObjects) {
		replaceObjects(newObjects);
		if (draggingIndex < 0) {
			fitToScreen();
		}
		invalidate();
	}

	public void updateObjects(List<SceneObject> newObjects) {
		replaceObjects(newObjects);
		invalidate();
	}

	private void replaceObjects(List<SceneObject> newObjects) {
		Sprite draggedSprite = null;
		float dragX = 0f;
		float dragY = 0f;
		if (draggingIndex >= 0 && draggingIndex < objects.size()) {
			SceneObject dragged = objects.get(draggingIndex);
			draggedSprite = dragged.sprite;
			dragX = dragged.x;
			dragY = dragged.y;
		}
		objects.clear();
		if (newObjects != null) {
			objects.addAll(newObjects);
		}
		selectedIndex = -1;
		eyeObjectIndex = -1;
		if (draggedSprite != null) {
			for (int i = 0; i < objects.size(); i++) {
				SceneObject candidate = objects.get(i);
				if (candidate.sprite == draggedSprite) {
					candidate.x = dragX;
					candidate.y = dragY;
					selectedIndex = i;
					draggingIndex = i;
					break;
				}
			}
		}
	}

	private void fitToScreen() {
		float viewW = getWidth();
		float viewH = getHeight();
		if (viewW <= 0f || viewH <= 0f) {
			return;
		}
		scale = Math.min(viewW * 0.85f / virtualWidth, viewH * 0.85f / virtualHeight);
		scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
		panX = 0f;
		panY = 0f;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (oldw == 0 && oldh == 0) {
			fitToScreen();
		}
	}

	private float sceneToScreenX(float sceneX) {
		return getWidth() / 2f + panX + sceneX * scale;
	}

	private float sceneToScreenY(float sceneY) {
		return getHeight() / 2f + panY - sceneY * scale;
	}

	private float halfWidthPx(SceneObject object) {
		return object.widthUnits > 0f ? object.widthUnits * scale / 2f : PLACEHOLDER_HALF_PX;
	}

	private float halfHeightPx(SceneObject object) {
		return object.heightUnits > 0f ? object.heightUnits * scale / 2f : PLACEHOLDER_HALF_PX;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		drawGrid(canvas);
		drawBackground(canvas);
		drawSceneBounds(canvas);
		for (int i = 0; i < objects.size(); i++) {
			drawObject(canvas, objects.get(i), i == selectedIndex);
		}
		if (selectedIndex >= 0 && selectedIndex < objects.size() && !isPlayingMode) {
			drawResizeHandles(canvas, objects.get(selectedIndex));
		}
		if (eyeObjectIndex >= 0 && eyeObjectIndex < objects.size()) {
			drawEye(canvas, objects.get(eyeObjectIndex));
		}
	}

	private void drawGrid(Canvas canvas) {
		float step = GRID_STEP_UNITS * scale;
		if (step < 6f) {
			return;
		}
		float originX = sceneToScreenX(0f);
		float originY = sceneToScreenY(0f);
		float startX = originX % step;
		for (float x = startX; x < getWidth(); x += step) {
			canvas.drawLine(x, 0, x, getHeight(), gridPaint);
		}
		float startY = originY % step;
		for (float y = startY; y < getHeight(); y += step) {
			canvas.drawLine(0, y, getWidth(), y, gridPaint);
		}
		canvas.drawLine(originX, 0, originX, getHeight(), axisPaint);
		canvas.drawLine(0, originY, getWidth(), originY, axisPaint);
	}

	public void setBackgroundBitmap(Bitmap bitmap) {
		this.backgroundBitmap = bitmap;
	}

	private void drawBackground(Canvas canvas) {
		Bitmap bg = backgroundBitmap;
		if (bg == null || bg.isRecycled()) {
			return;
		}
		float halfW = virtualWidth / 2f * scale;
		float halfH = virtualHeight / 2f * scale;
		float cx = sceneToScreenX(0f);
		float cy = sceneToScreenY(0f);
		rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
		canvas.drawBitmap(bg, null, rect, imagePaint);
	}

	private void drawSceneBounds(Canvas canvas) {
		float halfW = virtualWidth / 2f * scale;
		float halfH = virtualHeight / 2f * scale;
		float cx = sceneToScreenX(0f);
		float cy = sceneToScreenY(0f);
		rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
		canvas.drawRect(rect, sceneBoundsPaint);
	}

	private void drawObject(Canvas canvas, SceneObject object, boolean selected) {
		if (object.isBackground && !selected) {
			return;
		}
		float cx = sceneToScreenX(object.x);
		float cy = sceneToScreenY(object.y);
		float halfW = halfWidthPx(object);
		float halfH = halfHeightPx(object);
		rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
		Bitmap bitmap = object.bitmap;
		if (!object.isBackground && bitmap != null && !bitmap.isRecycled()) {
			canvas.drawBitmap(bitmap, null, rect, imagePaint);
		} else if (!object.isBackground) {
			canvas.drawRect(rect, placeholderPaint);
		}
		canvas.drawRect(rect, selected ? selectedPaint : borderPaint);
	}

	private void drawResizeHandles(Canvas canvas, SceneObject object) {
		float cx = sceneToScreenX(object.x);
		float cy = sceneToScreenY(object.y);
		float halfW = Math.max(halfWidthPx(object), dp(18));
		float halfH = Math.max(halfHeightPx(object), dp(18));
		float r = dp(7);
		canvas.drawCircle(cx - halfW, cy, r, resizeHandlePaint);
		canvas.drawCircle(cx + halfW, cy, r, resizeHandlePaint);
		canvas.drawCircle(cx, cy - halfH, r, resizeHandlePaint);
		canvas.drawCircle(cx, cy + halfH, r, resizeHandlePaint);
	}

	private int resizeHandleAt(float screenX, float screenY) {
		if (selectedIndex < 0 || selectedIndex >= objects.size()) return 0;
		SceneObject object = objects.get(selectedIndex);
		float cx = sceneToScreenX(object.x);
		float cy = sceneToScreenY(object.y);
		float halfW = Math.max(halfWidthPx(object), dp(18));
		float halfH = Math.max(halfHeightPx(object), dp(18));
		float hit = dp(18);
		if (Math.abs(screenX - (cx - halfW)) <= hit && Math.abs(screenY - cy) <= hit) return 1;
		if (Math.abs(screenX - (cx + halfW)) <= hit && Math.abs(screenY - cy) <= hit) return 2;
		if (Math.abs(screenX - cx) <= hit && Math.abs(screenY - (cy - halfH)) <= hit) return 3;
		if (Math.abs(screenX - cx) <= hit && Math.abs(screenY - (cy + halfH)) <= hit) return 4;
		return 0;
	}

	private void resizeSelected(float screenX, float screenY) {
		if (resizingIndex < 0 || resizingIndex >= objects.size()) return;
		SceneObject object = objects.get(resizingIndex);
		float cx = sceneToScreenX(object.x);
		float cy = sceneToScreenY(object.y);
		float left = cx - halfWidthPx(object);
		float right = cx + halfWidthPx(object);
		float top = cy - halfHeightPx(object);
		float bottom = cy + halfHeightPx(object);
		float min = dp(20);
		if (resizeHandle == 1) left = Math.min(screenX, right - min);
		if (resizeHandle == 2) right = Math.max(screenX, left + min);
		if (resizeHandle == 3) top = Math.min(screenY, bottom - min);
		if (resizeHandle == 4) bottom = Math.max(screenY, top + min);
		float newCx = (left + right) / 2f;
		float newCy = (top + bottom) / 2f;
		object.x = (newCx - getWidth() / 2f - panX) / scale;
		object.y = -(newCy - getHeight() / 2f - panY) / scale;
		object.widthUnits = Math.max(10f, (right - left) / scale);
		object.heightUnits = Math.max(10f, (bottom - top) / scale);
		invalidate();
	}

	private void drawEye(Canvas canvas, SceneObject object) {
		float cx = sceneToScreenX(object.x);
		float cy = sceneToScreenY(object.y) - Math.max(halfHeightPx(object), PLACEHOLDER_HALF_PX) - dp(26);
		float r = dp(22);
		eyeRect.set(cx - r, cy - r, cx + r, cy + r);
		canvas.drawRoundRect(eyeRect, dp(8), dp(8), eyeBgPaint);
		canvas.drawOval(cx - r * 0.62f, cy - r * 0.38f, cx + r * 0.62f, cy + r * 0.38f, eyeGlyphPaint);
		canvas.drawCircle(cx, cy, r * 0.18f, eyePupilPaint);
	}

	private int objectAt(float screenX, float screenY) {
		for (int i = objects.size() - 1; i >= 0; i--) {
			SceneObject object = objects.get(i);
			float cx = sceneToScreenX(object.x);
			float cy = sceneToScreenY(object.y);
			float halfW = Math.max(halfWidthPx(object), PLACEHOLDER_HALF_PX);
			float halfH = Math.max(halfHeightPx(object), PLACEHOLDER_HALF_PX);
			if (Math.abs(screenX - cx) <= halfW && Math.abs(screenY - cy) <= halfH) {
				return i;
			}
		}
		return -1;
	}

	private void cancelLongPressTimer() {
		longPressHandler.removeCallbacksAndMessages(null);
	}

	@Override
	protected void onDetachedFromWindow() {
		cancelLongPressTimer();
		resizingIndex = -1;
		draggingIndex = -1;
		super.onDetachedFromWindow();
	}

	private void clearEye() {
		if (eyeObjectIndex != -1) {
			eyeObjectIndex = -1;
			invalidate();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (isPlayingMode) {
			return super.onTouchEvent(event);
		}
		scaleDetector.onTouchEvent(event);
		float x = event.getX();
		float y = event.getY();
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				if (eyeObjectIndex >= 0 && eyeObjectIndex < objects.size() && eyeRect.contains(x, y)) {
					Sprite target = objects.get(eyeObjectIndex).sprite;
					clearEye();
					if (listener != null) {
						listener.onObjectPaintRequested(target);
					}
					return true;
				}
				clearEye();
				resizeHandle = resizeHandleAt(x, y);
				if (resizeHandle != 0) {
					resizingIndex = selectedIndex;
					downX = lastX = x;
					downY = lastY = y;
					cancelLongPressTimer();
					return true;
				}
				downX = x;
				downY = y;
				lastX = x;
				lastY = y;
				longPressFired = false;
				draggingIndex = objectAt(x, y);
				panning = draggingIndex < 0;
				if (draggingIndex >= 0) {
					selectedIndex = draggingIndex;
					final int idx = draggingIndex;
					longPressHandler.postDelayed(() -> {
						eyeObjectIndex = idx;
						longPressFired = true;
						invalidate();
					}, LONG_PRESS_MS);
					invalidate();
				}
				return true;
			case MotionEvent.ACTION_POINTER_DOWN:
				cancelLongPressTimer();
				clearEye();
				draggingIndex = -1;
				resizingIndex = -1;
				panning = false;
				lastX = x;
				lastY = y;
				return true;
			case MotionEvent.ACTION_POINTER_UP:
				int upIndex = event.getActionIndex();
				int remainingIndex = upIndex == 0 ? 1 : 0;
				if (remainingIndex < event.getPointerCount()) {
					lastX = event.getX(remainingIndex);
					lastY = event.getY(remainingIndex);
				}
				draggingIndex = -1;
				panning = true;
				return true;
			case MotionEvent.ACTION_MOVE:
				if (scaleDetector.isInProgress()) {
					return true;
				}
				if (Math.hypot(x - downX, y - downY) > TAP_SLOP_PX) {
					cancelLongPressTimer();
					if (longPressFired) {
						longPressFired = false;
						clearEye();
					}
				}
				float dx = x - lastX;
				float dy = y - lastY;
				if (resizingIndex >= 0) {
					resizeSelected(x, y);
				} else if (draggingIndex >= 0 && draggingIndex < objects.size()) {
					SceneObject object = objects.get(draggingIndex);
					object.x += dx / scale;
					object.y -= dy / scale;
					invalidate();
				} else if (panning) {
					panX += dx;
					panY += dy;
					invalidate();
				}
				lastX = x;
				lastY = y;
				return true;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				cancelLongPressTimer();
				if (resizingIndex >= 0 && resizingIndex < objects.size()) {
					SceneObject object = objects.get(resizingIndex);
					if (listener != null) listener.onObjectResized(object.sprite,
							Math.round(object.widthUnits), Math.round(object.heightUnits));
					resizingIndex = -1;
					resizeHandle = 0;
					return true;
				}
				if (longPressFired) {
					longPressFired = false;
					draggingIndex = -1;
					panning = false;
					return true;
				}
				boolean isTap = Math.hypot(x - downX, y - downY) < TAP_SLOP_PX;
				if (draggingIndex >= 0 && draggingIndex < objects.size()) {
					SceneObject object = objects.get(draggingIndex);
					if (isTap) {
						if (listener != null) {
							listener.onObjectTapped(object.sprite);
						}
					} else if (listener != null) {
						listener.onObjectMoved(object.sprite, Math.round(object.x), Math.round(object.y));
					}
				}
				draggingIndex = -1;
				panning = false;
				return true;
			default:
				return super.onTouchEvent(event);
		}
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			cancelLongPressTimer();
			float focusX = detector.getFocusX();
			float focusY = detector.getFocusY();
			float previousScale = scale;
			scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * detector.getScaleFactor()));
			float ratio = scale / previousScale;
			panX = focusX - getWidth() / 2f - (focusX - getWidth() / 2f - panX) * ratio;
			panY = focusY - getHeight() / 2f - (focusY - getHeight() / 2f - panY) * ratio;
			invalidate();
			return true;
		}
	}
}
