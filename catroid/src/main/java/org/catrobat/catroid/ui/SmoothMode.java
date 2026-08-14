/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package org.catrobat.catroid.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.SharedPreferenceKeys;

import java.util.WeakHashMap;

public final class SmoothMode {
	private static final WeakHashMap<View, android.graphics.drawable.Drawable> ORIGINAL_BACKGROUNDS =
			new WeakHashMap<>();
	private static final WeakHashMap<MaterialCardView, CardAppearance> ORIGINAL_CARDS = new WeakHashMap<>();
	private static final WeakHashMap<TextView, ColorStateList> ORIGINAL_TEXT_COLORS = new WeakHashMap<>();
	private static final WeakHashMap<Toolbar, ColorStateList> ORIGINAL_TOOLBAR_TITLES = new WeakHashMap<>();

	private SmoothMode() {
	}

	public static void apply(Activity activity) {
		Context context = activity.getApplicationContext();
		boolean enabled = PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(SharedPreferenceKeys.REDRAWN_BRICK_MODE_PREFERENCE_KEY, false);
		boolean smoothProjectSurface = enabled && activity instanceof ProjectListActivity;
		activity.getWindow().setBackgroundDrawableResource(
				smoothProjectSurface ? R.color.smooth_background : R.color.app_background);
		applyToView(activity.getWindow().getDecorView(), smoothProjectSurface);
	}

	private static void applyToView(View view, boolean enabled) {
		if (enabled) {
			applyEnabledAppearance(view);
		} else {
			restoreAppearance(view);
		}

		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			for (int index = 0; index < group.getChildCount(); index++) {
				applyToView(group.getChildAt(index), enabled);
			}
		}
	}

	private static void applyEnabledAppearance(View view) {
		Object tag = view.getTag();
		if ("toolbar_background".equals(tag)) {
			saveBackground(view);
			view.setBackgroundColor(view.getResources().getColor(R.color.smooth_toolbar));
		}
		if ("app_background".equals(tag)) {
			saveBackground(view);
			view.setBackgroundColor(view.getResources().getColor(R.color.smooth_background));
		}
		if ("button_background".equals(tag)) {
			saveBackground(view);
			view.setBackground(createSurface(view));
		}

		if (view instanceof Toolbar) {
			Toolbar toolbar = (Toolbar) view;
			if (!ORIGINAL_TOOLBAR_TITLES.containsKey(toolbar)) {
				ORIGINAL_TOOLBAR_TITLES.put(toolbar,
						ColorStateList.valueOf(view.getResources().getColor(R.color.toolbar_title)));
			}
			toolbar.setBackgroundColor(view.getResources().getColor(R.color.smooth_toolbar));
			toolbar.setTitleTextColor(view.getResources().getColor(R.color.smooth_toolbar_text));
		}
		if (view instanceof RecyclerView) {
			saveBackground(view);
			view.setBackgroundColor(view.getResources().getColor(R.color.smooth_background));
		}
		if (view instanceof MaterialCardView) {
			MaterialCardView card = (MaterialCardView) view;
			if (!ORIGINAL_CARDS.containsKey(card)) {
				ORIGINAL_CARDS.put(card, new CardAppearance(card));
			}
			card.setCardBackgroundColor(view.getResources().getColor(R.color.smooth_card));
			card.setStrokeColor(ColorStateList.valueOf(view.getResources().getColor(R.color.smooth_border)));
			card.setStrokeWidth(view.getResources().getDimensionPixelSize(R.dimen.smooth_border_width));
			card.setRadius(view.getResources().getDimension(R.dimen.smooth_corner_radius));
			card.setCardElevation(0);
		}
		if (view instanceof TextView && view.getId() == R.id.title_view) {
			saveTextColor((TextView) view);
			((TextView) view).setTextColor(view.getResources().getColor(R.color.smooth_text));
		}
		if (view instanceof TextView && view.getId() == R.id.details_view) {
			saveTextColor((TextView) view);
			((TextView) view).setTextColor(view.getResources().getColor(R.color.smooth_text_secondary));
		}
	}

	private static void saveBackground(View view) {
		if (!ORIGINAL_BACKGROUNDS.containsKey(view)) {
			android.graphics.drawable.Drawable background = view.getBackground();
			if (background != null && background.getConstantState() != null) {
				background = background.getConstantState().newDrawable(view.getResources());
			}
			ORIGINAL_BACKGROUNDS.put(view, background);
		}
	}

	private static void saveTextColor(TextView view) {
		if (!ORIGINAL_TEXT_COLORS.containsKey(view)) {
			ORIGINAL_TEXT_COLORS.put(view, view.getTextColors());
		}
	}

	private static void restoreAppearance(View view) {
		if (ORIGINAL_BACKGROUNDS.containsKey(view)) {
			view.setBackground(ORIGINAL_BACKGROUNDS.remove(view));
		}
		if (view instanceof Toolbar) {
			Toolbar toolbar = (Toolbar) view;
			ColorStateList titleColors = ORIGINAL_TOOLBAR_TITLES.remove(toolbar);
			if (titleColors != null) {
				toolbar.setTitleTextColor(titleColors);
			}
		}
		if (view instanceof MaterialCardView) {
			MaterialCardView card = (MaterialCardView) view;
			CardAppearance appearance = ORIGINAL_CARDS.remove(card);
			if (appearance != null) {
				appearance.restore(card);
			}
		}
		if (view instanceof TextView) {
			ColorStateList textColors = ORIGINAL_TEXT_COLORS.remove(view);
			if (textColors != null) {
				((TextView) view).setTextColor(textColors);
			}
		}
	}

	private static final class CardAppearance {
		private final ColorStateList backgroundColor;
		private final ColorStateList strokeColor;
		private final int strokeWidth;
		private final float radius;
		private final float elevation;

		private CardAppearance(MaterialCardView card) {
			backgroundColor = card.getCardBackgroundColor();
			strokeColor = ColorStateList.valueOf(card.getStrokeColor());
			strokeWidth = card.getStrokeWidth();
			radius = card.getRadius();
			elevation = card.getCardElevation();
		}

		private void restore(MaterialCardView card) {
			card.setCardBackgroundColor(backgroundColor);
			card.setStrokeColor(strokeColor);
			card.setStrokeWidth(strokeWidth);
			card.setRadius(radius);
			card.setCardElevation(elevation);
		}
	}

	private static GradientDrawable createSurface(View view) {
		GradientDrawable drawable = new GradientDrawable();
		drawable.setColor(view.getResources().getColor(R.color.smooth_card));
		drawable.setCornerRadius(view.getResources().getDimension(R.dimen.smooth_corner_radius));
		drawable.setStroke(view.getResources().getDimensionPixelSize(R.dimen.smooth_border_width),
				view.getResources().getColor(R.color.smooth_border));
		return drawable;
	}
}
