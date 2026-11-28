package com.gurusainathp.movieed.ui.person;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Hairline divider between list rows (none after the last row), starting a fixed distance
 * from the row's left edge so it lines up with the text instead of the poster.
 */
public class ListDividerDecoration extends RecyclerView.ItemDecoration {

    private final Paint paint = new Paint();
    private final int thickness;
    private final int startInset;

    public ListDividerDecoration(@NonNull Context context, @ColorRes int colorRes, float startInsetDp) {
        float density = context.getResources().getDisplayMetrics().density;
        paint.setColor(ContextCompat.getColor(context, colorRes));
        thickness = Math.max(1, Math.round(density));          // 1dp
        startInset = Math.round(startInsetDp * density);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        boolean notLast = position != RecyclerView.NO_POSITION && position < state.getItemCount() - 1;
        outRect.bottom = notLast ? thickness : 0;
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);
            if (position == RecyclerView.NO_POSITION || position >= state.getItemCount() - 1)
                continue;

            float top = child.getBottom() + child.getTranslationY();
            c.drawRect(child.getLeft() + startInset, top,
                    child.getRight(), top + thickness, paint);
        }
    }
}