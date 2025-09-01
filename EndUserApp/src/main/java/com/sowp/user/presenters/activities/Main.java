package com.sowp.user.presenters.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.sowp.user.presenters.fragments.ProfileFragment;
//import com.sowp.user.presenters.fragments.AssessmentFragment;
import com.sowp.user.R;
import com.sowp.user.presenters.fragments.ChatFragment;
import com.sowp.user.presenters.fragments.CoursesFragment;
import com.sowp.user.presenters.fragments.HomeFragment;
import com.sowp.user.repositories.firebase.UserRepository;
import com.sowp.user.utils.UserAuthenticationUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class Main extends AppCompatActivity {

    public BottomNavigationView bottomNavigationView;
    Map<Integer, Fragment> fragmentMap;
    Fragment currentFragment;
    ImageView menuIcon;
    PopupMenu popupMenu;
    UserRepository userRepository;
    UserAuthenticationUtils userAuthenticationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize(savedInstanceState);
        if(!userAuthenticationUtils.hasPasswordProvider()) {
            showCustomPasswordDialog();
        }
        setUpClickListeners();
    }

    private void initialize(Bundle savedInstanceState) {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        menuIcon = findViewById(R.id.menu_icon);
        popupMenu = new PopupMenu(this, menuIcon);
        popupMenu.inflate(R.menu.header_menu);
        fragmentMap = new HashMap<>();
        userRepository = new UserRepository(this);
        userAuthenticationUtils = new UserAuthenticationUtils(this);
        // Initialize fragments
        fragmentMap.put(R.id.nav_home, new HomeFragment());
        fragmentMap.put(R.id.nav_courses, new CoursesFragment());
        fragmentMap.put(R.id.nav_chat, new ChatFragment());
        //fragmentMap.put(R.id.nav_quizzes, new AssessmentFragment());
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
    private void showCustomPasswordDialog() {
        // Inflate custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_password_input, null);

        TextInputEditText emailInput = dialogView.findViewById(R.id.email_input);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.password_input);

        // Pre-fill email if available
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            emailInput.setText(user.getEmail());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Link Account", null) // Set to null initially
                .create();

        // Override positive button to prevent auto-dismiss on validation failure
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                    // Show loading
                    positiveButton.setText("Linking...");
                    positiveButton.setEnabled(false);

                    userAuthenticationUtils.linkEmailPassword(email, password, new UserAuthenticationUtils.LinkingCallback() {
                        @Override
                        public void onSuccess() {
                            dialog.dismiss();
                            userRepository.updatePassword(password);
                        }

                        @Override
                        public void onFailure(Exception error) {
                            positiveButton.setText("Link Account");
                            positiveButton.setEnabled(true);
                            Toast.makeText(Main.this, "Error: " + error,
                                    Toast.LENGTH_LONG).show();
                        }
                    });

            });
        });

        dialog.show();
    }
}
