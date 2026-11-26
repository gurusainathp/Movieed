package com.gurusainathp.movieed.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.ui.movie.MovieDetailsActivity;

public class SearchFragment extends Fragment {

    private SearchViewModel viewModel;
    private SearchMovieAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView tvErrorState;
    private RecyclerView rvResults;
    private EditText etQuery;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        etQuery = view.findViewById(R.id.et_search_query);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvErrorState = view.findViewById(R.id.tv_error_state);
        rvResults = view.findViewById(R.id.rv_search_results);

        adapter = new SearchMovieAdapter(movie -> {
            if (movie != null) {
                if ("person".equalsIgnoreCase(movie.getMediaType())) {
                    com.gurusainathp.movieed.ui.person.PersonDetailsActivity.launch(requireContext(), movie.getId(), movie.getTitle());
                } else {
                    boolean isTv = "tv".equals(movie.getMediaType());
                    MovieDetailsActivity.launch(requireContext(), movie.getId(), movie.getTitle(), movie.getPosterPath(), isTv);
                }
            }
        });
        rvResults.setAdapter(adapter);

        etQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        etQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    performSearch();
                } else if (s.length() == 0) {
                    viewModel.searchMovies("", BuildConfig.TMDB_API_KEY);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        observeViewModel();
    }

    private void performSearch() {
        String query = etQuery.getText() != null ? etQuery.getText().toString() : "";
        viewModel.searchMovies(query, BuildConfig.TMDB_API_KEY);
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), movies -> {
            adapter.setMovies(movies);
            if (movies != null && !movies.isEmpty()) {
                tvEmptyState.setVisibility(View.GONE);
                rvResults.setVisibility(View.VISIBLE);
            } else {
                rvResults.setVisibility(View.GONE);
                if (Boolean.FALSE.equals(viewModel.getIsLoading().getValue())) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                tvEmptyState.setVisibility(View.GONE);
                tvErrorState.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                tvErrorState.setText(error);
                tvErrorState.setVisibility(View.VISIBLE);
                tvEmptyState.setVisibility(View.GONE);
                rvResults.setVisibility(View.GONE);
            } else {
                tvErrorState.setVisibility(View.GONE);
            }
        });
    }
}
