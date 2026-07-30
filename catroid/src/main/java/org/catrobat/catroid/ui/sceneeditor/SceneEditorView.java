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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.catrobat.catroid.content.Sprite;

import java.util.ArrayList;
import java.util.List;

/**
 * Canvas for the UI 2.0 scene editor. Draws the whole scene (grid + every sprite at its scene
 * position) with infinite pan/zoom, and lets the user drag objects to reposition them. Scene
 * coordinates are Catroid user-interface units (origin centre, Y up). Screen mapping:
 * screenX = width/2 + panX + sceneX * scale, screenY = height/2 + panY - sceneY * scale.
 */
public class SceneEditorView extends View {

	public static class SceneObject {
		public final Sprite sprite;
		public final Bitmap bitmap;
		public float x;
		public float y;
		public final float widthUnits;
		public final float heightUnits;

		public SceneObject(Sprite sprite, Bitmap bitmap, float x, float y, float widthUnits, float heightUnits) {
			this.sprite = sprite;
			this.bitmap = bitmap;
			this.x = x;
			this.y = y;
			this.widthUnits = widthUnits;
			this.heightUnits = heightUnits;
		}
	}

	public interface Listener {
		void onObjectMoved(Sprite sprite, int x, int y);

		void onObjectTapped(Sprite sprite);
	}

	private static final float MIN_SCALE = 0.05f;
	private static final float MAX_SCALE = 20f;
	private static final float GRID_STEP_UNITS = 50f;
	private static final float TAP_SLOP_PX = 12f;
	private static final float PLACEHOLDER_HALF_PX = 40f;

	private final List<SceneObject> objects = new ArrayList<>();
	private float virtualWidth = 480f;
	private float virtualHeight = 800f;

	private float scale = 1f;
	private float panX = 0f;
	private float panY = 0f;

	private int selectedIndex = -1;
	private int draggingIndex = -1;
	private boolean panning = false;
	private float lastX;
	private float lastY;
	private float downX;
	private float downY;

	private final ScaleGestureDetector scaleDetector;

	private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint sceneBoundsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final RectF rect = new RectF();

	private Listener listener;

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
		borderPaint.setColor(0xFF38BDF8);

		selectedPaint.setStyle(Paint.Style.STROKE);
		selectedPaint.setStrokeWidth(4.5f);
		selectedPaint.setColor(0xFFFFD600);

		placeholderPaint.setStyle(Paint.Style.FILL);
		placeholderPaint.setColor(0x552563EB);
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
		objects.clear();
		if (newObjects != null) {
			objects.addAll(newObjects);
		}
		selectedIndex = -1;
		fitToScreen();
		invalidate();
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

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		drawGrid(canvas);
		drawSceneBounds(canvas);
		for (int i = 0; i < objects.size(); i++) {
			drawObject(canvas, objects.get(i), i == selectedIndex);
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

	private void drawSceneBounds(Canvas canvas) {
		float halfW = virtualWidth / 2f * scale;
		float halfH = virtualHeight / 2f * scale;
		float cx = sceneToScreenX(0f);
		float cy = sceneToScreenY(0f);
		rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
		canvas.drawRect(rect, sceneBoundsPaint);
	}

	private void drawObject(Canvas canvas, SceneObject object, boolean selected) {
		float cx = sceneToScreenX(object.x);
		float cy = sceneToScreenY(object.y);
		float halfW;
		float halfH;
		if (object.bitmap != null) {
			halfW = object.widthUnits * scale / 2f;
			halfH = object.heightUnits * scale / 2f;
			rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
			canvas.drawBitmap(object.bitmap, null, rect, imagePaint);
		} else {
			halfW = PLACEHOLDER_HALF_PX;
			halfH = PLACEHOLDER_HALF_PX;
			rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
			canvas.drawRect(rect, placeholderPaint);
		}
		canvas.drawRect(rect, selected ? selectedPaint : borderPaint);
	}

	private int objectAt(float screenX, float screenY) {
		for (int i = objects.size() - 1; i >= 0; i--) {
			SceneObject object = objects.get(i);
			float cx = sceneToScreenX(object.x);
			float cy = sceneToScreenY(object.y);
			float halfW = object.bitmap != null ? object.widthUnits * scale / 2f : PLACEHOLDER_HALF_PX;
			float halfH = object.bitmap != null ? object.heightUnits * scale / 2f : PLACEHOLDER_HALF_PX;
			halfW = Math.max(halfW, PLACEHOLDER_HALF_PX);
			halfH = Math.max(halfH, PLACEHOLDER_HALF_PX);
			if (Math.abs(screenX - cx) <= halfW && Math.abs(screenY - cy) <= halfH) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		scaleDetector.onTouchEvent(event);
		float x = event.getX();
		float y = event.getY();
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				downX = x;
				downY = y;
				lastX = x;
				lastY = y;
				draggingIndex = objectAt(x, y);
				panning = draggingIndex < 0;
				if (draggingIndex >= 0) {
					selectedIndex = draggingIndex;
					invalidate();
				}
				return true;
			case MotionEvent.ACTION_MOVE:
				if (scaleDetector.isInProgress()) {
					return true;
				}
				float dx = x - lastX;
				float dy = y - lastY;
				if (draggingIndex >= 0 && draggingIndex < objects.size()) {
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
