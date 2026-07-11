package com.example.studyplan;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class AvatarFrameView extends View {
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] rainbowColors = {
            0xFFFF5F6D, 0xFFFFC371, 0xFFFFE66D, 0xFF7BE495,
            0xFF56CCF2, 0xFF8E7CFF, 0xFFFF6FD8
    };
    private ValueAnimator animator;
    private float rotation;
    private float pulse = 0.5f;
    private int accentColor = Color.TRANSPARENT;
    private boolean fallback;

    public AvatarFrameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ringPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStyle(Paint.Style.STROKE);
        setWillNotDraw(false);
    }

    public void setAccentColor(int color) {
        accentColor = color == Color.WHITE ? Color.TRANSPARENT : color;
        invalidate();
    }

    public void setFallback(boolean value) {
        fallback = value;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(6500L);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            rotation = progress * 360f;
            pulse = 0.5f + 0.5f * (float) Math.sin(progress * Math.PI * 2f);
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        // Keep the inner edge of the 6dp ring flush with the 80dp avatar edge.
        float radius = Math.min(getWidth(), getHeight()) / 2f - dp(2f);
        if (fallback) {
            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setColor(getResources().getColor(R.color.primary));
            canvas.drawCircle(cx, cy, radius - dp(3f), fill);
        }

        int[] colors;
        if (accentColor == Color.TRANSPARENT) {
            colors = rainbowColors;
        } else {
            colors = new int[]{
                    Color.argb(110, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
                    accentColor,
                    Color.argb(230, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
            };
        }
        SweepGradient gradient = new SweepGradient(cx, cy, colors, null);
        ringPaint.setShader(gradient);
        ringPaint.setStrokeWidth(dp(6f));
        canvas.save();
        canvas.rotate(rotation, cx, cy);
        canvas.drawCircle(cx, cy, radius, ringPaint);
        canvas.restore();

        glowPaint.setShader(null);
        glowPaint.setColor(Color.argb((int) (45 + 85 * pulse), 255, 255, 255));
        glowPaint.setStrokeWidth(dp(2f) + dp(2f) * pulse);
        canvas.drawCircle(cx, cy, radius - dp(4f), glowPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
