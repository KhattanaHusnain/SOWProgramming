package com.android.nexcode.presenters.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.models.Assignment;
import com.android.nexcode.presenters.activities.AssignmentActivity;
import com.android.nexcode.R;

import java.util.ArrayList;
import java.util.List;

public class AssignmentListFragment extends Fragment {

    private static final String ARG_ASSIGNMENT_LIST = "assignment_list";
    private List<Assignment> assignmentList = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView emptyView;

    public AssignmentListFragment() {
        // Required empty constructor
    }

    public static AssignmentListFragment newInstance(List<Assignment> assignmentList) {
        AssignmentListFragment fragment = new AssignmentListFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_ASSIGNMENT_LIST, new ArrayList<>(assignmentList));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            assignmentList = getArguments().getParcelableArrayList(ARG_ASSIGNMENT_LIST);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);

        setupRecyclerView();

        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        AssignmentAdapter adapter = new AssignmentAdapter(assignmentList);
        recyclerView.setAdapter(adapter);

        // Show empty view if list is empty
        if (assignmentList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("No assignments available");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    // RecyclerView adapter for Assignments
    private class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {

        private final List<Assignment> assignmentList;

        public AssignmentAdapter(List<Assignment> assignmentList) {
            this.assignmentList = assignmentList;
        }

        @NonNull
        @Override
        public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_assignment, parent, false);
            return new AssignmentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
            Assignment assignment = assignmentList.get(position);
            holder.titleTextView.setText(assignment.getTitle());
            holder.descriptionTextView.setText(assignment.getDescription());
            holder.dueDateTextView.setText("Due: " + assignment.getDueDate());
            holder.statusTextView.setText(assignment.getStatus());

            // Set status color based on assignment status
            setStatusColor(holder.statusTextView, assignment.getStatus());

            holder.itemView.setOnClickListener(v -> {
                // Navigate to AssignmentActivity
                Intent intent = AssignmentActivity.createIntent(getContext(), assignment);
                startActivity(intent);
            });
        }

        private void setStatusColor(TextView statusTextView, String status) {
            int colorResId;
            switch (status.toLowerCase()) {
                case "submitted":
                    colorResId = R.color.status_submitted;
                    break;
                default: // "not started"
                    colorResId = R.color.status_not_started;
                    break;
            }
            statusTextView.setTextColor(getResources().getColor(colorResId));
        }

        @Override
        public int getItemCount() {
            return assignmentList.size();
        }

        class AssignmentViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;
            TextView descriptionTextView;
            TextView dueDateTextView;
            TextView statusTextView;

            public AssignmentViewHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.titleTextView);
                descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
                dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
                statusTextView = itemView.findViewById(R.id.statusTextView);
            }
        }
    }
}