package org.catrobat.catroid.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

public class ThemeEngine {

    public static final String THEME_PREFS_NAME = "live_theme_settings";

    public static void applyTheme(Activity activity) {
        Log.d("ThemeEngine", "--- Применение темы для " + activity.getClass().getSimpleName() + " ---");
        View decorView = activity.getWindow().getDecorView();
        ViewGroup root = (ViewGroup) decorView;
        if (root == null) {
            Log.d("ThemeEngine", "Root view не найден, выход.");
            return;
        }

        SharedPreferences themePrefs = activity.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE);
        Log.d("ThemeEngine", "Читаем из файла: " + THEME_PREFS_NAME);

        themeView(root, themePrefs);
    }

    private static void themeView(View view, SharedPreferences themePrefs) {
        if (view.getTag() instanceof String) {
            String tag = (String) view.getTag();
            if (tag.endsWith("_background")) {
                Log.d("ThemeEngine", "Найден View с тегом: " + tag);


                String colorString = themePrefs.getString(tag, null);
                Log.d("ThemeEngine", "Для ключа '" + tag + "' найдено значение (String): " + colorString);

                if (colorString != null) {
                    try {

                        int color = Color.parseColor(colorString);
                        Log.d("ThemeEngine", "Цвет успешно распарсен: " + color);

                        if (tag.endsWith("_background")) {
                            view.setBackgroundColor(color);
                        } else if (tag.endsWith("_text")) {
                            if (view instanceof android.widget.TextView) {
                                ((android.widget.TextView) view).setTextColor(color);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e("ThemeEngine", "Неверный формат цвета в настройках: " + colorString);

                    }
                } else {
                    Log.d("ThemeEngine", "Значение не найдено или пустое, цвет не применяется.");
                }

            }
        }


        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                themeView(viewGroup.getChildAt(i), themePrefs);
            }
        }
    }
}