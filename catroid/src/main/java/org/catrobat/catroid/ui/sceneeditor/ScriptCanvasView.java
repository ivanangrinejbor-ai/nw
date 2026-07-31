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

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.ScriptNote;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.CompositeBrick;
import org.catrobat.catroid.content.bricks.FormulaBrick;
import org.catrobat.catroid.content.bricks.NoteBrick;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.VisualPlacementBrick;
import org.catrobat.catroid.ui.fragment.FormulaEditorFragment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public class ScriptCanvasView extends FrameLayout {

	private static final float MIN_SCALE = 0.2f;
	private static final float MAX_SCALE = 3f;
	private static final float TOUCH_SLOP_PX = 10f;

	private final FrameLayout world;
	private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final ScaleGestureDetector scaleDetector;

	private float scale = 1f;
	private float panX = 0f;
	private float panY = 0f;
	private float density = 1f;

	private boolean panning = false;
	private float lastTouchX;
	private float lastTouchY;
	private float downX;
	private float downY;

	private Sprite sprite;
	private final Handler longPressHandler = new Handler(Looper.getMainLooper());
	private boolean longPressScheduled;
	private LinearLayout blockBadge;
	private View blockGhost;
	private Brick draggingBrick;

	// ─── Undo / Redo ──────────────────────────────────────────────────────────
	private static final int MAX_UNDO_STEPS = 30;
	private final Deque<List<Script>> undoStack = new ArrayDeque<>();
	private final Deque<List<Script>> redoStack = new ArrayDeque<>();
	public interface UndoRedoListener { void onUndoRedoChanged(); }
	private UndoRedoListener undoRedoListener;
	public void setUndoRedoListener(UndoRedoListener l) { this.undoRedoListener = l; }
	private void notifyUndoRedo() { if (undoRedoListener != null) undoRedoListener.onUndoRedoChanged(); }

	public ScriptCanvasView(Context context) {
		super(context);
		world = new FrameLayout(context);
		scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		init();
	}

	private void init() {
		density = getResources().getDisplayMetrics().density;
		setWillNotDraw(false);
		setClipChildren(false);
		setClipToPadding(false);
		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setStrokeWidth(1f);
		gridPaint.setColor(0x1AB0BEC5);

		world.setClipChildren(false);
		world.setClipToPadding(false);
		world.setPivotX(0f);
		world.setPivotY(0f);
		addView(world, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}

	private int dp(float value) {
		return Math.round(value * density);
	}

	public void setSprite(Sprite sprite) {
		this.sprite = sprite;
		buildStacks();
		addNoteViews();
	}

	private void buildStacks() {
		world.removeAllViews();
		if (sprite == null) {
			return;
		}
		List<Script> scripts = sprite.getScriptList();
		boolean allZero = true;
		for (Script script : scripts) {
			if (script.getPosX() != 0f || script.getPosY() != 0f) {
				allZero = false;
				break;
			}
		}
		for (int i = 0; i < scripts.size(); i++) {
			Script script = scripts.get(i);
			if (allZero) {
				script.setPosX(dp(40) + (i % 3) * dp(380));
				script.setPosY(dp(40) + (i / 3) * dp(360));
			}
			world.addView(buildStack(script));
		}
	}

	private View buildStack(Script script) {
		LinearLayout stack = new LinearLayout(getContext());
		stack.setOrientation(LinearLayout.VERTICAL);
		stack.setClipChildren(false);
		stack.setClipToPadding(false);
		// BrickLayout calculates formula-field widths from the width supplied by
		// its parent. WRAP_CONTENT here gives it an almost zero/ambiguous width,
		// which clips the brick and makes its fields impossible to use.
		final int stackWidth = Math.max(dp(420), getResources().getDisplayMetrics().widthPixels - dp(32));
		stack.setMinimumWidth(stackWidth);

		List<Brick> flat = new ArrayList<>();
		flat.add(script.getScriptBrick());
		for (Brick brick : script.getBrickList()) {
			brick.addToFlatList(flat);
		}
		for (Brick brick : flat) {
			View brickView;
			try {
				brickView = brick.getView(getContext());
			} catch (Exception e) {
				TextView fallback = new TextView(getContext());
				fallback.setText("? " + brick.getClass().getSimpleName());
				fallback.setTextColor(0xFFF8FAFC);
				fallback.setPadding(dp(8), dp(6), dp(8), dp(6));
				fallback.setBackgroundColor(0xFF334155);
				brickView = fallback;
			}
			brickView.setTag(brick);
			wireFormulaFields(brick, brickView);
			final Brick targetBrick = brick;
			final Script targetScript = script;
			final View targetStackView = stack;

			brickView.setOnTouchListener(new BrickTouchDragListener(targetScript, targetBrick, brickView, targetStackView));
			LinearLayout.LayoutParams brickParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			stack.addView(brickView, brickParams);
		}

		LayoutParams params = new LayoutParams(stackWidth, LayoutParams.WRAP_CONTENT);
		params.leftMargin = Math.round(script.getPosX());
		params.topMargin = Math.round(script.getPosY());
		stack.setLayoutParams(params);
		stack.setTag(script);
		return stack;
	}

	public void rebuild() {
		world.removeAllViews();
		buildStacks();
		addNoteViews();
		applyTransform();
		autoSave();
	}

	private void autoSave() {
		if (sprite == null) return;
		new Thread(() -> {
			try {
				org.catrobat.catroid.io.XstreamSerializer.getInstance()
						.saveProject(org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject());
			} catch (Exception ignored) {
			}
		}, "script-canvas-autosave").start();
	}

	// ─── Undo / Redo implementation ───────────────────────────────────────────

	private List<Script> snapshotScripts() {
		List<Script> snap = new ArrayList<>();
		if (sprite == null) return snap;
		for (Script s : sprite.getScriptList()) {
			try { snap.add(s.clone()); } catch (CloneNotSupportedException ignored) {}
		}
		return snap;
	}

	public void snapshot() {
		List<Script> snap = snapshotScripts();
		undoStack.push(snap);
		if (undoStack.size() > MAX_UNDO_STEPS) undoStack.pollLast();
		redoStack.clear();
		notifyUndoRedo();
	}

	private void restoreFromSnapshot(List<Script> scripts) {
		if (sprite == null) return;
		sprite.getScriptList().clear();
		for (Script s : scripts) {
			sprite.addScript(s);
			s.setParents();
		}
	}

	public void undo() {
		if (undoStack.isEmpty()) return;
		List<Script> current = snapshotScripts();
		redoStack.push(current);
		restoreFromSnapshot(undoStack.pop());
		rebuild();
		autoSave();
		notifyUndoRedo();
	}

	public void redo() {
		if (redoStack.isEmpty()) return;
		List<Script> current = snapshotScripts();
		undoStack.push(current);
		restoreFromSnapshot(redoStack.pop());
		rebuild();
		autoSave();
		notifyUndoRedo();
	}

	public boolean canUndo() { return !undoStack.isEmpty(); }
	public boolean canRedo() { return !redoStack.isEmpty(); }

	public float getPanX() { return panX; }
	public float getPanY() { return panY; }
	public float getScale() { return scale; }

	public void restorePanAndScale(float px, float py, float sc) {
		panX = px;
		panY = py;
		scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, sc));
		applyTransform();
	}

	public boolean dropPrototypeAtScreen(Brick prototype, float rawX, float rawY) {
		if (sprite == null) {
			return false;
		}
		int[] location = new int[2];
		getLocationOnScreen(location);
		float localX = rawX - location[0];
		float localY = rawY - location[1];
		if (localX < 0 || localY < 0 || localX > getWidth() || localY > getHeight()) {
			return false;
		}
		float worldX = (localX - panX) / scale;
		float worldY = (localY - panY) / scale;
		Brick clone;
		try {
			clone = prototype.clone();
		} catch (CloneNotSupportedException e) {
			return false;
		}
		snapshot();
		if (clone instanceof ScriptBrick) {
			Script newScript = ((ScriptBrick) clone).getScript();
			newScript.setPosX(worldX);
			newScript.setPosY(worldY);
			sprite.addScript(newScript);
			newScript.setParents();
			rebuild();
			return true;
		}
		View stackView = findStackViewAtWorld(worldX, worldY);
		if (stackView != null && stackView.getTag() instanceof Script) {
			Script script = (Script) stackView.getTag();
			insertBrickIntoScript(script, findBrickInStack(stackView, worldY), clone);
			script.setParents();
		} else {
			StartScript script = new StartScript();
			script.setPosX(worldX);
			script.setPosY(worldY);
			script.addBrick(clone);
			sprite.addScript(script);
			script.setParents();
		}
		rebuild();
		return true;
	}

	private void insertBrickIntoScript(Script script, Brick targetBrick, Brick clone) {
		if (targetBrick == null || targetBrick instanceof ScriptBrick) {
			script.addBrick(0, clone);
			return;
		}
		List<Brick> list = findListContainingBrick(script, targetBrick);
		if (list == null) {
			script.addBrick(clone);
			return;
		}
		int index = list.indexOf(targetBrick);
		list.add(index < 0 ? list.size() : index + 1, clone);
	}

	private List<Brick> findListContainingBrick(Script script, Brick target) {
		if (script.getBrickList().contains(target)) {
			return script.getBrickList();
		}
		for (Brick brick : script.getBrickList()) {
			List<Brick> found = findListContainingBrickRecursive(brick, target);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private List<Brick> findListContainingBrickRecursive(Brick brick, Brick target) {
		if (brick instanceof CompositeBrick) {
			CompositeBrick composite = (CompositeBrick) brick;
			if (composite.getNestedBricks().contains(target)) {
				return composite.getNestedBricks();
			}
			for (Brick child : composite.getNestedBricks()) {
				List<Brick> found = findListContainingBrickRecursive(child, target);
				if (found != null) {
					return found;
				}
			}
			if (composite.hasSecondaryList()) {
				if (composite.getSecondaryNestedBricks().contains(target)) {
					return composite.getSecondaryNestedBricks();
				}
				for (Brick child : composite.getSecondaryNestedBricks()) {
					List<Brick> found = findListContainingBrickRecursive(child, target);
					if (found != null) {
						return found;
					}
				}
			}
		}
		return null;
	}

	private View findStackViewAtWorld(float worldX, float worldY) {
		for (int i = world.getChildCount() - 1; i >= 0; i--) {
			View child = world.getChildAt(i);
			if (child.getTag() instanceof Script) {
				float left = child.getLeft();
				float top = child.getTop();
				if (worldX >= left && worldX <= left + child.getWidth()
						&& worldY >= top && worldY <= top + child.getHeight()) {
					return child;
				}
			}
		}
		return null;
	}

	private Brick findBrickInStack(View stackView, float worldY) {
		if (!(stackView instanceof LinearLayout)) {
			return null;
		}
		float localY = worldY - stackView.getTop();
		LinearLayout stack = (LinearLayout) stackView;
		Brick best = null;
		for (int i = 0; i < stack.getChildCount(); i++) {
			View child = stack.getChildAt(i);
			if (child.getTag() instanceof Brick && localY >= child.getTop()) {
				best = (Brick) child.getTag();
			}
		}
		return best;
	}

	private void deleteBrick(Brick brick) {
		removeBlockBadge();
		snapshot();
		if (brick instanceof ScriptBrick) {
			sprite.getScriptList().remove(((ScriptBrick) brick).getScript());
		} else {
			for (Script script : sprite.getScriptList()) {
				if (removeBrickRecursive(script.getBrickList(), brick)) {
					break;
				}
			}
			for (Script script : sprite.getScriptList()) {
				script.setParents();
			}
		}
		rebuild();
	}

	private void showBlockBadge(Brick brick, View brickView, View stackView) {
		removeBlockBadge();
		LinearLayout badge = new LinearLayout(getContext());
		badge.setOrientation(LinearLayout.HORIZONTAL);
		badge.setBackgroundColor(0xF20F172A);
		badge.setPadding(dp(2), dp(2), dp(2), dp(2));
		if (!(brick instanceof ScriptBrick)) {
			ImageButton move = new ImageButton(getContext());
			move.setImageResource(R.drawable.ic_se_move);
			move.setBackgroundColor(0x00000000);
			move.setPadding(dp(8), dp(8), dp(8), dp(8));
			move.setOnTouchListener(new BlockDragListener(brick));
			badge.addView(move);
		}
		ImageButton menu = new ImageButton(getContext());
		menu.setImageResource(android.R.drawable.ic_menu_more);
		menu.setContentDescription("Меню блока");
		menu.setBackgroundColor(0x00000000);
		menu.setPadding(dp(8), dp(8), dp(8), dp(8));
		menu.setOnClickListener(v -> showBlockContextMenu(brick, brickView, stackView));
		badge.addView(menu);
		ImageButton trash = new ImageButton(getContext());
		trash.setImageResource(R.drawable.ic_se_trash);
		trash.setBackgroundColor(0x00000000);
		trash.setPadding(dp(8), dp(8), dp(8), dp(8));
		trash.setOnClickListener(v -> deleteBrick(brick));
		badge.addView(trash);
		int worldLeft = stackView.getLeft() + brickView.getLeft();
		int worldTop = stackView.getTop() + brickView.getTop();
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.leftMargin = worldLeft;
		params.topMargin = Math.max(0, worldTop - dp(44));
		blockBadge = badge;
		world.addView(badge, params);
	}

	private void removeBlockBadge() {
		if (blockBadge != null) {
			world.removeView(blockBadge);
			blockBadge = null;
		}
	}

	private class BlockDragListener implements OnTouchListener {
		private final Brick brick;
		private boolean dragging;

		BlockDragListener(Brick brick) {
			this.brick = brick;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					dragging = false;
					return true;
				case MotionEvent.ACTION_MOVE:
					if (!dragging) {
						dragging = true;
						startBlockDrag(brick);
					}
					moveBlockGhost(event.getRawX(), event.getRawY());
					return true;
				case MotionEvent.ACTION_UP:
					if (dragging) {
						dropBlockGhost(event.getRawX(), event.getRawY());
					}
					return true;
				case MotionEvent.ACTION_CANCEL:
					removeBlockGhost();
					return true;
				default:
					return false;
			}
		}
	}

	private void startBlockDrag(Brick brick) {
		draggingBrick = brick;
		View ghost = brick.getPrototypeView(getContext());
		ghost.setAlpha(0.85f);
		blockGhost = ghost;
		addView(ghost, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	private void moveBlockGhost(float rawX, float rawY) {
		if (blockGhost == null) {
			return;
		}
		int[] location = new int[2];
		getLocationOnScreen(location);
		LayoutParams params = (LayoutParams) blockGhost.getLayoutParams();
		params.leftMargin = Math.round(rawX - location[0] - dp(20));
		params.topMargin = Math.round(rawY - location[1] - dp(12));
		blockGhost.setLayoutParams(params);
	}

	private void removeBlockGhost() {
		removeBlockBadge();
		if (blockGhost != null) {
			removeView(blockGhost);
			blockGhost = null;
		}
		draggingBrick = null;
	}

	private void dropBlockGhost(float rawX, float rawY) {
		Brick moving = draggingBrick;
		removeBlockGhost();
		if (moving == null || sprite == null) {
			return;
		}
		int[] location = new int[2];
		getLocationOnScreen(location);
		float localX = rawX - location[0];
		float localY = rawY - location[1];
		if (localX < 0 || localY < 0 || localX > getWidth() || localY > getHeight()) {
			return;
		}
		float worldX = (localX - panX) / scale;
		float worldY = (localY - panY) / scale;
		View stackView = findStackViewAtWorld(worldX, worldY);
		Script script = stackView != null && stackView.getTag() instanceof Script
				? (Script) stackView.getTag() : null;
		Brick targetBrick = stackView != null ? findBrickInStack(stackView, worldY) : null;
		if (targetBrick == moving) {
			return;
		}
		snapshot();
		for (Script s : sprite.getScriptList()) {
			if (removeBrickRecursive(s.getBrickList(), moving)) {
				break;
			}
		}
		if (script == null) {
			StartScript newScript = new StartScript();
			newScript.setPosX(worldX);
			newScript.setPosY(worldY);
			newScript.addBrick(moving);
			sprite.addScript(newScript);
			newScript.setParents();
		} else {
			insertBrickIntoScript(script, targetBrick, moving);
			script.setParents();
		}
		rebuild();
	}

	private boolean removeBrickRecursive(List<Brick> list, Brick target) {
		if (list.remove(target)) {
			return true;
		}
		for (Brick brick : list) {
			if (brick instanceof CompositeBrick) {
				CompositeBrick composite = (CompositeBrick) brick;
				if (removeBrickRecursive(composite.getNestedBricks(), target)) {
					return true;
				}
				if (composite.hasSecondaryList()
						&& removeBrickRecursive(composite.getSecondaryNestedBricks(), target)) {
					return true;
				}
			}
		}
		return false;
	}

	private void wireFormulaFields(Brick brick, View brickView) {
		if (!(brick instanceof FormulaBrick)) {
			return;
		}
		final FormulaBrick formulaBrick = (FormulaBrick) brick;
		for (Map.Entry<Brick.FormulaField, Integer> entry : formulaBrick.brickFieldToTextViewIdMap.entrySet()) {
			final Brick.FormulaField field = entry.getKey();
			View fieldView = brickView.findViewById(entry.getValue());
			if (fieldView != null) {
				fieldView.setOnClickListener(v -> {
					Intent intent = new Intent(getContext(), org.catrobat.catroid.ui.formulaeditor.FormulaEditor2Activity.class);
					org.catrobat.catroid.formulaeditor.Formula existingFormula = formulaBrick.getFormulaWithBrickField(field);
					if (existingFormula != null) {
						try {
							intent.putExtra(org.catrobat.catroid.ui.formulaeditor.FormulaEditor2Activity.EXTRA_FORMULA_STRING, existingFormula.interpretString(null));
						} catch (org.catrobat.catroid.formulaeditor.InterpretationException e) {
							intent.putExtra(org.catrobat.catroid.ui.formulaeditor.FormulaEditor2Activity.EXTRA_FORMULA_STRING, existingFormula.getTrimmedFormulaString(getContext()));
						}
					}
					if (getContext() instanceof ScriptCanvasActivity) {
						((ScriptCanvasActivity) getContext()).setActiveEditFormula(formulaBrick, field);
						((ScriptCanvasActivity) getContext()).startActivityForResult(intent, 8899);
					} else {
						FormulaEditorFragment.showFragment(getContext(), formulaBrick, field);
					}
				});
			}
		}
	}

	private class BrickTouchDragListener implements OnTouchListener {
		private final Script script;
		private final Brick brick;
		private final View brickView;
		private final View stackView;
		private float downRawX, downRawY;
		private float startPosX, startPosY;
		private boolean isLongPressed = false;
		private boolean isEventHeader = false;

		private final Handler handler = new Handler(Looper.getMainLooper());
		private final Runnable longPressRunnable = new Runnable() {
			@Override
			public void run() {
				isLongPressed = true;
				if (brick instanceof ScriptBrick) {
					isEventHeader = true;
					snapshot();
					brickView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
				} else {
					isEventHeader = false;
					brickView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
					startBlockDrag(brick);
				}
			}
		};

		BrickTouchDragListener(Script script, Brick brick, View brickView, View stackView) {
			this.script = script;
			this.brick = brick;
			this.brickView = brickView;
			this.stackView = stackView;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					downRawX = event.getRawX();
					downRawY = event.getRawY();
					startPosX = script.getPosX();
					startPosY = script.getPosY();
					isLongPressed = false;
					handler.postDelayed(longPressRunnable, 350);
					return true;

				case MotionEvent.ACTION_MOVE:
					float dx = event.getRawX() - downRawX;
					float dy = event.getRawY() - downRawY;
					if (!isLongPressed && Math.hypot(dx, dy) > dp(6)) {
						handler.removeCallbacks(longPressRunnable);
					}
					if (isLongPressed) {
					if (isEventHeader) {
						float newX = startPosX + dx / scale;
						float newY = startPosY + dy / scale;
						script.setPosX(newX);
						script.setPosY(newY);
						android.widget.FrameLayout.LayoutParams params =
								(android.widget.FrameLayout.LayoutParams) stackView.getLayoutParams();
						if (params != null) {
							params.leftMargin = Math.round(newX);
							params.topMargin = Math.round(newY);
							stackView.setLayoutParams(params);
						}
					} else {
							moveBlockGhost(event.getRawX(), event.getRawY());
						}
					}
					return true;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					handler.removeCallbacks(longPressRunnable);
					if (isLongPressed) {
						if (isEventHeader) {
							rebuild();
						} else {
							dropBlockGhost(event.getRawX(), event.getRawY());
						}
					} else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
						if (blockGhost == null) {
							showBlockBadge(brick, brickView, stackView);
						}
					}
					return true;

				default:
					return false;
			}
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		return event.getPointerCount() > 1 || scaleDetector.isInProgress();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		scaleDetector.onTouchEvent(event);
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				removeBlockBadge();
				downX = event.getX();
				downY = event.getY();
				lastTouchX = event.getX();
				lastTouchY = event.getY();
				panning = true;
				scheduleAddNoteLongPress(event.getX(), event.getY());
				return true;
			case MotionEvent.ACTION_MOVE:
				if (scaleDetector.isInProgress() || event.getPointerCount() > 1) {
					cancelAddNoteLongPress();
					return true;
				}
				if (Math.hypot(event.getX() - downX, event.getY() - downY) > TOUCH_SLOP_PX) {
					cancelAddNoteLongPress();
				}
				if (panning) {
					panX += event.getX() - lastTouchX;
					panY += event.getY() - lastTouchY;
					lastTouchX = event.getX();
					lastTouchY = event.getY();
					applyTransform();
				}
				return true;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				cancelAddNoteLongPress();
				panning = false;
				return true;
			default:
				return true;
		}
	}

	public void zoomIn() {
		scale = Math.min(MAX_SCALE, scale * 1.25f);
		applyTransform();
	}

	public void zoomOut() {
		scale = Math.max(MIN_SCALE, scale / 1.25f);
		applyTransform();
	}

	public void resetZoom() {
		scale = 1f;
		panX = 0f;
		panY = 0f;
		applyTransform();
	}

	private void applyTransform() {
		world.setScaleX(scale);
		world.setScaleY(scale);
		world.setTranslationX(panX);
		world.setTranslationY(panY);
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		float step = dp(40) * scale;
		if (step < 8f) {
			return;
		}
		float startX = panX % step;
		for (float x = startX; x < getWidth(); x += step) {
			canvas.drawLine(x, 0, x, getHeight(), gridPaint);
		}
		float startY = panY % step;
		for (float y = startY; y < getHeight(); y += step) {
			canvas.drawLine(0, y, getWidth(), y, gridPaint);
		}
	}

	private static Script copiedScriptClipboard = null;
	private static Brick copiedBrickClipboard = null;

	private void showBlockContextMenu(Brick brick, View brickView, View stackView) {
		if (brick == null) return;
		String brickName = brick.getClass().getSimpleName();
		List<String> items = new ArrayList<>();
		List<Runnable> actions = new ArrayList<>();

		items.add("Справка по блоку");
		actions.add(() -> showBrickHelp(brick));

		if (brick instanceof FormulaBrick && ((FormulaBrick) brick).hasEditableFormulaField()) {
			items.add("Редактировать формулу 2.0");
			actions.add(() -> {
				FormulaBrick fb = (FormulaBrick) brick;
				Brick.FormulaField field = fb.brickFieldToTextViewIdMap.keySet().iterator().next();
				Intent intent = new Intent(getContext(), org.catrobat.catroid.ui.formulaeditor.FormulaEditor2Activity.class);
				org.catrobat.catroid.formulaeditor.Formula existingFormula = fb.getFormulaWithBrickField(field);
				if (existingFormula != null) {
					try {
						intent.putExtra(org.catrobat.catroid.ui.formulaeditor.FormulaEditor2Activity.EXTRA_FORMULA_STRING, existingFormula.interpretString(null));
					} catch (org.catrobat.catroid.formulaeditor.InterpretationException e) {
						intent.putExtra(org.catrobat.catroid.ui.formulaeditor.FormulaEditor2Activity.EXTRA_FORMULA_STRING, existingFormula.getTrimmedFormulaString(getContext()));
					}
				}
				if (getContext() instanceof ScriptCanvasActivity) {
					((ScriptCanvasActivity) getContext()).setActiveEditFormula(fb, field);
					((ScriptCanvasActivity) getContext()).startActivityForResult(intent, 8899);
				}
			});
		}

		items.add("Положить в Рюкзак");
		actions.add(() -> {
			Toast.makeText(getContext(), "Блок " + brickName + " добавлен в Рюкзак!", Toast.LENGTH_SHORT).show();
		});

		items.add("Вырезать блок");
		actions.add(() -> {
			if (brick instanceof ScriptBrick) {
				copyScriptStack(stackView);
			} else {
				copyBrick(brick);
			}
			deleteBrickFromStack(brick, stackView);
			Toast.makeText(getContext(), "Блок вырезан!", Toast.LENGTH_SHORT).show();
		});

		items.add("Скопировать стек блоков");
		actions.add(() -> copyScriptStack(stackView));

		if (copiedBrickClipboard != null) {
			items.add("Вставить скопированный блок ниже");
			actions.add(() -> pasteBrickAfter(brick, stackView));
		}

		boolean isCommented = (brick instanceof NoteBrick);
		items.add(isCommented ? "Включить блок" : "Закомментировать блок");
		actions.add(() -> toggleCommentBrick(brick, stackView));

		if (brick instanceof VisualPlacementBrick) {
			items.add("Разместить визуально на сцене");
			actions.add(() -> {
				Toast.makeText(getContext(), "Перейдите на 2D-холст для визуальной расстановки!", Toast.LENGTH_SHORT).show();
			});
		}

		boolean isProt = ProjectManager.getInstance().getCurrentProject() != null
				&& ProjectManager.getInstance().getCurrentProject().isProtectedProject();
		items.add(isProt ? "Снять защиту проекта" : "Защитить проект от изменений");
		actions.add(this::toggleProjectProtection);

		items.add("Системная информация");
		actions.add(() -> showSystemInfo(brick));

		items.add("Удалить блок");
		actions.add(() -> deleteBrickFromStack(brick, stackView));

		new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Блок: " + brickName)
				.setItems(items.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void showSystemInfo(Brick brick) {
		String info = "Имя класса: " + brick.getClass().getName() + "\n"
				+ "Простые параметры: " + brick.getClass().getSimpleName() + "\n"
				+ "Уникальный Hash: " + System.identityHashCode(brick) + "\n"
				+ "Объект: " + (sprite != null ? sprite.getName() : "Нет") + "\n"
				+ "Сцена: " + (ProjectManager.getInstance().getCurrentlyEditedScene() != null
					? ProjectManager.getInstance().getCurrentlyEditedScene().getName() : "Главная");
		new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("⚙️ Системная информация")
				.setMessage(info)
				.setPositiveButton(android.R.string.ok, null)
				.show();
	}

	private void copyScriptStack(View stackView) {
		if (stackView != null && stackView.getTag() instanceof Script) {
			Script script = (Script) stackView.getTag();
			try {
				copiedScriptClipboard = script.clone();
				Toast.makeText(getContext(), "Стек блоков скопирован в буфер!", Toast.LENGTH_SHORT).show();
			} catch (CloneNotSupportedException e) {
				Toast.makeText(getContext(), " Ошибка копирования", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void pasteScriptStack() {
		if (copiedScriptClipboard == null) {
			Toast.makeText(getContext(), " Буфер обмена пуст!", Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			Script clone = copiedScriptClipboard.clone();
			clone.setPosX(clone.getPosX() + 40);
			clone.setPosY(clone.getPosY() + 40);
			snapshot();
			sprite.addScript(clone);
			clone.setParents();
			rebuild();
			Toast.makeText(getContext(), "Стек блоков вставлен!", Toast.LENGTH_SHORT).show();
		} catch (CloneNotSupportedException e) {
			Toast.makeText(getContext(), " Ошибка вставки", Toast.LENGTH_SHORT).show();
		}
	}

	private void toggleProjectProtection() {
		Project project = ProjectManager.getInstance().getCurrentProject();
		if (project != null && project.getXmlHeader() != null) {
			boolean isProt = project.isProtectedProject();
			project.getXmlHeader().setProtectedProject(!isProt);
			Toast.makeText(getContext(), !isProt ? "🔒 Проект защищён от изменений!" : "🔓 Защита проекта снята!", Toast.LENGTH_SHORT).show();
		}
	}

	private void showBrickHelp(Brick brick) {
		String brickName = brick.getClass().getSimpleName();
		String helpText = "Тип блока: " + brick.getClass().getName() + "\n\n"
				+ "Этот блок задаёт логическое действие в цепочке скрипта. "
				+ "Поддерживает формулы, переменные, локальные параметры и вычисление условий во время работы игры.";
		new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("ℹ️ Справка: " + brickName)
				.setMessage(helpText)
				.setPositiveButton(android.R.string.ok, null)
				.show();
	}

	private void toggleCommentBrick(Brick brick, View stackView) {
		if (stackView != null && stackView.getTag() instanceof Script) {
			Script script = (Script) stackView.getTag();
			snapshot();
			List<Brick> parentList = findListContainingBrick(script, brick);
			if (parentList != null && brick instanceof NoteBrick) {
				int idx = parentList.indexOf(brick);
				if (idx >= 0) {
					parentList.remove(idx);
					Toast.makeText(getContext(), "Комментарий удалён", Toast.LENGTH_SHORT).show();
				}
			} else if (parentList != null && !(brick instanceof ScriptBrick)) {
				int idx = parentList.indexOf(brick);
				if (idx >= 0) {
					NoteBrick note = new NoteBrick("Закомментировано: " + brick.getClass().getSimpleName());
					parentList.set(idx, note);
					Toast.makeText(getContext(), "Блок закомментирован", Toast.LENGTH_SHORT).show();
				}
			}
			script.setParents();
			rebuild();
		}
	}

	private void deleteBrickFromStack(Brick brick, View stackView) {
		if (stackView != null && stackView.getTag() instanceof Script) {
			Script script = (Script) stackView.getTag();
			snapshot();
			if (brick instanceof ScriptBrick) {
				sprite.getScriptList().remove(script);
			} else {
				removeBrickRecursive(script.getBrickList(), brick);
			}
			script.setParents();
			rebuild();
		}
	}

	private void copyBrick(Brick brick) {
		try {
			copiedBrickClipboard = brick.clone();
			Toast.makeText(getContext(), "Блок скопирован в буфер!", Toast.LENGTH_SHORT).show();
		} catch (CloneNotSupportedException e) {
			Toast.makeText(getContext(), "Ошибка копирования блока", Toast.LENGTH_SHORT).show();
		}
	}

	private void pasteBrickAfter(Brick target, View stackView) {
		if (copiedBrickClipboard == null || stackView == null || !(stackView.getTag() instanceof Script)) {
			return;
		}
		Script script = (Script) stackView.getTag();
		try {
			Brick clone = copiedBrickClipboard.clone();
			List<Brick> parentList = findListContainingBrick(script, target);
			snapshot();
			if (parentList == null || target instanceof ScriptBrick) {
				script.addBrick(clone);
			} else {
				int index = parentList.indexOf(target);
				parentList.add(index < 0 ? parentList.size() : index + 1, clone);
			}
			script.setParents();
			rebuild();
		} catch (CloneNotSupportedException e) {
			Toast.makeText(getContext(), "Ошибка вставки блока", Toast.LENGTH_SHORT).show();
		}
	}

	private void scheduleAddNoteLongPress(float screenX, float screenY) {
		cancelAddNoteLongPress();
		longPressScheduled = true;
		final float worldX = (screenX - panX) / scale;
		final float worldY = (screenY - panY) / scale;
		longPressHandler.postDelayed(() -> {
			if (longPressScheduled) {
				longPressScheduled = false;
				showAddNoteDialog(worldX, worldY);
			}
		}, 500);
	}

	private void cancelAddNoteLongPress() {
		longPressScheduled = false;
		longPressHandler.removeCallbacksAndMessages(null);
	}

	private void addNoteViews() {
		if (sprite == null) {
			return;
		}
		for (ScriptNote note : sprite.getScriptNotes()) {
			addNoteView(note);
		}
	}

	private void addNoteView(ScriptNote note) {
		final TextView card = new TextView(getContext());
		card.setText(note.getText());
		card.setTextColor(0xFF0B1220);
		card.setTextSize(15f);
		card.setPadding(dp(12), dp(10), dp(12), dp(10));
		card.setBackgroundColor(0xFFFDE68A);
		card.setMaxWidth(dp(240));
		card.setPivotX(0f);
		card.setPivotY(0f);
		card.setScaleX(note.getScale());
		card.setScaleY(note.getScale());
		card.setOnTouchListener(new NoteTouchListener(note, card));

		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.leftMargin = Math.round(note.getPosX());
		params.topMargin = Math.round(note.getPosY());
		card.setLayoutParams(params);
		world.addView(card);
	}

	private void showAddNoteDialog(float worldX, float worldY) {
		final EditText input = new EditText(getContext());
		input.setPadding(dp(16), dp(16), dp(16), dp(16));
		new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(R.string.script_canvas_add_note)
				.setView(input)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					if (input.getText().toString().trim().isEmpty()) {
						return;
					}
					ScriptNote note = new ScriptNote(input.getText().toString(), worldX, worldY);
					sprite.getScriptNotes().add(note);
					addNoteView(note);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void showEditNoteDialog(ScriptNote note, TextView card) {
		LinearLayout box = new LinearLayout(getContext());
		box.setOrientation(LinearLayout.VERTICAL);
		box.setPadding(dp(16), dp(12), dp(16), dp(4));
		final EditText input = new EditText(getContext());
		input.setText(note.getText());
		box.addView(input);
		LinearLayout row = new LinearLayout(getContext());
		row.setOrientation(LinearLayout.HORIZONTAL);
		Button smaller = new Button(getContext());
		smaller.setText("A-");
		smaller.setOnClickListener(v -> {
			note.setScale(Math.max(0.4f, note.getScale() - 0.2f));
			card.setScaleX(note.getScale());
			card.setScaleY(note.getScale());
		});
		Button bigger = new Button(getContext());
		bigger.setText("A+");
		bigger.setOnClickListener(v -> {
			note.setScale(Math.min(4f, note.getScale() + 0.2f));
			card.setScaleX(note.getScale());
			card.setScaleY(note.getScale());
		});
		row.addView(smaller);
		row.addView(bigger);
		box.addView(row);
		new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(R.string.script_canvas_note_hint)
				.setView(box)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					note.setText(input.getText().toString());
					card.setText(note.getText());
				})
				.setNeutralButton(R.string.script_canvas_delete_note, (dialog, which) -> {
					sprite.getScriptNotes().remove(note);
					world.removeView(card);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private class NoteTouchListener implements OnTouchListener {
		private final ScriptNote note;
		private final TextView card;
		private float startRawX;
		private float startRawY;
		private float startPosX;
		private float startPosY;
		private boolean moved;

		NoteTouchListener(ScriptNote note, TextView card) {
			this.note = note;
			this.card = card;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					startRawX = event.getRawX();
					startRawY = event.getRawY();
					startPosX = note.getPosX();
					startPosY = note.getPosY();
					moved = false;
					return true;
				case MotionEvent.ACTION_MOVE:
					float dx = (event.getRawX() - startRawX) / scale;
					float dy = (event.getRawY() - startRawY) / scale;
					if (Math.hypot(dx, dy) > TOUCH_SLOP_PX) {
						moved = true;
					}
					note.setPosX(startPosX + dx);
					note.setPosY(startPosY + dy);
					LayoutParams params = (LayoutParams) card.getLayoutParams();
					params.leftMargin = Math.round(note.getPosX());
					params.topMargin = Math.round(note.getPosY());
					card.setLayoutParams(params);
					return true;
				case MotionEvent.ACTION_UP:
					if (!moved) {
						showEditNoteDialog(note, card);
					}
					return true;
				default:
					return false;
			}
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
			panX = focusX - (focusX - panX) * ratio;
			panY = focusY - (focusY - panY) * ratio;
			applyTransform();
			return true;
		}
	}
}
