package com.gurusainathp.movieed.ui.diary;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class DiaryPagerAdapter extends FragmentStateAdapter {

    public DiaryPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return DiaryTabFragment.newInstance(DiaryTabFragment.TAB_WATCHLIST);
        } else {
            return DiaryTabFragment.newInstance(DiaryTabFragment.TAB_WATCHED);
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
