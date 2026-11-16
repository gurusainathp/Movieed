package com.gurusainathp.movieed;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.gurusainathp.movieed.ui.HomeFragment;
import com.gurusainathp.movieed.ui.PlaceholderFragment;
import com.gurusainathp.movieed.ui.diary.DiaryFragment;
import com.gurusainathp.movieed.ui.profile.ProfileFragment;
import com.gurusainathp.movieed.ui.search.SearchFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void showTab(int itemId) {
        Fragment fragment;
        String tag;
        if (itemId == R.id.nav_search) {
            tag = "search";
            fragment = new SearchFragment();
        } else if (itemId == R.id.nav_diary) {
            tag = "diary";
            fragment = new DiaryFragment();
        } else if (itemId == R.id.nav_profile) {
            tag = "profile";
            fragment = new ProfileFragment();
        } else {
            tag = "home";
            fragment = new HomeFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host, fragment, tag)
                .commit();
    }
}
