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
            return DiaryTabFragment.newInstance(DiaryTabFragment.TAB_WATCHED);
        } else {
            return DiaryTabFragment.newInstance(DiaryTabFragment.TAB_WATCHLIST);
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
