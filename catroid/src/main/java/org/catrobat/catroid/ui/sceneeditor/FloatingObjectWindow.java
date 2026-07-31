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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;

public class FloatingObjectWindow extends FrameLayout {

	public interface Callback {
		void onOpenBlocks(Sprite sprite);

		void onOpenLooks(Sprite sprite);

		void onOpenSounds(Sprite sprite);

		void onOpenLayering(Sprite sprite);

		void onOpenInspector(Sprite sprite);

		void onOpenSwipeEditor(Sprite sprite);

		void onClosed(FloatingObjectWindow window);
	}

	private final Sprite sprite;
	private final Callback callback;

	private View body;
	private ImageButton minimizeButton;
	private boolean minimized = false;
	private int expandedHeightPx;

	@SuppressLint("ClickableViewAccessibility")
	public FloatingObjectWindow(Context context, Sprite sprite, Callback callback) {
		super(context);
		this.sprite = sprite;
		this.callback = callback;
		LayoutInflater.from(context).inflate(R.layout.view_floating_object_window, this, true);

		setElevation(dp(8));
		setOutlineProvider(android.view.ViewOutlineProvider.BOUNDS);

		TextView title = findViewById(R.id.floating_window_title);
		TextView info = findViewById(R.id.floating_window_info);
		View header = findViewById(R.id.floating_window_header);
		body = findViewById(R.id.floating_window_body);
		minimizeButton = findViewById(R.id.floating_window_minimize);
		ImageButton close = findViewById(R.id.floating_window_close);
		View resize = findViewById(R.id.floating_window_resize);
		Button addBlocks = findViewById(R.id.floating_window_add_blocks);
		Button btnLooks = findViewById(R.id.floating_window_looks);
		Button btnSounds = findViewById(R.id.floating_window_sounds);
		Button btnLayers = findViewById(R.id.floating_window_layers);
		Button btnInspector = findViewById(R.id.floating_window_inspector);
		Button swipe = findViewById(R.id.floating_window_swipe);

		title.setText(sprite.getName());
		info.setText(context.getString(R.string.scene_editor_window_info, sprite.getScriptList().size()));

		header.setOnTouchListener(new DragListener());
		resize.setOnTouchListener(new ResizeListener());
		minimizeButton.setOnClickListener(v -> toggleMinimize());
		close.setOnClickListener(v -> {
			if (getParent() instanceof ViewGroup) {
				((ViewGroup) getParent()).removeView(this);
			}
			if (callback != null) {
				callback.onClosed(this);
			}
		});
		addBlocks.setOnClickListener(v -> {
			if (callback != null) {
				callback.onOpenBlocks(sprite);
			}
		});
		if (btnLooks != null) {
			btnLooks.setOnClickListener(v -> {
				if (callback != null) {
					callback.onOpenLooks(sprite);
				}
			});
		}
		if (btnSounds != null) {
			btnSounds.setOnClickListener(v -> {
				if (callback != null) {
					callback.onOpenSounds(sprite);
				}
			});
		}
		if (btnLayers != null) {
			btnLayers.setOnClickListener(v -> {
				if (callback != null) {
					callback.onOpenLayering(sprite);
				}
			});
		}
		if (btnInspector != null) {
			btnInspector.setOnClickListener(v -> {
				if (callback != null) {
					callback.onOpenInspector(sprite);
				}
			});
		}
		swipe.setOnClickListener(v -> {
			if (callback != null) {
				callback.onOpenSwipeEditor(sprite);
			}
		});
	}

	public Sprite getSprite() {
		return sprite;
	}

	private void toggleMinimize() {
		minimized = !minimized;
		FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
		if (minimized) {
			expandedHeightPx = params.height;
			body.setVisibility(GONE);
			params.height = dp(40);
			minimizeButton.setImageResource(R.drawable.ic_se_add);
		} else {
			body.setVisibility(VISIBLE);
			params.height = expandedHeightPx > 0 ? expandedHeightPx : dp(220);
			minimizeButton.setImageResource(R.drawable.ic_se_minimize);
		}
		setLayoutParams(params);
	}

	private int dp(float value) {
		return Math.round(value * getResources().getDisplayMetrics().density);
	}

	private class DragListener implements OnTouchListener {
		private float downRawX;
		private float downRawY;
		private int startLeft;
		private int startTop;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					downRawX = event.getRawX();
					downRawY = event.getRawY();
					startLeft = params.leftMargin;
					startTop = params.topMargin;
					bringToFront();
					return true;
				case MotionEvent.ACTION_MOVE:
					params.gravity = Gravity.TOP | Gravity.START;
					params.leftMargin = Math.max(0, (int) (startLeft + (event.getRawX() - downRawX)));
					params.topMargin = Math.max(0, (int) (startTop + (event.getRawY() - downRawY)));
					setLayoutParams(params);
					return true;
				default:
					return false;
			}
		}
	}

	private class ResizeListener implements OnTouchListener {
		private float downRawX;
		private float downRawY;
		private int startWidth;
		private int startHeight;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					downRawX = event.getRawX();
					downRawY = event.getRawY();
					startWidth = getWidth();
					startHeight = getHeight();
					return true;
				case MotionEvent.ACTION_MOVE:
					params.width = Math.max(dp(160), (int) (startWidth + (event.getRawX() - downRawX)));
					params.height = Math.max(dp(120), (int) (startHeight + (event.getRawY() - downRawY)));
					if (!minimized) {
						expandedHeightPx = params.height;
					}
					setLayoutParams(params);
					return true;
				default:
					return false;
			}
		}
	}
}
