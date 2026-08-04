package org.catrobat.catroid.ui.sceneeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;

import java.util.List;

public class FloatingDebugWindow extends FrameLayout {

	private final Project project;
	private final Handler updateHandler = new Handler(Looper.getMainLooper());
	private final LinearLayout debugContainer;
	private boolean isMinimized = false;
	private View bodyView;
	private int savedHeightPx;

	private final Runnable updateRunnable = new Runnable() {
		@Override
		public void run() {
			refreshDebugValues();
			updateHandler.postDelayed(this, 500);
		}
	};

	@SuppressLint("ClickableViewAccessibility")
	public FloatingDebugWindow(Context context) {
		super(context);
		this.project = ProjectManager.getInstance().getCurrentProject();

		float density = getResources().getDisplayMetrics().density;
		int dp10 = Math.round(10 * density);
		int dp12 = Math.round(12 * density);

		setElevation(dp10);
		setOutlineProvider(android.view.ViewOutlineProvider.BOUNDS);
		setBackgroundResource(R.drawable.bg_object_card_cube);

		LinearLayout rootLayout = new LinearLayout(context);
		rootLayout.setOrientation(LinearLayout.VERTICAL);
		rootLayout.setPadding(dp10, dp10, dp10, dp10);

		LinearLayout header = new LinearLayout(context);
		header.setOrientation(LinearLayout.HORIZONTAL);
		header.setGravity(Gravity.CENTER_VERTICAL);
		header.setBackgroundColor(0xFF1E293B);
		header.setPadding(dp10, dp10, dp10, dp10);

		TextView title = new TextView(context);
		title.setText("Отладчик 2.0 (Переменные)");
		title.setTextColor(0xFF94A3B8);
		title.setTextSize(14f);
		title.setTypeface(null, Typeface.BOLD);
		LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		title.setLayoutParams(titleLp);
		header.addView(title);

		TextView minBtn = new TextView(context);
		minBtn.setText(" _ ");
		minBtn.setTextColor(Color.WHITE);
		minBtn.setTextSize(16f);
		minBtn.setTypeface(null, Typeface.BOLD);
		minBtn.setOnClickListener(v -> toggleMinimize());
		header.addView(minBtn);

		TextView closeBtn = new TextView(context);
		closeBtn.setText(" ✕ ");
		closeBtn.setTextColor(0xFFEF4444);
		closeBtn.setTextSize(16f);
		closeBtn.setTypeface(null, Typeface.BOLD);
		closeBtn.setOnClickListener(v -> close());
		header.addView(closeBtn);

		header.setOnTouchListener(new DragListener());
		rootLayout.addView(header);

		ScrollView scrollView = new ScrollView(context);
		debugContainer = new LinearLayout(context);
		debugContainer.setOrientation(LinearLayout.VERTICAL);
		debugContainer.setPadding(0, dp10, 0, 0);
		scrollView.addView(debugContainer);

		bodyView = scrollView;
		rootLayout.addView(scrollView, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, Math.round(220 * density)));

		addView(rootLayout);

		updateHandler.post(updateRunnable);
	}

	private void refreshDebugValues() {
		debugContainer.removeAllViews();
		if (project == null) return;

		float density = getResources().getDisplayMetrics().density;
		int dp6 = Math.round(6 * density);

		TextView vHeader = new TextView(getContext());
		vHeader.setText("Глобальные Переменные:");
		vHeader.setTextColor(0xFFF8FAFC);
		vHeader.setTypeface(null, Typeface.BOLD);
		vHeader.setTextSize(13f);
		debugContainer.addView(vHeader);

		List<UserVariable> vars = project.getUserVariables();
		if (vars == null || vars.isEmpty()) {
			TextView emptyTv = new TextView(getContext());
			emptyTv.setText("  (Нет переменных)");
			emptyTv.setTextColor(0xFF64748B);
			emptyTv.setTextSize(12f);
			debugContainer.addView(emptyTv);
		} else {
			for (UserVariable uv : vars) {
				TextView tv = new TextView(getContext());
				Object val = uv.getValue();
				tv.setText("  • " + uv.getName() + " = " + (val != null ? val.toString() : "0"));
				tv.setTextColor(0xFF94A3B8);
				tv.setTextSize(13f);
				tv.setPadding(0, dp6, 0, dp6);
				debugContainer.addView(tv);
			}
		}

		TextView lHeader = new TextView(getContext());
		lHeader.setText("Глобальные Списки:");
		lHeader.setTextColor(0xFFF8FAFC);
		lHeader.setTypeface(null, Typeface.BOLD);
		lHeader.setTextSize(13f);
		lHeader.setPadding(0, dp6, 0, 0);
		debugContainer.addView(lHeader);

		List<UserList> lists = project.getUserLists();
		if (lists == null || lists.isEmpty()) {
			TextView emptyL = new TextView(getContext());
			emptyL.setText("  (Нет списков)");
			emptyL.setTextColor(0xFF64748B);
			emptyL.setTextSize(12f);
			debugContainer.addView(emptyL);
		} else {
			for (UserList ul : lists) {
				TextView tv = new TextView(getContext());
				List<?> items = ul.getValue();
				tv.setText("  • " + ul.getName() + " [" + (items != null ? items.size() : 0) + " элементов]");
				tv.setTextColor(0xFFA7F3D0);
				tv.setTextSize(13f);
				tv.setPadding(0, dp6, 0, dp6);
				debugContainer.addView(tv);
			}
		}
	}

	private void toggleMinimize() {
		isMinimized = !isMinimized;
		ViewGroup.LayoutParams lp = getLayoutParams();
		if (isMinimized) {
			savedHeightPx = lp.height;
			bodyView.setVisibility(GONE);
		} else {
			bodyView.setVisibility(VISIBLE);
		}
	}

	public void close() {
		stopUpdates();
		if (getParent() instanceof ViewGroup) {
			((ViewGroup) getParent()).removeView(this);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		stopUpdates();
		super.onDetachedFromWindow();
	}

	private void stopUpdates() {
		updateHandler.removeCallbacks(updateRunnable);
	}

	private class DragListener implements OnTouchListener {
		private float downX;
		private float downY;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					downX = event.getRawX() - getTranslationX();
					downY = event.getRawY() - getTranslationY();
					return true;
				case MotionEvent.ACTION_MOVE:
					setTranslationX(event.getRawX() - downX);
					setTranslationY(event.getRawY() - downY);
					return true;
				default:
					return false;
			}
		}
	}
}
