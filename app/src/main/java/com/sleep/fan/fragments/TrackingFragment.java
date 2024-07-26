package com.sleep.fan.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;
import com.sleep.fan.R;

import com.sleep.fan.databinding.FragmentTrackingBinding;
import com.sleep.fan.fragments.trackFragments.DayFragment;
import com.sleep.fan.fragments.trackFragments.MonthFragment;
import com.sleep.fan.fragments.trackFragments.WeekFragment;

public class TrackingFragment extends Fragment {
    private FragmentTrackingBinding binding;
    private static final String TAG = "MainActivity";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tracking, container, false);
        View view = binding.getRoot();

        // Setup TabLayout with listeners
        setupTabLayout();

        // Initially display the first fragment
        replaceFragment(new DayFragment());

        return view;
    }

    private void setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("D")); // Day
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("W")); // Week
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("M")); // Month

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        replaceFragment(new DayFragment());
                        break;
                    case 1:
                        replaceFragment(new WeekFragment());
                        break;
                    case 2:
                        replaceFragment(new MonthFragment());
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Do nothing
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Do nothing
            }
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

}
