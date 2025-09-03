package com.sowp.admin.assignmentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sowp.admin.R;

import java.util.ArrayList;
import java.util.List;

public class ViewUncheckedAssignmentsActivity extends AppCompatActivity implements UncheckedAssignmentAdapter.OnAssignmentClickListener {

    private static final String TAG = "ViewUncheckedAssignments";
    private static final int PAGE_SIZE = 10;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private UncheckedAssignmentAdapter adapter;
    private FirebaseFirestore db;

    private List<UncheckedAssignment> assignmentsList;
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_unchecked_assignments);

        initViews();
        setupRecyclerView();
        loadFirstPage();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewAssignments);
        progressBar = findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        assignmentsList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new UncheckedAssignmentAdapter(assignmentsList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Add pagination scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadNextPage();
                    }
                }
            }
        });
    }

    private void loadFirstPage() {
        showLoading(true);
        isLoading = true;

        db.collection("uncheckedAssignments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    assignmentsList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        UncheckedAssignment assignment = createAssignmentFromDocument(document);
                        assignmentsList.add(assignment);
                    }

                    if (queryDocumentSnapshots.size() > 0) {
                        lastVisible = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                    }

                    if (queryDocumentSnapshots.size() < PAGE_SIZE) {
                        isLastPage = true;
                    }

                    adapter.notifyDataSetChanged();
                    showLoading(false);
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading assignments: ", e);
                    Toast.makeText(this, "Failed to load assignments", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    isLoading = false;
                });
    }

    private void loadNextPage() {
        if (lastVisible == null) return;

        isLoading = true;
        adapter.showLoadingFooter(true);

        db.collection("uncheckedAssignments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UncheckedAssignment> newAssignments = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        UncheckedAssignment assignment = createAssignmentFromDocument(document);
                        newAssignments.add(assignment);
                    }

                    if (queryDocumentSnapshots.size() > 0) {
                        lastVisible = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);

                        int startPosition = assignmentsList.size();
                        assignmentsList.addAll(newAssignments);
                        adapter.notifyItemRangeInserted(startPosition, newAssignments.size());
                    }

                    if (queryDocumentSnapshots.size() < PAGE_SIZE) {
                        isLastPage = true;
                    }

                    adapter.showLoadingFooter(false);
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading next page: ", e);
                    Toast.makeText(this, "Failed to load more assignments", Toast.LENGTH_SHORT).show();
                    adapter.showLoadingFooter(false);
                    isLoading = false;
                });
    }

    private UncheckedAssignment createAssignmentFromDocument(QueryDocumentSnapshot document) {
        UncheckedAssignment assignment = new UncheckedAssignment();
        assignment.setId(document.getId());
        assignment.setAssignmentTitle(document.getString("assignmentTitle"));
        assignment.setUserEmail(document.getString("userEmail"));

        // Handle DocumentReference properly
        DocumentReference attemptRef = document.getDocumentReference("assignmentAttemptRef");
        assignment.setAssignmentAttemptRef(attemptRef);

        assignment.setCreatedAt(document.getLong("createdAt"));

        return assignment;
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onAssignmentClick(UncheckedAssignment assignment) {
        Intent intent = new Intent(this, AssignmentDetailsActivity.class);
        intent.putExtra("uncheckedAssignmentId", assignment.getId());

        // Pass the DocumentReference path as string
            intent.putExtra("assignmentAttemptRef", assignment.getAssignmentAttemptRef().getPath());

        startActivity(intent);
    }
}