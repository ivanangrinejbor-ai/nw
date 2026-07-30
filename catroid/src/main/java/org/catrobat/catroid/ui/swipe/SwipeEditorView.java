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
package org.catrobat.catroid.ui.swipe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Preview canvas for the Swipe Editor. The parent sprite is drawn at the centre; each attached
 * child sprite is drawn at its offset (in Catroid user-interface dimension units) and can be
 * dragged to reposition it. Offsets are what the runtime {@code SwipeController} applies, so the
 * preview assumes 100% sprite scale (1 image pixel maps to 1 UI unit).
 */
public class SwipeEditorView extends View {

	public static class Attachment {
		public final String childName;
		public final Bitmap bitmap;
		public float offsetX;
		public float offsetY;

		public Attachment(String childName, Bitmap bitmap, float offsetX, float offsetY) {
			this.childName = childName;
			this.bitmap = bitmap;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}
	}

	public interface OnChangeListener {
		void onChanged();
	}

	private static final int MAX_BITMAP_DIM = 2048;

	private float virtualWidth = 480f;
	private float virtualHeight = 800f;
	private float previewScale = 1f;

	private Bitmap parentBitmap;
	private final List<Attachment> attachments = new ArrayList<>();
	private int selectedIndex = -1;

	private float lastTouchX;
	private float lastTouchY;
	private boolean dragging = false;

	private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint parentBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint childBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint selectedBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final RectF rect = new RectF();

	private OnChangeListener changeListener;

	public SwipeEditorView(Context context) {
		super(context);
		init();
	}

	public SwipeEditorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SwipeEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		imagePaint.setFilterBitmap(true);

		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setStrokeWidth(1.5f);
		gridPaint.setColor(0x2294A3B8);

		parentBorderPaint.setStyle(Paint.Style.STROKE);
		parentBorderPaint.setStrokeWidth(3f);
		parentBorderPaint.setColor(0xFF00E676);

		childBorderPaint.setStyle(Paint.Style.STROKE);
		childBorderPaint.setStrokeWidth(3f);
		childBorderPaint.setColor(0xFF38BDF8);

		selectedBorderPaint.setStyle(Paint.Style.STROKE);
		selectedBorderPaint.setStrokeWidth(4.5f);
		selectedBorderPaint.setColor(0xFFFFD600);

