package com.gurusainathp.movieed.ui.profile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gurusainathp.movieed.R;

public class MonthlyBarChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();

    private final String[] monthLabels = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private int[] monthlyCounts = new int[12];
    private int maxCount = 1;

    public MonthlyBarChartView(Context context) {
        this(context, null);
    }

    public MonthlyBarChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MonthlyBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        barPaint.setColor(ContextCompat.getColor(context, R.color.movieed_primary));
        barPaint.setStyle(Paint.Style.FILL);

        trackPaint.setColor(ContextCompat.getColor(context, R.color.movieed_surface_elevated));
        trackPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(ContextCompat.getColor(context, R.color.movieed_text_secondary));
        textPaint.setTextSize(spToPx(10));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setMonthlyData(int[] counts) {
        if (counts != null && counts.length == 12) {
            this.monthlyCounts = counts;
            int max = 0;
            for (int c : counts) {
                if (c > max) max = c;
            }
            this.maxCount = Math.max(1, max);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float bottomTextHeight = dpToPx(16);
        float chartHeight = height - bottomTextHeight - dpToPx(8);
        float itemWidth = (float) width / 12f;
        float barWidth = itemWidth * 0.45f;
        float cornerRadius = dpToPx(4);

        for (int i = 0; i < 12; i++) {
            float centerX = (i * itemWidth) + (itemWidth / 2f);
            float left = centerX - (barWidth / 2f);
            float right = centerX + (barWidth / 2f);
            float topMax = dpToPx(8);
            float bottom = topMax + chartHeight;

            // Draw track
            rectF.set(left, topMax, right, bottom);
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, trackPaint);

            // Draw active bar
            int count = monthlyCounts[i];
            if (count > 0) {
                float fillHeight = (float) count / maxCount * chartHeight;
                float barTop = bottom - fillHeight;
                rectF.set(left, barTop, right, bottom);
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, barPaint);
            }

            // Draw month label
            canvas.drawText(monthLabels[i], centerX, height - dpToPx(2), textPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
