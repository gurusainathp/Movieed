package com.gurusainathp.movieed.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gurusainathp.movieed.R;

import java.util.List;

/**
 * Small themed dropdown that opens under a filter chip (shared by the person page and the diary).
 * One row per option: label, optional detail (e.g. a count), and a check mark on the selected one.
 */
public final class FilterDropdown {

    public interface OnSelected {
        void onSelected(int id);
    }

    public static final class Option {
        final int id;
        @NonNull
        final String label;
        @Nullable
        final String detail;
        final boolean selected;
        final boolean dimmed;
        @DrawableRes
        final int iconRes; // 0 = no icon

        public Option(int id, @NonNull String label, @Nullable String detail,
                      boolean selected, boolean dimmed) {
            this(id, label, detail, selected, dimmed, 0);
        }

        public Option(int id, @NonNull String label, @Nullable String detail,
                      boolean selected, boolean dimmed, @DrawableRes int iconRes) {
            this.id = id;
            this.label = label;
            this.detail = detail;
            this.selected = selected;
            this.dimmed = dimmed;
            this.iconRes = iconRes;
        }
    }

    private static final float MIN_WIDTH_DP = 190f;
    private static final float MAX_HEIGHT_DP = 340f;
    private static final float SHADOW_PADDING_DP = 8f;

    private FilterDropdown() {
    }

    public static void show(@NonNull View anchor, @NonNull List<Option> options,
                            @NonNull OnSelected callback) {
        Context ctx = anchor.getContext();
        float density = ctx.getResources().getDisplayMetrics().density;
        LayoutInflater inflater = LayoutInflater.from(ctx);

        View content = inflater.inflate(R.layout.popup_filter_menu, null);
        LinearLayout list = content.findViewById(R.id.layout_filter_options);

        PopupWindow popup = new PopupWindow(content,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(0f);                       // the card draws its own shadow
        popup.setAnimationStyle(R.style.Animation_Movieed_FilterPopup);

        int primary = ContextCompat.getColor(ctx, R.color.movieed_primary);
        int textPrimary = ContextCompat.getColor(ctx, R.color.movieed_text_primary);

        for (Option o : options) {
            View row = inflater.inflate(R.layout.item_filter_option, list, false);
            TextView label = row.findViewById(R.id.tv_option_label);
            TextView detail = row.findViewById(R.id.tv_option_count);
            View check = row.findViewById(R.id.iv_option_check);
            ImageView icon = row.findViewById(R.id.iv_option_icon);

            if (icon != null) {
                if (o.iconRes != 0) {
                    icon.setImageResource(o.iconRes);
                    icon.setVisibility(View.VISIBLE);
                } else {
                    icon.setVisibility(View.GONE);
                }
            }

            label.setText(o.label);
            label.setTextColor(o.selected ? primary : textPrimary);
            label.setTypeface(null, o.selected ? Typeface.BOLD : Typeface.NORMAL);

            if (o.detail != null) {
                detail.setText(o.detail);
                detail.setVisibility(View.VISIBLE);
            } else {
                detail.setVisibility(View.GONE);
            }
            check.setVisibility(o.selected ? View.VISIBLE : View.INVISIBLE);
            row.setAlpha(o.dimmed ? 0.45f : 1f);

            row.setOnClickListener(v -> {
                popup.dismiss();
                callback.onSelected(o.id);
            });
            list.addView(row);
        }

        // Size: at least as wide as the chip, capped in height (the list scrolls beyond that)
        int minWidth = Math.max(anchor.getWidth(), Math.round(MIN_WIDTH_DP * density));
        content.measure(
                View.MeasureSpec.makeMeasureSpec(ctx.getResources().getDisplayMetrics().widthPixels,
                        View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int pad = Math.round(SHADOW_PADDING_DP * density * 2);
        popup.setWidth(Math.max(content.getMeasuredWidth(), minWidth + pad));
        int maxHeight = Math.round(MAX_HEIGHT_DP * density) + pad;
        if (content.getMeasuredHeight() > maxHeight) popup.setHeight(maxHeight);

        // Shift left/up by the shadow padding so the card lines up with the chip
        popup.showAsDropDown(anchor,
                -Math.round(SHADOW_PADDING_DP * density),
                -Math.round(2 * density));
    }
}
