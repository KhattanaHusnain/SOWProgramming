package com.sowp.user.presenters.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.sowp.user.presenters.fragments.AssessmentFragment;
import com.sowp.user.presenters.fragments.ProfileFragment;
import com.sowp.user.R;
import com.sowp.user.presenters.fragments.ChatFragment;
import com.sowp.user.presenters.fragments.CoursesFragment;
import com.sowp.user.presenters.fragments.HomeFragment;
import com.sowp.user.repositories.UserRepository;
import com.sowp.user.services.UserAuthenticationUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

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

        fragmentMap.put(R.id.nav_home, new HomeFragment());
        fragmentMap.put(R.id.nav_courses, new CoursesFragment());
        fragmentMap.put(R.id.nav_chat, new ChatFragment());
        fragmentMap.put(R.id.nav_quizzes, new AssessmentFragment());
        fragmentMap.put(R.id.nav_profile, new ProfileFragment());

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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomNavigationView != null) {
            outState.putInt("selected_nav_item", bottomNavigationView.getSelectedItemId());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int selectedItemId = savedInstanceState.getInt("selected_nav_item", R.id.nav_home);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(selectedItemId);
        }
        Fragment targetFragment = fragmentMap.get(selectedItemId);
        if (targetFragment != null) {
            currentFragment = targetFragment;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (fragmentMap != null && currentFragment == null) {
            currentFragment = fragmentMap.get(R.id.nav_home);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentFragment != null && fragmentMap != null) {
            for (Fragment fragment : fragmentMap.values()) {
                if (fragment != null && fragment.isAdded() && fragment != currentFragment) {
                    getSupportFragmentManager().beginTransaction()
                            .hide(fragment)
                            .commit();
                }
            }
            if (currentFragment.isAdded()) {
                getSupportFragmentManager().beginTransaction()
                        .show(currentFragment)
                        .commit();
            }
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        for (Map.Entry<Integer, Fragment> entry : fragmentMap.entrySet()) {
            Fragment fragment = entry.getValue();
            if (!fragment.isAdded()) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, fragment)
                        .hide(fragment)
                        .commit();
            }
        }

        if (currentFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .show(currentFragment)
                    .commit();
        }
    }

    private void showCustomPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_password_input, null);

        TextInputEditText emailInput = dialogView.findViewById(R.id.email_input);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.password_input);

        emailInput.setText(userAuthenticationUtils.getCurrentUserEmail());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Link Account", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

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
                        Toast.makeText(Main.this, "The password must be atleast 8 characters long, with one capital, one small, one numeric and one special character.", Toast.LENGTH_LONG).show();
                        positiveButton.setText("Link Account");
                        positiveButton.setEnabled(true);
                    }
                });
            });
        });

        dialog.show();
    }

    @SuppressLint({"GestureBackNavigation", "MissingSuperCall"})
    @Override
    public void onBackPressed() {
        if(currentFragment instanceof HomeFragment) {
            new AlertDialog.Builder(this).setTitle("Exit App").setMessage("Do you really want to exit the app?")
                    .setPositiveButton("Exit",(dialog, which)->finishAffinity()).setNegativeButton("Cancel",(dialog,which)->dialog.dismiss()).show();
        } else {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }
}