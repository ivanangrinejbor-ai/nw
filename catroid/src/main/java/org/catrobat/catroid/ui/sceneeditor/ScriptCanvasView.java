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
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.FloatingBrick;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.BrickBaseType;
import org.catrobat.catroid.content.bricks.CompositeBrick;
import org.catrobat.catroid.content.bricks.ElseIfSeparatorBrick;
import org.catrobat.catroid.content.bricks.EndBrick;
import org.catrobat.catroid.content.bricks.FormulaBrick;
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.VisualPlacementBrick;
import org.catrobat.catroid.utils.BrickCollapseManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
	private LinearLayout blockBadge;
	private View blockGhost;
	private Brick draggingBrick;
	private View connectionHighlight;
	private FloatingBrick draggingDetachedBrick;
	private final List<View> hiddenDragViews = new ArrayList<>();
	private Brick animateNextBrick;
	private List<FloatingBrick> detachedBricks = new ArrayList<>();

	private final Set<Brick> selectedBricks = new HashSet<>();
	private final List<View> selectedOverlays = new ArrayList<>();
	private boolean selectionMode = false;
	private View selectionBar;
	public interface SelectionListener { void onSelectionChanged(int count); }
	private SelectionListener selectionListener;
	public void setSelectionListener(SelectionListener l) { this.selectionListener = l; }
