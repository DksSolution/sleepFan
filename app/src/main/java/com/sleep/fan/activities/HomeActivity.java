package com.sleep.fan.activities;

import static android.os.Build.VERSION.SDK_INT;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.multidex.BuildConfig;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.sleep.fan.MainActivity;
import com.sleep.fan.MainActivityKt;
import com.sleep.fan.R;
import com.sleep.fan.databinding.ActivityHomeBinding;
import com.sleep.fan.db.SleepDataDao;
import com.sleep.fan.fragments.FansFragment;
import com.sleep.fan.fragments.FrgTracking;
import com.sleep.fan.fragments.SoundsFragment;
import com.sleep.fan.newdb.NewSleepDatabase;
import com.sleep.fan.newdb.SleepDao;
import com.sleep.fan.signin.SocialData;
import com.sleep.fan.utility.GoogleLoginUtil;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private List<TextView> drawerTextViews;
    private Fragment fansFragment;
    private Fragment trackingFragment;
    private Fragment soundsFragment;
    private SleepDataDao sleepDataDao;
    SleepDao sleepDao;
    private boolean isForegroundServiceStarted = false;

    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);// set status text dark
        if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));// set status background white
        }
        binding = DataBindingUtil.setContentView(HomeActivity.this, R.layout.activity_home);
        binding.bnView.setOnNavigationItemSelectedListener(navListener);

        setDrawer();

        NewSleepDatabase newSleepDatabase = NewSleepDatabase.getInstance(this);
        sleepDao = newSleepDatabase.sleepDao();

        // Initialize the fragments
        fansFragment = new FansFragment();
        trackingFragment = new FrgTracking();
        soundsFragment = new SoundsFragment();

        // Add the fragments to the FragmentManager
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fansFragment, "FANS_FRAGMENT").hide(fansFragment)
                .add(R.id.fragment_container, trackingFragment, "TRACKING_FRAGMENT").hide(trackingFragment)
                .add(R.id.fragment_container, soundsFragment, "SOUNDS_FRAGMENT").hide(soundsFragment)
                .commit();

        // Show the default fragment
        showFragment(fansFragment);

    }

    private void setDrawer() {
        drawerTextViews = new ArrayList<>();
        drawerTextViews.add(binding.drawerTerms);
        drawerTextViews.add(binding.drawerPrivacy);
        drawerTextViews.add(binding.drawerWhatPremium);
        drawerTextViews.add(binding.drawerSleepTrack);
        drawerTextViews.add(binding.drawerHowCancel);
        drawerTextViews.add(binding.drawerContact);

        for (TextView textView : drawerTextViews) {
            textView.setOnClickListener(view -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        binding.btnMenu.setOnClickListener(view -> binding.drawerLayout.openDrawer(GravityCompat.START));

        binding.tvVersion.setText("v" + BuildConfig.VERSION_NAME);
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Hide all fragments
        transaction.hide(fansFragment);
        transaction.hide(trackingFragment);
        transaction.hide(soundsFragment);

        // Show the selected fragment
        transaction.show(fragment);

        transaction.commit();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_fans:
                    binding.appBar.setVisibility(View.VISIBLE);
                    showFragment(fansFragment);
                    break;
                case R.id.menu_tracking:
                    binding.appBar.setVisibility(View.GONE);
                    showFragment(trackingFragment);
                    break;
                case R.id.menu_sounds:
                    binding.appBar.setVisibility(View.VISIBLE);
                    showFragment(soundsFragment);
                    break;
            }
            return true;
        }
    };
}
