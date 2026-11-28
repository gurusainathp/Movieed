package com.gurusainathp.movieed;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.gurusainathp.movieed.domain.AppPreloader;
import com.gurusainathp.movieed.ui.HomeFragment;
import com.gurusainathp.movieed.ui.HomeViewModel;
import com.gurusainathp.movieed.ui.diary.DiaryFragment;
import com.gurusainathp.movieed.ui.explore.ExploreFragment;
import com.gurusainathp.movieed.ui.explore.ExploreViewModel;
import com.gurusainathp.movieed.ui.profile.ProfileFragment;
import com.gurusainathp.movieed.ui.profile.ProfileViewModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Keep splash screen visible until preloading finishes
        splashScreen.setKeepOnScreenCondition(() -> !Boolean.TRUE.equals(AppPreloader.getIsPreloadReady().getValue()));

        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        ExploreViewModel exploreViewModel = new ViewModelProvider(this).get(ExploreViewModel.class);
        ProfileViewModel profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        AppPreloader.startPreload(getApplication(), homeViewModel, exploreViewModel, profileViewModel);

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
        if (itemId == R.id.nav_explore) {
            tag = "explore";
            fragment = new ExploreFragment();
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
