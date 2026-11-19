package com.gurusainathp.movieed.ui.diary;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.HorizontalScrollView;

public class NestedHorizontalScrollView extends HorizontalScrollView {

    private float startX;
    private float startY;
    private final int touchSlop;

    public NestedHorizontalScrollView(Context context) {
        this(context, null);
    }

    public NestedHorizontalScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NestedHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - startX);
                float dy = Math.abs(ev.getY() - startY);

                if (dx > touchSlop && dx > dy) {
                    boolean draggingLeft = (ev.getX() - startX) < 0;
                    boolean canScrollLeft = canScrollHorizontally(1);
                    boolean canScrollRight = canScrollHorizontally(-1);

                    if ((draggingLeft && canScrollLeft) || (!draggingLeft && canScrollRight)) {
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    } else {
                        // Reached scroll edge: allow parent ViewPager2 to handle page swipe
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
}