private void notifySelection() {
		if (selectionListener != null) selectionListener.onSelectionChanged(selectedBricks.size());
	}

	private boolean indentationEnabled = false;

	private static final int MAX_UNDO_STEPS = 30;
	private final Deque<List<Script>> undoStack = new ArrayDeque<>();
	private final Deque<List<Script>> redoStack = new ArrayDeque<>();
	private boolean rebuildPosted;
	public interface UndoRedoListener { void onUndoRedoChanged(); }
	public interface ContentChangedListener { void onContentChanged(); }
	private UndoRedoListener undoRedoListener;
	private ContentChangedListener contentChangedListener;
	public void setUndoRedoListener(UndoRedoListener l) { this.undoRedoListener = l; }
	public void setContentChangedListener(ContentChangedListener l) { this.contentChangedListener = l; }
	private void notifyUndoRedo() { if (undoRedoListener != null) undoRedoListener.onUndoRedoChanged(); }
	private void notifyContentChanged() { if (contentChangedListener != null) contentChangedListener.onContentChanged(); }

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

		SelectionBar bar = new SelectionBar(getContext());
		bar.setVisibility(View.GONE);
		LayoutParams barParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		barParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		barParams.bottomMargin = dp(16);
		selectionBar = bar;
		addView(bar, barParams);
	}

	private int dp(float value) {
		return Math.round(value * density);
	}

	private final Map<Integer, Queue<View>> brickViewPool = new HashMap<>();
	private final List<StackBound> cachedStackBounds = new ArrayList<>();

	private static class StackBound {
		final View stackView;
		final Script script;
		final float left, top, right, bottom;

		StackBound(View stackView, Script script, float left, float top, float right, float bottom) {
			this.stackView = stackView;
			this.script = script;
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
		}
	}

	private void recycleBrickViews(ViewGroup parent) {
		if (parent == null) return;
		for (int i = 0; i < parent.getChildCount(); i++) {
			View child = parent.getChildAt(i);
			Object tag = child.getTag();
			if (tag instanceof Brick) {
				Brick brick = (Brick) tag;
				child.clearAnimation();
				child.setOnTouchListener(null);
				if (brick instanceof BrickBaseType) {
					brickViewPool.computeIfAbsent(((BrickBaseType) brick).getViewResource(), k -> new ArrayDeque<>()).offer(child);
				}
			}
			if (child instanceof ViewGroup && !(child.getTag() instanceof Script)) {
				recycleBrickViews((ViewGroup) child);
			}
		}
	}

	private View obtainBrickView(Brick brick) {
		if (brick instanceof BrickBaseType) {
			int layoutId = ((BrickBaseType) brick).getViewResource();
			Queue<View> pool = brickViewPool.get(layoutId);
			if (pool != null && !pool.isEmpty()) {
				View reused = pool.poll();
				if (reused != null) {
					reused.setVisibility(View.VISIBLE);
					reused.setAlpha(1f);
					reused.setScaleX(1f);
					reused.setScaleY(1f);
					reused.setSelected(false);
					return reused;
				}
			}
		}
		return brick.getView(getContext());
	}

	private void updateViewportCulling() {
		if (getWidth() <= 0 || getHeight() <= 0 || world == null) return;
		float viewportLeft = -panX / scale - dp(120);
		float viewportTop = -panY / scale - dp(120);
		float viewportRight = (getWidth() - panX) / scale + dp(120);
		float viewportBottom = (getHeight() - panY) / scale + dp(120);

		for (int i = 0; i < world.getChildCount(); i++) {
			View child = world.getChildAt(i);
			float childLeft = child.getLeft();
			float childTop = child.getTop();
			float childRight = childLeft + Math.max(child.getWidth(), dp(420));
			float childBottom = childTop + Math.max(child.getHeight(), dp(100));

			boolean isVisible = childRight >= viewportLeft && childLeft <= viewportRight
					&& childBottom >= viewportTop && childTop <= viewportBottom;
			child.setVisibility(isVisible ? View.VISIBLE : View.GONE);
		}
	}

	private void cacheStackBounds() {
		cachedStackBounds.clear();
		for (int i = 0; i < world.getChildCount(); i++) {
			View child = world.getChildAt(i);
			if (child.getTag() instanceof Script) {
				Script script = (Script) child.getTag();
				float left = child.getLeft();
				float top = child.getTop();
				float right = left + Math.max(child.getWidth(), dp(420));
				float bottom = top + Math.max(child.getHeight(), dp(100));
				cachedStackBounds.add(new StackBound(child, script, left, top, right, bottom));
			}
		}
	}

	public void setSprite(Sprite sprite) {
		this.sprite = sprite;
		detachedBricks = sprite.getFloatingBricks();
		buildStacks();
		addDetachedViews();
	}

	private void buildStacks() {
		clearConnectionHighlight();
		recycleBrickViews(world);
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
		updateViewportCulling();
	}

	private View buildStack(Script script) {
		LinearLayout stack = new LinearLayout(getContext());
		stack.setOrientation(LinearLayout.VERTICAL);
		stack.setClipChildren(false);
		stack.setClipToPadding(false);
		final int stackWidth = Math.max(dp(420), getResources().getDisplayMetrics().widthPixels - dp(32));
		stack.setMinimumWidth(stackWidth);

		List<Brick> flat = new ArrayList<>();
		flat.add(script.getScriptBrick());
		for (Brick brick : script.getBrickList()) {
			brick.addToFlatList(flat);
		}
		for (Brick brick : flat) {
			if (!isBrickVisibleInCollapsedHierarchy(brick)) {
				continue;
			}
			View brickView;
			try {
				brickView = obtainBrickView(brick);
			} catch (Exception e) {
				Log.e("ScriptCanvasView", "Failed to render brick " + brick.getClass().getName(), e);
				TextView fallback = new TextView(getContext());
				fallback.setText("? " + brick.getClass().getSimpleName());
				fallback.setTextColor(0xFFF8FAFC);
				fallback.setPadding(dp(8), dp(6), dp(8), dp(6));
				fallback.setBackgroundColor(0xFF334155);
				brickView = fallback;
			}
			if (brick.isLocked()) {
				lockableView(brickView);
			}
			if (indentationEnabled) {
				int depth = getBrickDepth(brick);
				if (depth > 0) {
					brickView.setPadding(dp(10) + depth * dp(14), brickView.getPaddingTop(),
							brickView.getPaddingRight(), brickView.getPaddingBottom());
				}
			}
			brickView.setTag(brick);
			final Brick targetBrick = brick;
			final Script targetScript = script;
			final View targetStackView = stack;
			BrickTouchDragListener dragListener =
					new BrickTouchDragListener(targetScript, targetBrick, brickView, targetStackView);
			wireFormulaFields(brick, brickView);
			brickView.setOnTouchListener(dragListener);
			wireDragToChildren(brickView, dragListener);
			if (selectionMode && selectedBricks.contains(brick)) {
				highlightSelected(brickView);
			}
			LinearLayout.LayoutParams brickParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			stack.addView(brickView, brickParams);
			animateInsertedBrick(brickView, brick);
		}

		LayoutParams params = new LayoutParams(stackWidth, LayoutParams.WRAP_CONTENT);
		params.leftMargin = Math.round(script.getPosX());
		params.topMargin = Math.round(script.getPosY());
		stack.setLayoutParams(params);
		stack.setTag(script);
		return stack;
	}

	private boolean isBrickVisibleInCollapsedHierarchy(Brick brick) {
		Brick parent = brick.getParent();
		while (parent != null) {
			if (BrickCollapseManager.INSTANCE.isCollapsed(parent)) {
				return false;
			}
			parent = parent.getParent();
		}
		return true;
	}

	private int getBrickDepth(Brick brick) {
		int depth = 0;
		Brick parent = brick.getParent();
		while (parent != null) {
			if (!isElseBrick(parent)) {
				depth++;
			}
			parent = parent.getParent();
		}
		if (depth > 0 && isEndOrElseBrick(brick)) {
			depth--;
		}
		return depth;
	}

	private boolean isElseBrick(Brick brick) {
		String name = brick.getClass().getSimpleName();
		return name.endsWith("ElseBrick") || name.contains("Else");
	}

	private boolean isEndOrElseBrick(Brick brick) {
		if (brick instanceof EndBrick) {
			return true;
		}
		String name = brick.getClass().getSimpleName();
		return name.endsWith("EndBrick") || name.endsWith("ElseBrick");
	}

	private void lockableView(View brickView) {
		brickView.setAlpha(0.6f);
		lockBadge(brickView);
	}

	private void lockBadge(View brickView) {
		if (!(brickView instanceof ViewGroup)) return;
		ImageView lockIcon = new ImageView(getContext());
		try {
			android.graphics.drawable.Drawable d = getContext().getDrawable(R.drawable.ic_lock_gray);
			if (d == null) d = getContext().getDrawable(android.R.drawable.ic_lock_lock);
			if (d != null) lockIcon.setImageDrawable(d);
		} catch (Exception ignored) {}
		lockIcon.setPadding(dp(4), dp(2), dp(4), dp(2));
		if (brickView instanceof LinearLayout) {
			((LinearLayout) brickView).addView(lockIcon, 0);
		}
	}

	private void highlightSelected(View brickView) {
		GradientDrawable drawable = new GradientDrawable();
		drawable.setColor(0x3338BDF8);
		drawable.setCornerRadius(dp(6));
		drawable.setStroke(dp(2), 0xFF38BDF8);
		View overlay = new View(getContext());
		overlay.setBackground(drawable);
		overlay.setClickable(false);
		overlay.setEnabled(false);
		if (brickView instanceof ViewGroup) {
			packOverlay((ViewGroup) brickView, overlay);
		}
		selectedOverlays.add(overlay);
		brickView.setSelected(true);
	}

	private void packOverlay(ViewGroup parent, View overlay) {
		parent.setClipChildren(false);
		parent.addView(overlay, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}

	private void clearSelectedOverlays() {
		for (View overlay : selectedOverlays) {
			if (overlay.getParent() instanceof ViewGroup) {
				((ViewGroup) overlay.getParent()).removeView(overlay);
			}
		}
		selectedOverlays.clear();
	}

	private void addDetachedViews() {
		for (FloatingBrick detached : new ArrayList<>(detachedBricks)) {
			View brickView;
			try {
				brickView = obtainBrickView(detached.brick);
			} catch (Exception e) {
				Log.e("ScriptCanvasView", "Failed to render detached brick", e);
				continue;
			}
			brickView.setTag(detached.brick);
			BrickTouchDragListener dragListener =
					new BrickTouchDragListener(null, detached, detached.brick, brickView, world);
			wireFormulaFields(detached.brick, brickView);
			brickView.setOnTouchListener(dragListener);
			wireDragToChildren(brickView, dragListener);
			LayoutParams params = new LayoutParams(
					Math.max(dp(420), LayoutParams.WRAP_CONTENT), LayoutParams.WRAP_CONTENT);
			params.leftMargin = Math.round(detached.x);
			params.topMargin = Math.round(detached.y);
			world.addView(brickView, params);
			animateInsertedBrick(brickView, detached.brick);
		}
	}

	public void rebuild() {
		clearConnectionHighlight();
		world.removeAllViews();
		buildStacks();
		addDetachedViews();
		applyTransform();
		animateNextBrick = null;
		autoSave();
		notifyContentChanged();
	}

	private void rebuildDeferred() {
		if (rebuildPosted) return;
		rebuildPosted = true;
		post(() -> {
			rebuildPosted = false;
			if (isAttachedToWindow()) rebuild();
		});
	}

	private void autoSave() {
		Project currentProject = ProjectManager.getInstance().getCurrentProject();
		if (currentProject == null) return;
		ProjectSaveCoordinator.saveAsync(currentProject);
	}

	public static void saveProjectAsync(Project project) {
		ProjectSaveCoordinator.saveAsync(project);
	}

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
		rebuildDeferred();
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
		if (ProjectManager.getInstance().getCurrentProject() != null
				&& ProjectManager.getInstance().getCurrentProject().isProtectedProject()) {
			Toast.makeText(getContext(), R.string.protected_project_cannot_edit, Toast.LENGTH_SHORT).show();
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
			rebuildDeferred();
			return true;
		}
		View stackView = findStackViewAtWorld(worldX, worldY);
		if (stackView != null && stackView.getTag() instanceof Script) {
			Script script = (Script) stackView.getTag();
			Brick targetBrick = findBrickInStack(stackView, worldY);
			insertBrickIntoScript(script, targetBrick, clone);
			script.setParents();
			rebuildDeferred();
			return true;
		}
		float[] freePosition = findFreeDetachedPosition(worldX, worldY);
		detachedBricks.add(new FloatingBrick(clone, freePosition[0], freePosition[1]));
		rebuildDeferred();
		return true;
	}

	private float[] findFreeDetachedPosition(float requestedX, float requestedY) {
		float width = dp(420);
		float height = dp(112);
		float centerX = Math.max(dp(16), (getWidth() / 2f - panX) / scale - width / 2f);
		float centerY = Math.max(dp(16), (getHeight() / 2f - panY) / scale - height / 2f);
		float startX = Math.max(dp(16), requestedX - width / 2f);
		float startY = Math.max(dp(16), requestedY - dp(20));
		for (int radius = 0; radius < 12; radius++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dx = -radius; dx <= radius; dx++) {
					float x = radius == 0 ? startX : centerX + dx * width * 0.55f;
					float y = radius == 0 ? startY : centerY + dy * height * 1.25f;
					if (x >= 0 && y >= 0 && !positionOverlapsContent(x, y, width, height)) {
						return new float[]{x, y};
					}
				}
			}
		}
		return new float[]{startX, startY};
	}

	private boolean positionOverlapsContent(float x, float y, float width, float height) {
		for (int i = 0; i < world.getChildCount(); i++) {
			View child = world.getChildAt(i);
			if (!(child.getTag() instanceof Script)) continue;
			float childRight = child.getLeft() + Math.max(child.getWidth(), dp(420));
			float childBottom = child.getTop() + Math.max(child.getHeight(), dp(112));
			if (x < childRight && x + width > child.getLeft()
					&& y < childBottom && y + height > child.getTop()) return true;
		}
for (FloatingBrick detached : detachedBricks) {
			if (x < detached.x + width && x + width > detached.x
					&& y < detached.y + height && y + height > detached.y) return true;
		}
		return false;
	}

	private void insertBrickIntoScript(Script script, Brick targetBrick, Brick clone) {
		if (ProjectManager.getInstance().getCurrentProject() != null
				&& ProjectManager.getInstance().getCurrentProject().isProtectedProject()) {
			Toast.makeText(getContext(), R.string.protected_project_cannot_edit, Toast.LENGTH_SHORT).show();
			return;
		}
		if (targetBrick == null || targetBrick instanceof ScriptBrick) {
			script.addBrick(0, clone);
			return;
		}
		if (targetBrick instanceof CompositeBrick) {
			List<Brick> nested = ((CompositeBrick) targetBrick).getNestedBricks();
			if (nested != null) {
				nested.add(0, clone);
				return;
			}
		}
if (targetBrick instanceof EndBrick && targetBrick.getParent() instanceof CompositeBrick) {
			Brick container = targetBrick.getParent();
			List<Brick> parentList = findListContainingBrick(script, container);
			if (parentList != null) {
				int index = parentList.indexOf(container);
				parentList.add(index < 0 ? parentList.size() : index + 1, clone);
				return;
			}
		}
if (targetBrick instanceof IfLogicBeginBrick.ElseBrick
				|| targetBrick instanceof ElseIfSeparatorBrick) {
			List<Brick> branch = targetBrick.getDragAndDropTargetList();
			if (branch != null) {
				branch.add(0, clone);
				return;
			}
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
		if (!cachedStackBounds.isEmpty()) {
			for (int i = cachedStackBounds.size() - 1; i >= 0; i--) {
				StackBound bound = cachedStackBounds.get(i);
				if (worldX >= bound.left && worldX <= bound.right
						&& worldY >= bound.top && worldY <= bound.bottom) {
					return bound.stackView;
				}
			}
		}
		for (int i = world.getChildCount() - 1; i >= 0; i--) {
			View child = world.getChildAt(i);
			if (child.getTag() instanceof Script) {
				float left = child.getLeft();
				float top = child.getTop();
				if (worldX >= left && worldX <= left + Math.max(child.getWidth(), dp(420))
						&& worldY >= top && worldY <= top + Math.max(child.getHeight(), dp(100))) {
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
		FloatingBrick detached = findDetachedBrick(brick);
		if (detached != null) {
			detachedBricks.remove(detached);
			rebuild();
			return;
		}
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
						startBlockDrag(brick, findDetachedBrick(brick));
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
		startBlockDrag(brick, null);
	}

	private void startBlockDrag(Brick brick, FloatingBrick detached) {
		draggingBrick = brick;
		draggingDetachedBrick = detached;
		cacheStackBounds();
		View ghost;
		try {
			ghost = brick.getPrototypeView(getContext());
		} catch (Exception e) {
			Log.e("ScriptCanvasView", "Failed to create block drag preview", e);
			draggingBrick = null;
			draggingDetachedBrick = null;
			return;
		}
		ghost.setAlpha(0.85f);
		ghost.setScaleX(0.92f);
		ghost.setScaleY(0.92f);
		blockGhost = ghost;
		hideMovingViews(brick, detached);
		addView(ghost, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		ghost.animate().scaleX(1.04f).scaleY(1.04f).setDuration(140)
				.withEndAction(() -> ghost.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
				.start();
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
		autoScrollWhileDragging(rawX, rawY);
		updateConnectionHighlight(rawX, rawY);
	}

	private void hideMovingViews(Brick moving, FloatingBrick detached) {
		hiddenDragViews.clear();
		if (detached != null) {
			View source = findViewWithTag(world, moving);
			if (source != null) {
				source.setAlpha(0.12f);
				hiddenDragViews.add(source);
			}
			return;
		}
		collectMovingViews(world, moving);
	}

	private void collectMovingViews(ViewGroup parent, Brick moving) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			View child = parent.getChildAt(i);
			Object tag = child.getTag();
			if (tag instanceof Brick && (tag == moving || containsBrick(moving, (Brick) tag))) {
				child.setAlpha(0.12f);
				hiddenDragViews.add(child);
			}
			if (child instanceof ViewGroup) {
				collectMovingViews((ViewGroup) child, moving);
			}
		}
	}

	private View findViewWithTag(ViewGroup parent, Object tag) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			View child = parent.getChildAt(i);
			if (child.getTag() == tag) return child;
			if (child instanceof ViewGroup) {
				View result = findViewWithTag((ViewGroup) child, tag);
				if (result != null) return result;
			}
		}
		return null;
	}

	private void autoScrollWhileDragging(float rawX, float rawY) {
		int[] location = new int[2];
		getLocationOnScreen(location);
		float x = rawX - location[0];
		float y = rawY - location[1];
		float edge = dp(72);
		float step = dp(14);
		float dx = 0f;
		float dy = 0f;
		if (x >= 0f && x < edge) dx = step * (1f - x / edge);
		else if (x > getWidth() - edge && x <= getWidth()) dx = -step * (1f - (getWidth() - x) / edge);
		if (y >= 0f && y < edge) dy = step * (1f - y / edge);
		else if (y > getHeight() - edge && y <= getHeight()) dy = -step * (1f - (getHeight() - y) / edge);
		if (dx != 0f || dy != 0f) {
			panX += dx;
			panY += dy;
			applyTransform();
		}
	}

	private void updateConnectionHighlight(float rawX, float rawY) {
		if (draggingBrick == null) {
			clearConnectionHighlight();
			return;
		}
		int[] location = new int[2];
		getLocationOnScreen(location);
		float localX = rawX - location[0];
		float localY = rawY - location[1];
		if (localX < 0 || localY < 0 || localX > getWidth() || localY > getHeight()) {
			clearConnectionHighlight();
			return;
		}
		float worldX = (localX - panX) / scale;
		float worldY = (localY - panY) / scale;
		View stackView = findStackViewAtWorld(worldX, worldY);
		Brick targetBrick = stackView == null ? null : findBrickInStack(stackView, worldY);
		if (targetBrick == draggingBrick || (targetBrick != null && containsBrick(draggingBrick, targetBrick))) {
			clearConnectionHighlight();
			return;
		}
		View targetView = findBrickView(stackView, targetBrick);
		if (targetView == null) targetView = stackView;
		if (targetView == null) {
			clearConnectionHighlight();
			return;
		}
		if (connectionHighlight != null && connectionHighlight.getTag() == targetView) return;
		clearConnectionHighlight();
		GradientDrawable drawable = new GradientDrawable();
		drawable.setColor(0x2238BDF8);
		drawable.setStroke(dp(3), 0xFF38BDF8);
		View highlight = new View(getContext());
		highlight.setTag(targetView);
		highlight.setBackground(drawable);
		highlight.setClickable(false);
		int left = targetView == stackView ? stackView.getLeft() : stackView.getLeft() + targetView.getLeft();
		int top = targetView == stackView ? stackView.getTop() : stackView.getTop() + targetView.getTop();
		LayoutParams params = new LayoutParams(targetView.getWidth(), targetView.getHeight());
		params.leftMargin = left;
		params.topMargin = top;
		connectionHighlight = highlight;
		world.addView(highlight, params);
		highlight.setAlpha(0.45f);
		highlight.animate().alpha(1f).setDuration(260)
				.withEndAction(() -> {
					if (connectionHighlight == highlight) {
						highlight.animate().alpha(0.5f).setDuration(420).start();
					}
				}).start();
	}

	private View findBrickView(View stackView, Brick target) {
		if (!(stackView instanceof android.view.ViewGroup) || target == null) return null;
		android.view.ViewGroup group = (android.view.ViewGroup) stackView;
		for (int i = 0; i < group.getChildCount(); i++) {
			View child = group.getChildAt(i);
			if (child.getTag() == target) return child;
		}
		return null;
	}

	private void clearConnectionHighlight() {
		if (connectionHighlight != null) {
			world.removeView(connectionHighlight);
			connectionHighlight = null;
		}
	}

	private void removeBlockGhost() {
		removeBlockBadge();
		clearConnectionHighlight();
		if (blockGhost != null) {
			removeView(blockGhost);
			blockGhost = null;
		}
		for (View view : hiddenDragViews) {
			view.setAlpha(1f);
		}
		hiddenDragViews.clear();
		draggingBrick = null;
		draggingDetachedBrick = null;
	}

private FloatingBrick findDetachedBrick(Brick brick) {
		for (FloatingBrick detached : detachedBricks) {
			if (detached.brick == brick) return detached;
		}
		return null;
	}

	private void dropBlockGhost(float rawX, float rawY) {
		Brick moving = draggingBrick;
		FloatingBrick sourceDetached = draggingDetachedBrick;
		removeBlockGhost();
		if (moving == null || sprite == null) {
			return;
		}
		if (ProjectManager.getInstance().getCurrentProject() != null
				&& ProjectManager.getInstance().getCurrentProject().isProtectedProject()) {
			Toast.makeText(getContext(), R.string.protected_project_cannot_edit, Toast.LENGTH_SHORT).show();
			return;
		}
		if (moving.isLocked()) {
			Toast.makeText(getContext(), R.string.brick_locked, Toast.LENGTH_SHORT).show();
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
		if (targetBrick == moving || (targetBrick != null && containsBrick(moving, targetBrick))) {
			return;
		}
		snapshot();
		if (sourceDetached != null) {
			detachedBricks.remove(sourceDetached);
		}
		for (Script s : sprite.getScriptList()) {
			if (removeBrickRecursive(s.getBrickList(), moving)) {
				break;
			}
		}
if (script == null) {
			detachedBricks.add(new FloatingBrick(moving, worldX, worldY));
		} else {
			insertBrickIntoScript(script, targetBrick, moving);
			script.setParents();
		}
		animateNextBrick = moving;
		rebuildDeferred();
	}

	private void animateInsertedBrick(View view, Brick brick) {
		if (brick != animateNextBrick) return;
		view.setAlpha(0f);
		view.setTranslationY(-dp(14));
		view.animate().alpha(1f).translationY(0f).setDuration(180).start();
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
				if (fieldView instanceof TextView) {
					TextView textView = (TextView) fieldView;
					textView.setEllipsize(null);
					textView.setSingleLine(false);
					textView.setMaxLines(3);
				}
				fieldView.setOnClickListener(v -> {
					openFormulaEditor2(formulaBrick, field);
				});
			}
		}
	}

	private class BrickTouchDragListener implements OnTouchListener {
		private final Script script;
		private final FloatingBrick detached;
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
					startBlockDrag(brick, detached);
				}
			}
		};

		BrickTouchDragListener(Script script, Brick brick, View brickView, View stackView) {
			this(script, null, brick, brickView, stackView);
		}

		BrickTouchDragListener(Script script, FloatingBrick detached, Brick brick, View brickView, View stackView) {
			this.script = script;
			this.detached = detached;
			this.brick = brick;
			this.brickView = brickView;
			this.stackView = stackView;
		}

		boolean isDragActive() {
			return isLongPressed;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getActionMasked()) {
case MotionEvent.ACTION_DOWN:
					downRawX = event.getRawX();
					downRawY = event.getRawY();
					startPosX = script != null ? script.getPosX() : detached.x;
					startPosY = script != null ? script.getPosY() : detached.y;
					isLongPressed = false;
					if (brick != null && brick.isLocked() && detached == null) {
						return true;
					}
					handler.postDelayed(longPressRunnable, 350);
					return true;

				case MotionEvent.ACTION_MOVE:
					float dx = event.getRawX() - downRawX;
					float dy = event.getRawY() - downRawY;
					if (!isLongPressed && Math.hypot(dx, dy) > dp(6)) {
						handler.removeCallbacks(longPressRunnable);
					}
					if (isLongPressed) {
					if (isEventHeader && script != null) {
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
						if (isEventHeader && script != null) {
							if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
								script.setPosX(startPosX);
								script.setPosY(startPosY);
							}
							rebuildDeferred();
						} else {
							if (event.getActionMasked() == MotionEvent.ACTION_UP) {
								dropBlockGhost(event.getRawX(), event.getRawY());
							} else {
								removeBlockGhost();
							}
						}
} else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
						if (selectionMode) {
							selectBrick(brick);
						} else if (blockGhost == null) {
							showBlockBadge(brick, brickView, stackView);
						}
					}
					isLongPressed = false;
					isEventHeader = false;
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
				return true;
			case MotionEvent.ACTION_MOVE:
				if (scaleDetector.isInProgress() || event.getPointerCount() > 1) {
					return true;
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
		updateViewportCulling();
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
		boolean isProtected = ProjectManager.getInstance().getCurrentProject() != null
				&& ProjectManager.getInstance().getCurrentProject().isProtectedProject();
		if (isProtected) {
			List<String> protectedItems = new ArrayList<>();
			List<Runnable> protectedActions = new ArrayList<>();
			protectedItems.add("Справка по блоку");
			protectedActions.add(() -> showBrickHelp(brick));
			protectedItems.add("Системная информация");
			protectedActions.add(() -> showSystemInfo(brick));
			new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
					.setTitle("Блок: " + brick.getClass().getSimpleName())
					.setItems(protectedItems.toArray(new String[0]),
							(dialog, which) -> protectedActions.get(which).run())
					.setNegativeButton(android.R.string.cancel, null)
					.show();
			return;
		}
		if (brick.isLocked()) {
			List<String> lockedItems = new ArrayList<>();
			List<Runnable> lockedActions = new ArrayList<>();
			lockedItems.add("Справка по блоку");
			lockedActions.add(() -> showBrickHelp(brick));
			lockedItems.add("Снять блокировку");
			lockedActions.add(() -> unlockBrick(brick));
			lockedItems.add("Системная информация");
			lockedActions.add(() -> showSystemInfo(brick));
			new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
					.setTitle("Блок: " + brick.getClass().getSimpleName())
					.setItems(lockedItems.toArray(new String[0]),
							(dialog, which) -> lockedActions.get(which).run())
					.setNegativeButton(android.R.string.cancel, null)
					.show();
			return;
		}
		String brickName = brick.getClass().getSimpleName();
		List<String> items = new ArrayList<>();
		List<Runnable> actions = new ArrayList<>();
		Script script = (stackView != null && stackView.getTag() instanceof Script)
				? (Script) stackView.getTag() : null;

		items.add("Справка по блоку");
		actions.add(() -> showBrickHelp(brick));

		if (brick instanceof FormulaBrick && ((FormulaBrick) brick).hasEditableFormulaField()) {
			items.add("Редактировать формулу 2.0");
				actions.add(() -> {
					FormulaBrick fb = (FormulaBrick) brick;
					Brick.FormulaField field = fb.brickFieldToTextViewIdMap.keySet().iterator().next();
					if (getContext() instanceof ScriptCanvasActivity) {
						openFormulaEditor2(fb, field);
					}
			});
		}

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

		if (copiedScriptClipboard != null && brick instanceof ScriptBrick) {
			items.add("Вставить скопированный стек");
			actions.add(this::pasteScriptStack);
		}

if (script != null) {
			items.add("Переместить стек выше");
			actions.add(() -> moveScript(script, -1));
			items.add("Переместить стек ниже");
			actions.add(() -> moveScript(script, 1));
		}

		boolean isCommented = brick.isCommentedOut();
		items.add(isCommented ? "Включить блок" : "Закомментировать блок");
		actions.add(() -> toggleCommentBrick(brick, stackView));

if (brick instanceof VisualPlacementBrick) {
			items.add("Разместить визуально на сцене");
			actions.add(() -> {
				if (getContext() instanceof ScriptCanvasActivity) {
					((ScriptCanvasActivity) getContext()).openVisualPlacement((VisualPlacementBrick) brick);
				}
			});
		}

		if (brick instanceof CompositeBrick) {
			boolean collapsed = BrickCollapseManager.INSTANCE.isCollapsed(brick);
			items.add(collapsed ? "Развернуть блок" : "Свернуть блок");
			actions.add(() -> {
				snapshot();
				BrickCollapseManager.INSTANCE.toggleCollapsed(brick);
				rebuildDeferred();
			});
		}

		if (brick.isLocked()) {
			items.add("Снять блокировку");
			actions.add(() -> unlockBrick(brick));
		} else {
			items.add("Заблокировать блок");
			actions.add(() -> showLockBrickDialog(brick));
		}

		if (!(brick instanceof ScriptBrick) && !(brick instanceof EndBrick)) {
			items.add("Выбрать блок");
			actions.add(() -> {
				enterSelectionMode();
				selectBrick(brick);
			});
		}

		boolean isProt = ProjectManager.getInstance().getCurrentProject() != null
				&& ProjectManager.getInstance().getCurrentProject().isProtectedProject();
		if (!isProt) {
			items.add("Защитить проект от изменений");
			actions.add(() -> new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
					.setTitle("Защитить проект от изменений")
					.setMessage(getContext().getString(R.string.export_protected_warning))
					.setPositiveButton(android.R.string.ok, (d, w) -> toggleProjectProtection())
					.setNegativeButton(android.R.string.cancel, null)
					.show());
		}

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
				.setTitle("Системная информация")
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

	private void openFormulaEditor2(FormulaBrick formulaBrick, Brick.FormulaField field) {
		if (!(getContext() instanceof ScriptCanvasActivity)) return;
		if (formulaBrick.isLocked()) {
			Toast.makeText(getContext(), R.string.brick_locked, Toast.LENGTH_SHORT).show();
			return;
		}
		if (ProjectManager.getInstance().getCurrentProject() != null
				&& ProjectManager.getInstance().getCurrentProject().isProtectedProject()) {
			Toast.makeText(getContext(), R.string.protected_project_cannot_edit, Toast.LENGTH_SHORT).show();
			return;
		}
		ScriptCanvasActivity activity = (ScriptCanvasActivity) getContext();
		activity.setActiveEditFormula(formulaBrick, field);
		Intent intent = new Intent(getContext(),
				org.catrobat.catroid.ui.formulaeditor.FormulaEditor2Activity.class);
		org.catrobat.catroid.formulaeditor.Formula current =
				formulaBrick.getFormulaWithBrickField(field);
		intent.putExtra(
				org.catrobat.catroid.ui.formulaeditor.FormulaEditor2Activity.EXTRA_FORMULA_STRING,
				current == null ? "" : current.getTrimmedFormulaString(getContext()));
		activity.startActivityForResult(intent, 8899);
	}

	private void wireDragToChildren(View parent, BrickTouchDragListener dragListener) {
		if (!(parent instanceof android.view.ViewGroup)) return;
		android.view.ViewGroup group = (android.view.ViewGroup) parent;
		for (int i = 0; i < group.getChildCount(); i++) {
			View child = group.getChildAt(i);
			child.setOnTouchListener((v, event) -> {
				boolean wasDragging = dragListener.isDragActive();
				dragListener.onTouch(v, event);
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) return false;
				return wasDragging || dragListener.isDragActive();
			});
			wireDragToChildren(child, dragListener);
		}
	}

	private void moveScript(Script script, int direction) {
		if (sprite == null || script == null) return;
		List<Script> scripts = sprite.getScriptList();
		int index = scripts.indexOf(script);
		int target = index + direction;
		if (index < 0 || target < 0 || target >= scripts.size()) return;
		snapshot();
		java.util.Collections.swap(scripts, index, target);
		rebuildDeferred();
	}

	private boolean containsBrick(Brick parent, Brick target) {
		if (!(parent instanceof CompositeBrick)) return false;
		CompositeBrick composite = (CompositeBrick) parent;
		for (Brick child : composite.getNestedBricks()) {
			if (child == target || containsBrick(child, target)) return true;
		}
		if (composite.hasSecondaryList()) {
			for (Brick child : composite.getSecondaryNestedBricks()) {
				if (child == target || containsBrick(child, target)) return true;
			}
		}
		return false;
	}

	private void toggleProjectProtection() {
		Project project = ProjectManager.getInstance().getCurrentProject();
		if (project != null && project.getXmlHeader() != null) {
			boolean isProt = project.isProtectedProject();
			project.getXmlHeader().setProtectedProject(!isProt);
			Toast.makeText(getContext(), !isProt ? "Проект защищён от изменений!" : "Защита проекта снята!", Toast.LENGTH_SHORT).show();
		}
	}

	private void showBrickHelp(Brick brick) {
		String brickName = brick.getClass().getSimpleName();
		String helpText = "Тип блока: " + brick.getClass().getName() + "\n\n"
				+ "Этот блок задаёт логическое действие в цепочке скрипта. "
				+ "Поддерживает формулы, переменные, локальные параметры и вычисление условий во время работы игры.";
		new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Справка: " + brickName)
				.setMessage(helpText)
				.setPositiveButton(android.R.string.ok, null)
				.show();
	}

	private List<Brick> getLockGroup(Brick root) {
		List<Brick> group = new ArrayList<>();
		addToLockGroup(group, root);
		if (root instanceof ScriptBrick) {
			Script s = ((ScriptBrick) root).getScript();
			if (s != null) {
				for (Brick b : s.getBrickList()) {
					addToLockGroup(group, b);
				}
			}
		}
		return group;
	}

	private void addToLockGroup(List<Brick> group, Brick brick) {
		if (brick == null || group.contains(brick)) {
			return;
		}
		group.add(brick);
		if (brick instanceof CompositeBrick) {
			CompositeBrick composite = (CompositeBrick) brick;
			for (Brick child : composite.getNestedBricks()) {
				addToLockGroup(group, child);
			}
			if (composite.hasSecondaryList()) {
				for (Brick child : composite.getSecondaryNestedBricks()) {
					addToLockGroup(group, child);
				}
			}
		}
	}

	private boolean isGroupLocked(List<Brick> group) {
		for (Brick b : group) {
			if (b.isLocked()) {
				return true;
			}
		}
		return false;
	}

	private boolean verifyGroup(List<Brick> group, String password) {
		for (Brick b : group) {
			if (b.isLocked() && !b.verifyLock(password)) {
				return false;
			}
		}
		return true;
	}

	private void showLockBrickDialog(Brick brick) {
		List<Brick> group = getLockGroup(brick);
		showPasswordDialog("Заблокировать блок", password -> {
			for (Brick lockedBrick : group) {
				lockedBrick.setLock(password);
			}
			Toast.makeText(getContext(), "Блок заблокирован!", Toast.LENGTH_SHORT).show();
			rebuildDeferred();
		});
	}

	private void unlockBrick(Brick brick) {
		List<Brick> group = getLockGroup(brick);
		if (!isGroupLocked(group)) {
			return;
		}
		showPasswordDialog("Снять блокировку", password -> {
			if (verifyGroup(group, password)) {
				for (Brick lockedBrick : group) {
					lockedBrick.clearLock();
				}
				Toast.makeText(getContext(), "Блок разблокирован!", Toast.LENGTH_SHORT).show();
				rebuildDeferred();
			} else {
				Toast.makeText(getContext(), "Неверный пароль!", Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void showPasswordDialog(String title, java.util.function.Consumer<String> onOk) {
		final EditText input = new EditText(getContext());
		input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
				| android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
		new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(title)
				.setView(input)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					String password = input.getText().toString();
					if (password.isEmpty()) {
						Toast.makeText(getContext(), "Пароль не может быть пустым!", Toast.LENGTH_SHORT).show();
						return;
					}
					onOk.accept(password);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void toggleCommentBrick(Brick brick, View stackView) {
		if (brick != null) {
			snapshot();
			brick.setCommentedOut(!brick.isCommentedOut());
			if (stackView != null && stackView.getTag() instanceof Script) {
				((Script) stackView.getTag()).setParents();
			}
			rebuildDeferred();
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
	private class SelectionBar extends LinearLayout {
		private final TextView countText;

		SelectionBar(Context context) {
			super(context);
			setOrientation(LinearLayout.HORIZONTAL);
			setPadding(dp(10), dp(6), dp(10), dp(6));
			setBackgroundColor(0xF2151D2F);
			countText = new TextView(context);
			countText.setTextColor(0xFFF8FAFC);
			countText.setTextSize(14f);
			countText.setPadding(dp(10), dp(4), dp(10), dp(4));
			addView(countText);
			addSelectionButton("Delete", 0xFFF87171, ScriptCanvasView.this::deleteSelectedBricks);
			addSelectionButton("Copy", 0xFF93C5FD, ScriptCanvasView.this::copySelectedBricks);
			addSelectionButton("Paste", 0xFF86EFAC, ScriptCanvasView.this::pasteSelectedClipboard);
			addSelectionButton("✕", 0xFF94A3B8, ScriptCanvasView.this::exitSelectionMode);
		}

		void update() {
			countText.setText("✓ " + selectedBricks.size());
		}

		private void addSelectionButton(String label, int color, Runnable action) {
			Button btn = new Button(getContext());
			btn.setAllCaps(false);
			btn.setTextSize(12f);
			btn.setTextColor(0xFFF8FAFC);
			btn.setBackgroundColor(color);
			btn.setPadding(dp(10), dp(4), dp(10), dp(4));
			btn.setOnClickListener(v -> action.run());
			addView(btn);
		}
	}

	private void updateSelectionBar() {
		if (selectionBar != null) {
			selectionBar.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
			if (selectionBar instanceof SelectionBar) {
				((SelectionBar) selectionBar).update();
			}
		}
		notifySelection();
	}

	public void enterSelectionMode() {
		selectionMode = true;
		selectedBricks.clear();
		clearSelectedOverlays();
		rebuild();
		updateSelectionBar();
	}

	private void exitSelectionMode() {
		selectionMode = false;
		selectedBricks.clear();
		clearSelectedOverlays();
		rebuild();
		updateSelectionBar();
	}

	public boolean isSelectionMode() { return selectionMode; }

	private void selectBrick(Brick brick) {
		if (brick == null || brick instanceof ScriptBrick || brick instanceof EndBrick) {
			return;
		}
		if (!selectionMode) {
			return;
		}
		snapshot();
		if (selectedBricks.contains(brick)) {
			selectedBricks.remove(brick);
		} else {
			selectedBricks.add(brick);
		}
		rebuild();
		updateSelectionBar();
	}

	private void collectSelectedBricksRecursive(Brick brick, List<Brick> out) {
		if (brick == null) return;
		if (selectedBricks.contains(brick)) {
			out.add(brick);
			return;
		}
		if (brick instanceof CompositeBrick) {
			CompositeBrick composite = (CompositeBrick) brick;
			for (Brick child : composite.getNestedBricks()) {
				collectSelectedBricksRecursive(child, out);
			}
			if (composite.hasSecondaryList()) {
				for (Brick child : composite.getSecondaryNestedBricks()) {
					collectSelectedBricksRecursive(child, out);
				}
			}
		}
	}

	private List<Brick> collectSelection() {
		List<Brick> bricksToDelete = new ArrayList<>();
		for (Brick brick : selectedBricks) {
			boolean covered = false;
			for (Brick other : selectedBricks) {
				if (other != brick && containsBrick(other, brick)) {
					covered = true;
					break;
				}
			}
			if (!covered) {
				bricksToDelete.add(brick);
			}
		}
		return bricksToDelete;
	}

	private void deleteSelectedBricks() {
		if (selectedBricks.isEmpty()) return;
		if (ProjectManager.getInstance().getCurrentProject() != null
				&& ProjectManager.getInstance().getCurrentProject().isProtectedProject()) {
			Toast.makeText(getContext(), R.string.protected_project_cannot_edit, Toast.LENGTH_SHORT).show();
			return;
		}
		for (Brick brick : selectedBricks) {
			if (brick.isLocked()) {
				Toast.makeText(getContext(), R.string.brick_locked, Toast.LENGTH_SHORT).show();
				return;
			}
		}
		snapshot();
		List<Brick> toDelete = collectSelection();
		for (Brick brick : toDelete) {
			FloatingBrick detached = findDetachedBrick(brick);
			if (detached != null) {
				detachedBricks.remove(detached);
				continue;
			}
			if (brick instanceof ScriptBrick) {
				sprite.getScriptList().remove(((ScriptBrick) brick).getScript());
				continue;
			}
			for (Script script : sprite.getScriptList()) {
				if (removeBrickRecursive(script.getBrickList(), brick)) {
					break;
				}
			}
		}
		for (Script script : sprite.getScriptList()) {
			script.setParents();
		}
		selectedBricks.clear();
		clearSelectedOverlays();
		rebuild();
		updateSelectionBar();
	}

	private void copySelectedBricks() {
		if (selectedBricks.isEmpty()) return;
		snapshot();
		List<Brick> toCopy = collectSelection();
		copiedBrickClipboard = null;
		List<Brick> clipboard = new ArrayList<>();
		for (Brick brick : toCopy) {
			try {
				Brick clone = brick.clone();
				clipboard.add(clone);
				FloatingBrick detached = findDetachedBrick(brick);
				if (detached != null) {
					detachedBricks.add(new FloatingBrick(clone, detached.x + dp(16), detached.y + dp(16)));
					continue;
				}
				Script script = null;
				for (Script s : sprite.getScriptList()) {
					if (findListContainingBrick(s, brick) != null || s.getBrickList().contains(brick)) {
						script = s;
						break;
					}
				}
				if (script == null) continue;
				List<Brick> parentList = findListContainingBrick(script, brick);
				if (parentList == null || brick instanceof ScriptBrick) {
					script.addBrick(clone);
				} else {
					int index = parentList.indexOf(brick);
					parentList.add(index < 0 ? parentList.size() : index + 1, clone);
				}
			} catch (CloneNotSupportedException e) {
				Log.e("ScriptCanvasView", "Failed to clone selected brick", e);
			}
		}
		for (Script script : sprite.getScriptList()) {
			script.setParents();
		}
		copiedSelectionClipboard = clipboard;
		rebuild();
		Toast.makeText(getContext(), "Скопировано блоков: " + toCopy.size(), Toast.LENGTH_SHORT).show();
	}

	private static List<Brick> copiedSelectionClipboard = null;

	private void pasteSelectedClipboard() {
		if (copiedSelectionClipboard == null || copiedSelectionClipboard.isEmpty()) return;
		if (ProjectManager.getInstance().getCurrentProject() != null
				&& ProjectManager.getInstance().getCurrentProject().isProtectedProject()) {
			Toast.makeText(getContext(), R.string.protected_project_cannot_edit, Toast.LENGTH_SHORT).show();
			return;
		}
		snapshot();
		for (Brick template : copiedSelectionClipboard) {
			try {
				Brick clone = template.clone();
				if (clone instanceof ScriptBrick) {
					continue;
				}
				detachedBricks.add(new FloatingBrick(clone,
						dp(80) + (float)(Math.random() * 60), dp(80) + (float)(Math.random() * 60)));
			} catch (CloneNotSupportedException e) {
			}
		}
		rebuild();
		Toast.makeText(getContext(), "Вставлено блоков: " + copiedSelectionClipboard.size(), Toast.LENGTH_SHORT).show();
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
