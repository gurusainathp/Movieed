package com.gurusainathp.movieed.ui.movie;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class LogMovieBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public interface OnLogSaveListener {
        void onSave(float rating, String watchedDate, String review, String note);
        void onDelete();
    }

    private RatingBar rbRating;
    private EditText etDate;
    private EditText etReview;
    private EditText etNote;
    private Button btnSave;
    private Button btnDelete;
    private TextView tvTitle;

    private OnLogSaveListener listener;
    private MovieLogEntity existingLog;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static LogMovieBottomSheetDialogFragment newInstance(MovieLogEntity existingLog) {
        LogMovieBottomSheetDialogFragment fragment = new LogMovieBottomSheetDialogFragment();
        fragment.existingLog = existingLog;
        return fragment;
    }

    public void setOnLogSaveListener(OnLogSaveListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_log_movie, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTitle = view.findViewById(R.id.tv_dialog_title);
        rbRating = view.findViewById(R.id.rb_log_rating);
        etDate = view.findViewById(R.id.et_log_date);
        etReview = view.findViewById(R.id.et_log_review);
        etNote = view.findViewById(R.id.et_log_note);
        btnSave = view.findViewById(R.id.btn_save_log);
        btnDelete = view.findViewById(R.id.btn_delete_log);

        etDate.setText(dateFormat.format(calendar.getTime()));
        etDate.setOnClickListener(v -> showDatePicker());

        if (existingLog != null) {
            tvTitle.setText("Edit Log");
            rbRating.setRating(existingLog.getRating());
            if (existingLog.getWatchedDate() != null) {
                etDate.setText(existingLog.getWatchedDate());
            }
            etReview.setText(existingLog.getReview());
            etNote.setText(existingLog.getPrivateNote());
            btnDelete.setVisibility(View.VISIBLE);
        }

        btnSave.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSave(
                        rbRating.getRating(),
                        etDate.getText().toString(),
                        etReview.getText() != null ? etReview.getText().toString() : "",
                        etNote.getText() != null ? etNote.getText().toString() : ""
                );
            }
            dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete();
            }
            dismiss();
        });
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    etDate.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }
}
