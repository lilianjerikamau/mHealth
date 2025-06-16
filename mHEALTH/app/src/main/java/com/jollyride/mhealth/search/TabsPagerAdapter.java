package com.jollyride.mhealth.search;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TabsPagerAdapter extends FragmentStateAdapter {
    public TabsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new RecentFragment();
            case 1: return new HospitalsFragment();
            case 2: return new ClinicsFragment();
            default: return new RecentFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Number of tabs
    }
}

