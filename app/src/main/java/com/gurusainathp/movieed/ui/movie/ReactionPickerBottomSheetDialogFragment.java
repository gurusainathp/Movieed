package com.gurusainathp.movieed.ui.movie;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.gurusainathp.movieed.R;

public class ReactionPickerBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public interface OnReactionSelectedListener {
        void onReactionSelected(String reaction);
        void onReactionCleared();
    }

    private OnReactionSelectedListener listener;
    private String currentReaction;

    public static ReactionPickerBottomSheetDialogFragment newInstance(String currentReaction) {
        ReactionPickerBottomSheetDialogFragment fragment = new ReactionPickerBottomSheetDialogFragment();
        fragment.currentReaction = currentReaction;
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

        view.findViewById(R.id.btn_reaction_love).setOnClickListener(v -> selectReaction("LOVE"));
        view.findViewById(R.id.btn_reaction_like).setOnClickListener(v -> selectReaction("LIKE"));
        view.findViewById(R.id.btn_reaction_umm).setOnClickListener(v -> selectReaction("UMM"));
        view.findViewById(R.id.btn_reaction_dislike).setOnClickListener(v -> selectReaction("DISLIKE"));
        view.findViewById(R.id.btn_reaction_hate).setOnClickListener(v -> selectReaction("HATE"));

        Button btnClear = view.findViewById(R.id.btn_clear_reaction);
        if (currentReaction != null && !currentReaction.isEmpty()) {
            btnClear.setVisibility(View.VISIBLE);
            btnClear.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReactionCleared();
                }
                dismiss();
            });
        } else {
            btnClear.setVisibility(View.GONE);
        }
    }

    private void selectReaction(String reaction) {
        if (listener != null) {
            listener.onReactionSelected(reaction);
        }
        dismiss();
    }
}
