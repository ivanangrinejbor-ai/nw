package org.catrobat.catroid.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import org.catrobat.catroid.raptor.ParticleCurvePoint;

import java.util.List;


public class GradientPreviewView extends View {

    private Paint paint;
    private List<ParticleCurvePoint<com.badlogic.gdx.graphics.Color>> points;

    public GradientPreviewView(Context context, List<ParticleCurvePoint<com.badlogic.gdx.graphics.Color>> points) {
        super(context);
        this.points = points;
        init();
    }

    public GradientPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }

    public void setPoints(List<ParticleCurvePoint<com.badlogic.gdx.graphics.Color>> points) {
        this.points = points;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (points == null || points.isEmpty()) {
            paint.setColor(android.graphics.Color.WHITE);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            return;
        }

        if (points.size() == 1) {
            com.badlogic.gdx.graphics.Color c = points.get(0).value;
            paint.setColor(gdxToAndroid(c));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            return;
        }


        int count = points.size();
        int[] colors = new int[count];
        float[] positions = new float[count];

        for (int i = 0; i < count; i++) {
            colors[i] = gdxToAndroid(points.get(i).value);
            positions[i] = Math.max(0f, Math.min(1f, points.get(i).time));
        }


        for (int i = 1; i < count; i++) {
            if (positions[i] <= positions[i - 1]) {
                positions[i] = positions[i - 1] + 0.001f;
            }
        }

        LinearGradient gradient = new LinearGradient(
                0, 0, getWidth(), 0,
                colors, positions,
                Shader.TileMode.CLAMP);

        paint.setShader(gradient);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);



    }

    private int gdxToAndroid(com.badlogic.gdx.graphics.Color c) {
        return android.graphics.Color.argb(
                (int)(c.a * 255),
                (int)(c.r * 255),
                (int)(c.g * 255),
                (int)(c.b * 255));
    }
}