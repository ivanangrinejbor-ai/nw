package org.catrobat.catroid.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.catrobat.catroid.raptor.ParticleCurvePoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CurveEditorView extends View {

    private List<ParticleCurvePoint<Float>> points;
    private Paint linePaint, pointPaint, gridPaint, bgPaint;
    private int selectedIndex = -1;
    private Runnable onUpdateListener;


    private float minVal = 0f;
    private float maxVal = 1f;

    public CurveEditorView(Context context) { super(context); init(); }
    public CurveEditorView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(Color.GREEN);
        linePaint.setStrokeWidth(5f);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint = new Paint();
        pointPaint.setColor(Color.WHITE);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(0x40FFFFFF);
        gridPaint.setStrokeWidth(2f);

        bgPaint = new Paint();
        bgPaint.setColor(0xFF202020);
    }

    public void setData(List<ParticleCurvePoint<Float>> points, float maxExpectedValue, Runnable onUpdate) {
        this.points = points;
        this.maxVal = Math.max(1f, maxExpectedValue);
        this.onUpdateListener = onUpdate;
        invalidate();
    }

    public void setMaxVal(float maxVal) {
        this.maxVal = Math.max(0.1f, maxVal);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);


        float h = getHeight();
        float w = getWidth();
        canvas.drawLine(0, h/2, w, h/2, gridPaint);
        canvas.drawLine(w/2, 0, w/2, h, gridPaint);

        if (points == null || points.isEmpty()) return;


        for (int i = 0; i < points.size() - 1; i++) {
            float x1 = mapX(points.get(i).time);
            float y1 = mapY(points.get(i).value);
            float x2 = mapX(points.get(i+1).time);
            float y2 = mapY(points.get(i+1).value);
            canvas.drawLine(x1, y1, x2, y2, linePaint);
        }


        for (int i = 0; i < points.size(); i++) {
            float x = mapX(points.get(i).time);
            float y = mapY(points.get(i).value);


            if (i == selectedIndex) {
                pointPaint.setColor(Color.YELLOW);
                canvas.drawCircle(x, y, 16, pointPaint);
            } else {
                pointPaint.setColor(Color.WHITE);
                canvas.drawCircle(x, y, 12, pointPaint);
            }
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (points == null) return false;

        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // ВАЖНО: Запрещаем родительскому ScrollView перехватывать касания
                getParent().requestDisallowInterceptTouchEvent(true);

                // Ищем ближайшую точку
                float minDist = 80f; // Чуть увеличил радиус для удобства
                selectedIndex = -1;
                for (int i = 0; i < points.size(); i++) {
                    float px = mapX(points.get(i).time);
                    float py = mapY(points.get(i).value);
                    float dist = (float) Math.hypot(touchX - px, touchY - py);
                    if (dist < minDist) {
                        minDist = dist;
                        selectedIndex = i;
                    }
                }
                if (selectedIndex != -1) invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (selectedIndex != -1) {
                    // Продолжаем запрещать перехват при движении
                    getParent().requestDisallowInterceptTouchEvent(true);

                    float newTime = touchX / getWidth();
                    float newVal = 1f - (touchY / getHeight()); // Инверсия Y
                    newVal *= maxVal; // Масштабируем

                    newTime = Math.max(0f, Math.min(1f, newTime));

                    // Не даем значению уйти ниже 0 (если это не турбулентность, но пока ограничим 0)
                    // Можно убрать Math.max(0, ...), если нужны отрицательные значения
                    // newVal = Math.max(0f, newVal);

                    points.get(selectedIndex).time = newTime;
                    points.get(selectedIndex).value = newVal;

                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Разрешаем прокрутку обратно
                getParent().requestDisallowInterceptTouchEvent(false);

                if (selectedIndex != -1) {
                    Collections.sort(points, (o1, o2) -> Float.compare(o1.time, o2.time));
                    selectedIndex = -1;
                    if (onUpdateListener != null) onUpdateListener.run();
                    invalidate();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private float mapX(float time) {
        return time * getWidth();
    }

    private float mapY(float val) {

        float norm = val / maxVal;

        return getHeight() * (1f - norm);
    }
}