package com.gurusainathp.movieed.ui.movie;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.gurusainathp.movieed.R;

public class ReactionPickerBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_CURRENT_REACTION = "arg_current_reaction";

    public interface OnReactionSelectedListener {
        void onReactionSelected(String reaction);
        void onReactionCleared();
    }

    private OnReactionSelectedListener listener;
    private String currentReaction;
    private View viewSelectionIndicator;

    public static ReactionPickerBottomSheetDialogFragment newInstance(String currentReaction) {
        ReactionPickerBottomSheetDialogFragment fragment = new ReactionPickerBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_REACTION, currentReaction);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnReactionSelectedListener(OnReactionSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_reaction_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            currentReaction = getArguments().getString(ARG_CURRENT_REACTION);
        }

        viewSelectionIndicator = view.findViewById(R.id.view_selection_indicator);

        View btnLove = view.findViewById(R.id.btn_reaction_love);
        View btnLike = view.findViewById(R.id.btn_reaction_like);
        View btnUmm = view.findViewById(R.id.btn_reaction_umm);
        View btnDislike = view.findViewById(R.id.btn_reaction_dislike);
        View btnHate = view.findViewById(R.id.btn_reaction_hate);

        View initialSelectedView = getViewForReaction(view, currentReaction);
        if (initialSelectedView != null) {
            view.post(() -> {
                ViewGroup.LayoutParams lp = viewSelectionIndicator.getLayoutParams();
                lp.width = initialSelectedView.getWidth();
                lp.height = initialSelectedView.getHeight();
                viewSelectionIndicator.setLayoutParams(lp);
                viewSelectionIndicator.setTranslationX(initialSelectedView.getLeft());
                viewSelectionIndicator.setTranslationY(initialSelectedView.getTop());
                viewSelectionIndicator.setVisibility(View.VISIBLE);
            });
        }

        btnLove.setOnClickListener(v -> handleReactionClick("LOVE", v));
        btnLike.setOnClickListener(v -> handleReactionClick("LIKE", v));
        btnUmm.setOnClickListener(v -> handleReactionClick("UMM", v));
        btnDislike.setOnClickListener(v -> handleReactionClick("DISLIKE", v));
        btnHate.setOnClickListener(v -> handleReactionClick("HATE", v));
    }

    private View getViewForReaction(View root, String reaction) {
        if (reaction == null) return null;
        switch (reaction) {
            case "LOVE": return root.findViewById(R.id.btn_reaction_love);
            case "LIKE": return root.findViewById(R.id.btn_reaction_like);
            case "UMM": return root.findViewById(R.id.btn_reaction_umm);
            case "DISLIKE": return root.findViewById(R.id.btn_reaction_dislike);
            case "HATE": return root.findViewById(R.id.btn_reaction_hate);
            default: return null;
        }
    }

    private void handleReactionClick(String reaction, View clickedView) {
        if (listener == null) {
            dismiss();
            return;
        }

        boolean isTappingSameReaction = reaction.equals(currentReaction);

        if (viewSelectionIndicator.getVisibility() == View.VISIBLE) {
            if (isTappingSameReaction) {
                // Fade out indicator if deselecting
                viewSelectionIndicator.animate()
                        .alpha(0f)
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(180)
                        .start();
            } else {
                // Slide indicator smoothly to the clicked button's X position
                ViewGroup.LayoutParams lp = viewSelectionIndicator.getLayoutParams();
                if (lp.width != clickedView.getWidth() || lp.height != clickedView.getHeight()) {
                    lp.width = clickedView.getWidth();
                    lp.height = clickedView.getHeight();
                    viewSelectionIndicator.setLayoutParams(lp);
                }
                viewSelectionIndicator.animate()
                        .translationX(clickedView.getLeft())
                        .translationY(clickedView.getTop())
                        .setDuration(220)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            }
        } else {
            // First time selecting: position and fade/pop in at clicked button
            ViewGroup.LayoutParams lp = viewSelectionIndicator.getLayoutParams();
            lp.width = clickedView.getWidth();
            lp.height = clickedView.getHeight();
            viewSelectionIndicator.setLayoutParams(lp);
            viewSelectionIndicator.setTranslationX(clickedView.getLeft());
            viewSelectionIndicator.setTranslationY(clickedView.getTop());
            viewSelectionIndicator.setAlpha(0f);
            viewSelectionIndicator.setScaleX(0.8f);
            viewSelectionIndicator.setScaleY(0.8f);
            viewSelectionIndicator.setVisibility(View.VISIBLE);
            viewSelectionIndicator.animate()
                    .alpha(1f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(180)
                    .start();
        }

        // Delay to allow the user to see the box sliding to the new position
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isTappingSameReaction) {
                listener.onReactionCleared();
            } else {
                listener.onReactionSelected(reaction);
            }
            dismiss();
        }, 220);
    }
}
