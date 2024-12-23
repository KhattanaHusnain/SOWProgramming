package com.android.nexcode;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class Home extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    Map<Integer, Fragment> fragmentMap = new HashMap<>();
    Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Initialize fragments
        fragmentMap.put(R.id.nav_home, new HomeFragment());
        fragmentMap.put(R.id.nav_courses, new CoursesFragment());
        fragmentMap.put(R.id.nav_chat, new ChatFragment());
        fragmentMap.put(R.id.nav_quizzes, new QuizzesFragment());
        fragmentMap.put(R.id.nav_profile, new ProfileFragment());

        // Load Home Fragment by default
        if (savedInstanceState == null) {
            currentFragment = fragmentMap.get(R.id.nav_home);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, currentFragment)
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            switchFragment(fragmentMap.get(item.getItemId()));
            return true;
        });
    }

    private void switchFragment(Fragment targetFragment) {
        if (currentFragment != targetFragment) {
            getSupportFragmentManager().beginTransaction()
                    .hide(currentFragment)
                    .show(targetFragment)
                    .commit();
            currentFragment = targetFragment;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        // Ensure that fragments are added and handled properly during configuration changes
        for (Map.Entry<Integer, Fragment> entry : fragmentMap.entrySet()) {
            Fragment fragment = entry.getValue();
            if (!fragment.isAdded()) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, fragment)
                        .hide(fragment)
                        .commit();
            }
        }

        // Show the current fragment
        if (currentFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .show(currentFragment)
                    .commit();
        }
    }
}