		placeholderPaint.setStyle(Paint.Style.FILL);
		placeholderPaint.setColor(0x552563EB);
	}

	public void setOnChangeListener(OnChangeListener listener) {
		this.changeListener = listener;
	}

	public void setVirtualSize(float width, float height) {
		if (width > 0f) {
			virtualWidth = width;
		}
		if (height > 0f) {
			virtualHeight = height;
		}
		computeScale();
		invalidate();
	}

	public void setParentImage(String path) {
		parentBitmap = loadBitmap(path);
		invalidate();
	}

	public void setAttachments(List<Attachment> initial) {
		attachments.clear();
		if (initial != null) {
			attachments.addAll(initial);
		}
		selectedIndex = attachments.isEmpty() ? -1 : 0;
		invalidate();
	}

	public void addAttachment(String childName, String bitmapPath) {
		Bitmap bitmap = loadBitmap(bitmapPath);
		float defaultOffsetX = parentBitmap != null ? parentBitmap.getWidth() / 2f + 40f : 80f;
		attachments.add(new Attachment(childName, bitmap, defaultOffsetX, 0f));
		selectedIndex = attachments.size() - 1;
		notifyChange();
		invalidate();
	}

	public List<Attachment> getAttachments() {
		return new ArrayList<>(attachments);
	}

	public void loadExistingAttachment(String childName, String bitmapPath, float offsetX, float offsetY) {
		attachments.add(new Attachment(childName, loadBitmap(bitmapPath), offsetX, offsetY));
		if (selectedIndex < 0) {
			selectedIndex = 0;
		}
		invalidate();
	}

	public void deleteSelected() {
		if (selectedIndex >= 0 && selectedIndex < attachments.size()) {
			attachments.remove(selectedIndex);
			selectedIndex = attachments.isEmpty() ? -1 : Math.min(selectedIndex, attachments.size() - 1);
			notifyChange();
			invalidate();
		}
	}

	private void notifyChange() {
		if (changeListener != null) {
			changeListener.onChanged();
		}
	}

	private Bitmap loadBitmap(String path) {
		if (path == null) {
			return null;
		}
		BitmapFactory.Options bounds = new BitmapFactory.Options();
		bounds.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, bounds);
		int sample = 1;
		if (bounds.outWidth > 0 && bounds.outHeight > 0) {
			while (Math.max(bounds.outWidth, bounds.outHeight) / sample > MAX_BITMAP_DIM) {
				sample *= 2;
			}
		}
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = sample;
		return BitmapFactory.decodeFile(path, opts);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		computeScale();
	}

	private void computeScale() {
		float viewW = getWidth();
		float viewH = getHeight();
		if (viewW <= 0f || viewH <= 0f) {
			return;
		}
		previewScale = Math.min(viewW * 0.9f / virtualWidth, viewH * 0.9f / virtualHeight);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		float cx = getWidth() / 2f;
		float cy = getHeight() / 2f;

		canvas.drawLine(cx, 0, cx, getHeight(), gridPaint);
		canvas.drawLine(0, cy, getWidth(), cy, gridPaint);

		drawBitmapCentered(canvas, parentBitmap, cx, cy, parentBorderPaint, false);

		for (int i = 0; i < attachments.size(); i++) {
			Attachment attachment = attachments.get(i);
			float drawX = cx + attachment.offsetX * previewScale;
			float drawY = cy - attachment.offsetY * previewScale;
			Paint border = (i == selectedIndex) ? selectedBorderPaint : childBorderPaint;
			drawBitmapCentered(canvas, attachment.bitmap, drawX, drawY, border, true);
		}
	}

	private void drawBitmapCentered(Canvas canvas, Bitmap bitmap, float centerX, float centerY,
			Paint border, boolean placeholderIfNull) {
		float halfW;
		float halfH;
		if (bitmap != null) {
			halfW = bitmap.getWidth() * previewScale / 2f;
			halfH = bitmap.getHeight() * previewScale / 2f;
			rect.set(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH);
			canvas.drawBitmap(bitmap, null, rect, imagePaint);
		} else {
			halfW = 40f;
			halfH = 40f;
			rect.set(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH);
			if (placeholderIfNull) {
				canvas.drawRect(rect, placeholderPaint);
			}
		}
		canvas.drawRect(rect, border);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				lastTouchX = x;
				lastTouchY = y;
				int index = attachmentAt(x, y);
				if (index >= 0) {
					selectedIndex = index;
					dragging = true;
				}
				invalidate();
				return true;
			case MotionEvent.ACTION_MOVE:
				if (dragging && selectedIndex >= 0 && selectedIndex < attachments.size()
						&& previewScale > 0.0001f) {
					Attachment attachment = attachments.get(selectedIndex);
					attachment.offsetX += (x - lastTouchX) / previewScale;
					attachment.offsetY -= (y - lastTouchY) / previewScale;
					lastTouchX = x;
					lastTouchY = y;
					notifyChange();
					invalidate();
				}
				return true;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				dragging = false;
				return true;
			default:
				return super.onTouchEvent(event);
		}
	}

	private int attachmentAt(float x, float y) {
		float cx = getWidth() / 2f;
		float cy = getHeight() / 2f;
		for (int i = attachments.size() - 1; i >= 0; i--) {
			Attachment attachment = attachments.get(i);
			float drawX = cx + attachment.offsetX * previewScale;
			float drawY = cy - attachment.offsetY * previewScale;
			float halfW = attachment.bitmap != null ? attachment.bitmap.getWidth() * previewScale / 2f : 40f;
			float halfH = attachment.bitmap != null ? attachment.bitmap.getHeight() * previewScale / 2f : 40f;
			halfW = Math.max(halfW, 40f);
			halfH = Math.max(halfH, 40f);
			if (Math.abs(x - drawX) <= halfW && Math.abs(y - drawY) <= halfH) {
				return i;
			}
		}
		return -1;
	}
}
