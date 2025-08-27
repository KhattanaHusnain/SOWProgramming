package com.android.SOWProgramming.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.android.SOWProgramming.R;
import com.android.SOWProgramming.models.Assignment;

import java.util.List;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {

    private Context context;
    private List<Assignment> assignmentList;
    private OnAssignmentItemClickListener onAssignmentItemClickListener;

    public interface OnAssignmentItemClickListener {
        void onAssignmentItemClick(Assignment assignment);
    }

    public AssignmentAdapter(Context context, List<Assignment> assignmentList) {
        this.context = context;
        this.assignmentList = assignmentList;
    }

    public void setOnAssignmentItemClickListener(OnAssignmentItemClickListener listener) {
        this.onAssignmentItemClickListener = listener;
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_assignment, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        Assignment assignment = assignmentList.get(position);
        holder.bind(assignment);
    }

    @Override
    public int getItemCount() {
        return assignmentList != null ? assignmentList.size() : 0;
    }

    public void updateAssignments(List<Assignment> newAssignmentList) {
        this.assignmentList = newAssignmentList;
        notifyDataSetChanged();
    }

    public class AssignmentViewHolder extends RecyclerView.ViewHolder {

        private TextView titleTextView;
        private TextView descriptionTextView;
        private TextView dueDateTextView;
        private TextView statusTextView;
        private MaterialButton viewAssignmentButton;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);

            titleTextView = itemView.findViewById(R.id.titleTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            viewAssignmentButton = itemView.findViewById(R.id.viewAssignmentButton);
        }

        public void bind(Assignment assignment) {
            // Set title
            titleTextView.setText(assignment.getTitle() != null ? assignment.getTitle() : "Untitled Assignment");

            // Set description
            descriptionTextView.setText(assignment.getDescription() != null ? assignment.getDescription() : "No description available");

            // Set due date
            if (assignment.getDueDate() != null) {
                dueDateTextView.setText(assignment.getDueDate());
            } else {
                dueDateTextView.setText("No due date");
            }

            // Set status and update UI accordingly
            updateStatusAndButton(assignment);

            // Set click listeners
            viewAssignmentButton.setOnClickListener(v -> {
                if (onAssignmentItemClickListener != null) {
                    onAssignmentItemClickListener.onAssignmentItemClick(assignment);
                }
            });

            // Set item click listener for the entire card
            itemView.setOnClickListener(v -> {
                if (onAssignmentItemClickListener != null) {
                    onAssignmentItemClickListener.onAssignmentItemClick(assignment);
                }
            });
        }

        private void updateStatusAndButton(Assignment assignment) {
            String status = getAssignmentStatus(assignment);
            statusTextView.setText(status);

            // Update status text color based on status
            switch (status) {
                case "Submitted":
                    statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                    viewAssignmentButton.setText("View Submission");
                    break;
                case "Overdue":
                    statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                    viewAssignmentButton.setText("View Assignment");
                    break;
                case "In Progress":
                    statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
                    viewAssignmentButton.setText("Continue");
                    break;
                case "Graded":
                    statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark));
                    viewAssignmentButton.setText("View Grade");
                    break;
                default: // "Not Started"
                    statusTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                    viewAssignmentButton.setText("Start Assignment");
                    break;
            }
        }

        private String getAssignmentStatus(Assignment assignment) {
            // Check if assignment has a submission status
//            if (assignment.getSubmissionStatus() != null) {
//                switch (assignment.getSubmissionStatus()) {
//                    case "submitted":
//                        return assignment.getGrade() != null ? "Graded" : "Submitted";
//                    case "in_progress":
//                        return "In Progress";
//                    case "graded":
//                        return "Graded";
//                    default:
//                        break;
//                }
//            }
//
//            // Check if assignment is overdue
//            if (assignment.getDueDate() != null) {
//                Date currentDate = new Date();
//                if (assignment.getDueDate().before(currentDate)) {
//                    return "Overdue";
//                }
//            }

            // Default status
            return "Not Started";
        }

    }
}