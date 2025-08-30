package com.sowp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {

    // UI Components
    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private ProgressBar progressLoading;
    private TextView tvUserCount, tvPageInfo;
    private MaterialButton btnPrevious, btnNext;
    private Spinner spinnerVerification, spinnerSortBy;
    private EditText etSearch;
    private LinearLayout mainLayout;

    // Firebase
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    // Data
    private List<User> userList;
    private List<User> filteredUserList;
    private int currentPage = 1;
    private static final int USERS_PER_PAGE = 10;
    private int totalUsers = 0;
    private int totalPages = 1;

    // Filters
    private String currentVerificationFilter = "All";
    private String currentSortBy = "Name";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_management);

        initializeViews();
        setupWindowInsets();
        setupFirebase();
        setupSpinners();
        setupRecyclerView();
        setupListeners();
        loadUsers();
    }

    private void initializeViews() {
        recyclerViewUsers = findViewById(R.id.rv_users);
        progressLoading = findViewById(R.id.progress_loading);
        tvUserCount = findViewById(R.id.tv_user_count);
        tvPageInfo = findViewById(R.id.tv_page_info);
        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);
        spinnerVerification = findViewById(R.id.spinner_verification);
        spinnerSortBy = findViewById(R.id.spinner_sort_by);
        etSearch = findViewById(R.id.et_search);
        mainLayout = findViewById(R.id.main_layout);

        userList = new ArrayList<>();
        filteredUserList = new ArrayList<>();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            // Apply padding to the main layout
            v.setPadding(
                    v.getPaddingLeft(),
                    systemBars.top, // Top padding for status bar
                    v.getPaddingRight(),
                    navigationBars.bottom // Bottom padding for navigation bar
            );

            return insets;
        });
    }

    private void setupFirebase() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private void setupSpinners() {
        // Verification Status Spinner
        String[] verificationOptions = {"All", "Verified", "Unverified"};
        ArrayAdapter<String> verificationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, verificationOptions);
        verificationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVerification.setAdapter(verificationAdapter);

        // Sort By Spinner
        String[] sortOptions = {"Name", "Email", "Semester", "Gender", "Degree", "Date Created"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSortBy.setAdapter(sortAdapter);
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(filteredUserList, this);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(userAdapter);

        userAdapter.setOnItemClickListener(user -> {
            Intent intent = new Intent(UserManagementActivity.this, UserProfileActivity.class);
            intent.putExtra("USER_EMAIL", user.getEmail());
            startActivity(intent);
        });
    }

    private void setupListeners() {
        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                currentPage = 1;
                applyFiltersAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Verification filter
        spinnerVerification.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentVerificationFilter = parent.getItemAtPosition(position).toString();
                currentPage = 1;
                applyFiltersAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Sort by filter
        spinnerSortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSortBy = parent.getItemAtPosition(position).toString();
                currentPage = 1;
                applyFiltersAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Pagination buttons
        btnPrevious.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updatePaginatedList();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                updatePaginatedList();
            }
        });
    }

    private void loadUsers() {
        progressLoading.setVisibility(View.VISIBLE);
        recyclerViewUsers.setVisibility(View.GONE);

        firestore.collection("User")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = User.fromDocument(document);
                        if (user != null) {
                            userList.add(user);
                        }
                    }

                    progressLoading.setVisibility(View.GONE);
                    recyclerViewUsers.setVisibility(View.VISIBLE);

                    applyFiltersAndSort();
                })
                .addOnFailureListener(e -> {
                    progressLoading.setVisibility(View.GONE);
                    recyclerViewUsers.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Failed to load users: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFiltersAndSort() {
        filteredUserList.clear();

        for (User user : userList) {
            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    user.getFullName().toLowerCase().contains(currentSearchQuery) ||
                    user.getEmail().toLowerCase().contains(currentSearchQuery);

            boolean matchesVerification = currentVerificationFilter.equals("All") ||
                    (currentVerificationFilter.equals("Verified") && !user.isEmailVerified()) ||
                    (currentVerificationFilter.equals("Unverified") && user.isEmailVerified());

            if (matchesSearch && matchesVerification) {
                filteredUserList.add(user);
            }
        }

        // Sort the filtered list
        switch (currentSortBy) {
            case "Name":
                filteredUserList.sort((u1, u2) -> u1.getFullName().compareToIgnoreCase(u2.getFullName()));
                break;
            case "Email":
                filteredUserList.sort((u1, u2) -> u1.getEmail().compareToIgnoreCase(u2.getEmail()));
                break;
            case "Semester":
                filteredUserList.sort((u1, u2) -> u1.getDisplaySemester().compareToIgnoreCase(u2.getDisplaySemester()));
                break;
            case "Gender":
                filteredUserList.sort((u1, u2) -> u1.getGender().compareToIgnoreCase(u2.getGender()));
                break;
            case "Degree":
                filteredUserList.sort((u1, u2) -> u1.getDegree().compareToIgnoreCase(u2.getDegree()));
                break;
            case "Date Created":
                filteredUserList.sort((u1, u2) -> Long.compare(u2.getCreatedAt(), u1.getCreatedAt()));
                break;
        }

        totalUsers = filteredUserList.size();
        totalPages = (int) Math.ceil((double) totalUsers / USERS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        updatePaginatedList();
        updateUI();
    }

    private void updatePaginatedList() {
        List<User> paginatedList = new ArrayList<>();
        int startIndex = (currentPage - 1) * USERS_PER_PAGE;
        int endIndex = Math.min(startIndex + USERS_PER_PAGE, filteredUserList.size());

        if (startIndex < filteredUserList.size()) {
            paginatedList.addAll(filteredUserList.subList(startIndex, endIndex));
        }

        userAdapter.updateUsers(paginatedList);
        updatePaginationButtons();
    }

    private void updateUI() {
        tvUserCount.setText("Total Users: " + totalUsers);
        tvPageInfo.setText("Page " + currentPage + " of " + totalPages);
        updatePaginationButtons();
    }

    private void updatePaginationButtons() {
        btnPrevious.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);

        btnPrevious.setAlpha(currentPage > 1 ? 1.0f : 0.5f);
        btnNext.setAlpha(currentPage < totalPages ? 1.0f : 0.5f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning from user profile
        loadUsers();
    }
}