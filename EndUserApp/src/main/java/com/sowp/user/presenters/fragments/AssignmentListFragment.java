//package com.sowp.user.presenters.fragments;
//
//import android.content.Intent;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Parcelable;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
//
//import com.sowp.user.models.Assignment;
//import com.sowp.user.presenters.activities.AssignmentActivity;
//import com.sowp.user.repositories.firebase.AssignmentRepository;
//import com.sowp.user.R;
//import com.google.firebase.firestore.DocumentSnapshot;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//public class AssignmentListFragment extends Fragment {
//
//    private static final String ARG_ASSIGNMENT_LIST = "assignment_list";
//    private List<Parcelable> assignmentList = new ArrayList<>();
//    private RecyclerView recyclerView;
//    private TextView emptyView;
//    private SwipeRefreshLayout swipeRefreshLayout;
//    private ProgressBar loadMoreProgressBar;
//
//    private AssignmentAdapter adapter;
//    private AssessmentFragment parentFragment;
//    private boolean isLoading = false;
//    private boolean hasMoreData = false;
//
//    public AssignmentListFragment() {
//        // Required empty constructor
//    }
//
//    public static AssignmentListFragment newInstance(List<Assignment> assignmentList, AssessmentFragment parentFragment) {
//        AssignmentListFragment fragment = new AssignmentListFragment();
//        Bundle args = new Bundle();
//        //args.putParcelableArrayList(ARG_ASSIGNMENT_LIST, new ArrayList<android.os.Parcelable>((Collection<? extends Parcelable>) assignmentList));
//        //fragment.setArguments(args);
//        fragment.parentFragment = parentFragment;
//        return fragment;
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
//                assignmentList = getArguments().getParcelableArrayList(ARG_ASSIGNMENT_LIST).reversed();
//            }
//        }
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_assignment_list, container, false);
//
//        recyclerView = view.findViewById(R.id.recyclerView);
//        emptyView = view.findViewById(R.id.emptyView);
//        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
//        loadMoreProgressBar = view.findViewById(R.id.loadMoreProgressBar);
//
//        setupSwipeRefresh();
//        setupRecyclerView();
//
//        return view;
//    }
//
//    private void setupSwipeRefresh() {
//        swipeRefreshLayout.setOnRefreshListener(() -> {
//            if (parentFragment != null) {
//                parentFragment.refreshAssignments(new AssignmentRepository.PaginatedCallback() {
//                    @Override
//                    public void onSuccess(List<Assignment> assignments, DocumentSnapshot lastDocument, boolean hasMore) {
//                        assignmentList.clear();
//                        //assignmentList.addAll((Collection<? extends Parcelable>) assignments);
//                        hasMoreData = hasMore;
//                        adapter.notifyDataSetChanged();
//                        updateEmptyView();
//                        swipeRefreshLayout.setRefreshing(false);
//                        Toast.makeText(getContext(), "Assignments refreshed", Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onFailure(String message) {
//                        swipeRefreshLayout.setRefreshing(false);
//                        Toast.makeText(getContext(), "Failed to refresh: " + message, Toast.LENGTH_SHORT).show();
//                    }
//                });
//            } else {
//                swipeRefreshLayout.setRefreshing(false);
//            }
//        });
//    }
//
//    private void setupRecyclerView() {
//        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
//        recyclerView.setLayoutManager(layoutManager);
//
//        adapter = new AssignmentAdapter(new ArrayList<>());
//        recyclerView.setAdapter(adapter);
//
//        // Add scroll listener for pagination
//        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//
//                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
//                if (layoutManager != null && !isLoading && hasMoreData) {
//                    int visibleItemCount = layoutManager.getChildCount();
//                    int totalItemCount = layoutManager.getItemCount();
//                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
//
//                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
//                            && firstVisibleItemPosition >= 0) {
//                        loadMoreAssignments();
//                    }
//                }
//            }
//        });
//
//        updateEmptyView();
//    }
//
//    private void loadMoreAssignments() {
//        if (parentFragment == null || isLoading) return;
//
//        isLoading = true;
//        loadMoreProgressBar.setVisibility(View.VISIBLE);
//
//        parentFragment.loadMoreAssignments(new AssignmentRepository.PaginatedCallback() {
//            @Override
//            public void onSuccess(List<Assignment> assignments, DocumentSnapshot lastDocument, boolean hasMore) {
//                isLoading = false;
//                loadMoreProgressBar.setVisibility(View.GONE);
//                hasMoreData = hasMore;
//
//                if (!assignments.isEmpty()) {
//                    int startPosition = assignmentList.size();
//                   // assignmentList.addAll((Collection<? extends Parcelable>) assignments);
//                    adapter.notifyItemRangeInserted(startPosition, assignments.size());
//                }
//
//                updateEmptyView();
//            }
//
//            @Override
//            public void onFailure(String message) {
//                isLoading = false;
//                loadMoreProgressBar.setVisibility(View.GONE);
//                Toast.makeText(getContext(), "Failed to load more: " + message, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void updateEmptyView() {
//        if (assignmentList.isEmpty()) {
//            recyclerView.setVisibility(View.GONE);
//            emptyView.setVisibility(View.VISIBLE);
//            emptyView.setText("No assignments available");
//        } else {
//            recyclerView.setVisibility(View.VISIBLE);
//            emptyView.setVisibility(View.GONE);
//        }
//    }
//
//    // RecyclerView adapter for Assignments
//    private class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {
//
//        private final List<Assignment> assignmentList;
//
//        public AssignmentAdapter(List<Assignment> assignmentList) {
//            this.assignmentList = assignmentList;
//        }
//
//        @NonNull
//        @Override
//        public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            View view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.item_assignment, parent, false);
//            return new AssignmentViewHolder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
//            Assignment assignment = assignmentList.get(position);
//            holder.titleTextView.setText(assignment.getTitle());
//            holder.descriptionTextView.setText(assignment.getDescription());
//
//
//            holder.itemView.setOnClickListener(v -> {
//                // Navigate to AssignmentActivity
//                Intent intent = AssignmentActivity.createIntent(getContext(), assignment);
//                startActivity(intent);
//            });
//        }
//
//        private void setStatusColor(TextView statusTextView, String status) {
//            int colorResId;
//            switch (status.toLowerCase()) {
//                case "submitted":
//                    colorResId = R.color.status_submitted;
//                    break;
//                default: // "not started"
//                    colorResId = R.color.status_not_started;
//                    break;
//            }
//            statusTextView.setTextColor(getResources().getColor(colorResId));
//        }
//
//        @Override
//        public int getItemCount() {
//            return assignmentList.size();
//        }
//
//        class AssignmentViewHolder extends RecyclerView.ViewHolder {
//            TextView titleTextView;
//            TextView descriptionTextView;
//            TextView dueDateTextView;
//            TextView statusTextView;
//
//            public AssignmentViewHolder(@NonNull View itemView) {
//                super(itemView);
//                titleTextView = itemView.findViewById(R.id.titleTextView);
//                descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
//                dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
//                statusTextView = itemView.findViewById(R.id.statusTextView);
//            }
//        }
//    }
//}