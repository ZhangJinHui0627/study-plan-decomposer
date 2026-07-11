package com.example.studyplan;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.RoundedCorner;
import android.view.animation.LinearInterpolator;

public class ScreenGlowFrameView extends View {
    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float rotationAngle;
    private final Matrix gradientMatrix = new Matrix();

    public ScreenGlowFrameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        framePaint.setStyle(Paint.Style.STROKE);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(16000L);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            rotationAngle = (float) animation.getAnimatedValue();
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
        float inset = dp(7f);
        RectF bounds = new RectF(inset, inset, getWidth() - inset, getHeight() - inset);
        float radius = getDeviceCornerRadius();

        // Breathing sine wave: oscillates between 0.3 and 0.85 alpha
        double breathAngle = Math.toRadians(rotationAngle);
        float breathAlpha = 0.575f + 0.275f * (float) Math.sin(breathAngle * 2.5);
        breathAlpha = Math.max(0.3f, Math.min(0.85f, breathAlpha));

        // Shadow blur breathing: 8dp to 14dp
        float blurDp = 11f + 3f * (float) Math.sin(breathAngle * 3.0);
        blurDp = Math.max(8f, Math.min(14f, blurDp));

        // SweepGradient: blue #1A73E8 to cyan-blue #00D2FF
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int[] sweepColors = {Color.parseColor("#1A73E8"), Color.parseColor("#00D2FF"), Color.parseColor("#1A73E8")};
        float[] sweepPositions = {0f, 0.5f, 1f};
        SweepGradient sweep = new SweepGradient(centerX, centerY, sweepColors, sweepPositions);
        gradientMatrix.reset();
        gradientMatrix.postRotate(rotationAngle, centerX, centerY);
        sweep.setLocalMatrix(gradientMatrix);

        framePaint.setShader(sweep);
        framePaint.setStrokeWidth(dp(3f));
        framePaint.setAlpha((int) (breathAlpha * 255));
        framePaint.setShadowLayer(dp(blurDp), 0f, 0f,
                Color.argb((int) (breathAlpha * 180), 26, 115, 232));

        Path borderPath = new Path();
        borderPath.addRoundRect(bounds, radius, radius, Path.Direction.CW);
        canvas.drawPath(borderPath, framePaint);
    }

    private float getDeviceCornerRadius() {
        float radius = dp(24f);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.view.WindowInsets insets = getRootWindowInsets();
            if (insets != null) {
                int[] positions = {
                        RoundedCorner.POSITION_TOP_LEFT, RoundedCorner.POSITION_TOP_RIGHT,
                        RoundedCorner.POSITION_BOTTOM_LEFT, RoundedCorner.POSITION_BOTTOM_RIGHT
                };
                for (int position : positions) {
                    RoundedCorner corner = insets.getRoundedCorner(position);
                    if (corner != null) radius = Math.max(radius, corner.getRadius());
                }
            }
        }
        return Math.min(radius, Math.min(getWidth(), getHeight()) / 2f - dp(7f));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
