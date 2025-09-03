package com.sowp.admin.usermanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.sowp.admin.R;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    Spinner spinnerVerification;
    Spinner spinnerSortBy;
    EditText et_search;
    TextView tv_user_count;
    TextView tv_page_info;
    ProgressBar progressBar;
    MaterialButton btn_Previous;
    MaterialButton btn_Next;
    FirebaseFirestore fb;
    UserAdapter userAdapter;
    List<User> userList;
    List<User> filterList;
    int CurrentPage = 1;
    int Total_Page=1;
    int TotalUser =0;
    final int Page_Per_User= 10;
    String currentVerificationFilter= "All";
    String currentSortBy ="Name";
    String currentSearch ="";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);
        recyclerView = findViewById(R.id.rv_users);
        spinnerVerification = findViewById(R.id.spinner_verification);
        spinnerSortBy = findViewById(R.id.spinner_sort_by);
        et_search = findViewById(R.id.et_search);
        tv_user_count = findViewById(R.id.tv_user_count);
        tv_page_info = findViewById(R.id.tv_page_info);
        progressBar = findViewById(R.id.progress_loading);
        btn_Previous = findViewById(R.id.btn_previous);
        btn_Next = findViewById(R.id.btn_next);
        fb = FirebaseFirestore.getInstance();
        userList = new ArrayList<>();
        filterList = new ArrayList<>();

        String[] verification = {"All", "Verified", "UnVerified"};
        ArrayAdapter<String> verificationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, verification);
        spinnerVerification.setAdapter(verificationAdapter);

        String[] Sort = {"Name", "email", "semester", "Gender", "Degree", "Date Created"};
        ArrayAdapter<String> SortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Sort);
        spinnerSortBy.setAdapter(SortAdapter);

        userAdapter = new UserAdapter(filterList,this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(userAdapter);

        userAdapter.setOnItemClickListener(v->{
            Intent intent = new Intent(UserManagementActivity.this,UserProfileActivity.class);
            intent.putExtra("USER_EMAIL",v.getEmail());
            startActivity(intent);
        });
        btn_Previous.setOnClickListener( v -> {
            if(CurrentPage > 1){
                CurrentPage--;
                updatePaginated();
            }

        });
        btn_Next.setOnClickListener(v -> {
            if(CurrentPage < Total_Page)
                CurrentPage++;
            updatePaginated();
        });





        LoadUser();

    }

    public void LoadUser(){
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        fb.collection("User")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->{
                    userList.clear();
                    for(QueryDocumentSnapshot document :queryDocumentSnapshots){
                        User user = User.fromDocument(document);
                        if(user !=null){
                            userList.add(user);
                        }
                    }
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    applyFilterAndSort();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "data loading fail"+e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }

    public void applyFilterAndSort(){
        filterList.clear();
        for(User user: userList){
            boolean matchSearch = currentSearch.isEmpty() ||
                    user.getFullName().toLowerCase().contains(currentSearch) ||
                    user.getEmail().toLowerCase().contains(currentSearch) ||
                    user.getDegree().toLowerCase().contains(currentSearch) ||
                    user.getDegree().toLowerCase().contains(currentSearch);
            boolean matchVerification = currentVerificationFilter.equals("All") ||
                    (currentVerificationFilter.equals("Verified") && user.getIsVerified()) ||
                    (currentVerificationFilter.equals("UnVerified") && !user.getIsVerified());

            if( matchSearch && matchVerification){
                filterList.add(user);
            }

        }
        switch (currentSortBy){
            case "Name":
                filterList.sort((user1,user2)->user1.getFullName().compareToIgnoreCase(user2.getFullName()));
                break;
            case "Email":
                filterList.sort((user1,user2)->user1.getEmail().compareToIgnoreCase(user2.getEmail()));
                break;
            case "Semester":
                filterList.sort((user1,user2)->user1.getDisplaySemester().compareToIgnoreCase(user2.getDisplaySemester()));
                break;
            case "Gender":
                filterList.sort((user1,user2)->user1.getGender().compareToIgnoreCase(user2.getGender()));
                break;
            case "Degree":
                filterList.sort((user1,user2)->user1.getDegree().compareToIgnoreCase(user2.getDegree()));
                break;
            case "Date Created":
                filterList.sort((user1,user2)->Long.compare(user2.getCreatedAt(), user1.getCreatedAt()));
                break;


        }
        TotalUser = filterList.size();
        Total_Page =(int) Math.ceil((double) TotalUser /Page_Per_User);
        if(TotalUser == 0) Total_Page = 1;

        updatePaginated();
        updateUI();

    }
    private void updatePaginated(){
        List<User> PaginatedList = new ArrayList<>();
        int StartIndex = (CurrentPage -1) * Page_Per_User;
        int EndIndex = Math.min(StartIndex + Page_Per_User,filterList.size());
         if(StartIndex < filterList.size()){
             PaginatedList.addAll(filterList.subList(StartIndex,EndIndex));

         }

         userAdapter.updateUsers(PaginatedList);
         updatePaginatedButtons();


    }
    private void updateUI(){
        tv_user_count.setText("Total Users: " + TotalUser);
        tv_page_info.setText("Page "+CurrentPage +" of " + Total_Page);
        updatePaginatedButtons();
    }
    private void updatePaginatedButtons(){
        btn_Previous.setEnabled(CurrentPage > 1);
        btn_Next.setEnabled(CurrentPage < Total_Page);

        btn_Previous.setAlpha(CurrentPage > 1 ? 1.0f : 0.5f);
        btn_Next.setAlpha(CurrentPage < Total_Page ? 1.0f : 0.5f);

    }
    public void onResume(){
        super.onResume();
        LoadUser();
    }





}