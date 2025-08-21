package com.android.nexcode.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.nexcode.presenters.fragments.ProfileFragment;
import com.android.nexcode.presenters.fragments.AssessmentFragment;
import com.android.nexcode.R;
import com.android.nexcode.presenters.fragments.ChatFragment;
import com.android.nexcode.presenters.fragments.CoursesFragment;
import com.android.nexcode.presenters.fragments.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class Main extends AppCompatActivity {

    public BottomNavigationView bottomNavigationView;
    Map<Integer, Fragment> fragmentMap;
    Fragment currentFragment;
    ImageView menuIcon;
    PopupMenu popupMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize(savedInstanceState);
        setUpClickListeners();
    }

    private void initialize(Bundle savedInstanceState) {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        menuIcon = findViewById(R.id.menu_icon);
        popupMenu = new PopupMenu(this, menuIcon);
        popupMenu.inflate(R.menu.header_menu);
        fragmentMap = new HashMap<>();
        // Initialize fragments
        fragmentMap.put(R.id.nav_home, new HomeFragment());
        fragmentMap.put(R.id.nav_courses, new CoursesFragment());
        fragmentMap.put(R.id.nav_chat, new ChatFragment());
        fragmentMap.put(R.id.nav_quizzes, new AssessmentFragment());
        fragmentMap.put(R.id.nav_profile, new ProfileFragment());
        // Load Home Fragment by default
        if (savedInstanceState == null) {
            currentFragment = fragmentMap.get(R.id.nav_home);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, currentFragment)
                    .commit();
        }

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

    private void setUpClickListeners() {
        menuIcon.setOnClickListener(v -> popupMenu.show());
        popupMenu.setOnMenuItemClickListener(item -> {
            if(item.getItemId() == R.id.about)
                startActivity(new Intent(this, AboutActivity.class));
            else if(item.getItemId()==R.id.settings)
                startActivity(new Intent(this, SettingsActivity.class));
            else if(item.getItemId()==R.id.chatbot)
                Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show();
            else if(item.getItemId()==R.id.logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, Authentication.class));
                finish();
            }
            return true;
        });
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switchFragment(fragmentMap.get(item.getItemId()));
            return true;
        });
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
