package com.android.nexcode;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private Context context;
    private List<Course> courseList;

    // Constructor
    public CourseAdapter(Context context, List<Course> courseList) {
        this.context = context;
        this.courseList = courseList;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.course_item, parent, false);
        return new CourseViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);

        // Set course data to the views
        holder.courseImage.setImageResource(course.getIllustration());
        holder.courseName.setText(course.getTitle());
        holder.numberOfLearners.setText(course.getMembers() + " Learning");

        // Handling click on Open Course button
        holder.openCourseButton.setOnClickListener(view -> {
            Intent intent = new Intent(context, Description.class);
            intent.putExtra("Title", course.getTitle());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    // ViewHolder class
    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        ImageView courseImage;
        TextView courseName;
        TextView numberOfLearners;
        ImageButton openCourseButton;
        CardView courseCard;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            courseImage = itemView.findViewById(R.id.course_image);
            courseName = itemView.findViewById(R.id.course_title);
            numberOfLearners = itemView.findViewById(R.id.course_members);
            openCourseButton = itemView.findViewById(R.id.open_course_button);
            courseCard = itemView.findViewById(R.id.course_card);
        }
    }


}
