package com.jollyride.mhealth;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.jollyride.mhealth.search.TabsPagerAdapter;

public class AddressSearchActivity extends BaseActivity {

    TabLayout tabLayout;
    ViewPager2 viewPager;
    TabsPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_search);

        ImageView backButton = findViewById(R.id.backButton);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        adapter = new TabsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        String[] tabTitles = new String[]{"RECENT", "HOSPITALS", "CLINICS"};

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            View customView = LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
            TextView tabText = customView.findViewById(R.id.tabText);
            tabText.setText(tabTitles[position]);
            tabText.setTextSize(14);

            // First tab selected by default
            if (position == 0) {
                tabText.setTypeface(null, Typeface.BOLD);
                customView.setSelected(true);
            }

            tab.setCustomView(customView);
        }).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                TextView textView = tab.getCustomView().findViewById(R.id.tabText);
                textView.setTypeface(null, Typeface.BOLD);
                textView.setSelected(true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                TextView textView = tab.getCustomView().findViewById(R.id.tabText);
                textView.setTypeface(null, Typeface.NORMAL);
                textView.setSelected(false);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Optional: add behavior
            }
        });




        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(AddressSearchActivity.this, RiderHomeActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
    }
}