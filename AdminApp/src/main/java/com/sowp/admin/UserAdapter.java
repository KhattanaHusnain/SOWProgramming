package com.sowp.admin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.admin.R;
import com.sowp.admin.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public UserAdapter(List<User> userList, Context context) {
        this.userList = userList;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void updateUsers(List<User> newUserList) {
        this.userList = newUserList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivProfile, ivVerificationStatus;
        private TextView tvFullName, tvEmail, tvRole, tvGender, tvDegree, tvSemester;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            ivProfile = itemView.findViewById(R.id.iv_profile);
            ivVerificationStatus = itemView.findViewById(R.id.iv_verification_status);
            tvFullName = itemView.findViewById(R.id.tv_full_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvRole = itemView.findViewById(R.id.tv_role);
            tvGender = itemView.findViewById(R.id.tv_gender);
            tvDegree = itemView.findViewById(R.id.tv_degree);
            tvSemester = itemView.findViewById(R.id.tv_semester);

            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onItemClickListener.onItemClick(userList.get(getAdapterPosition()));
                }
            });
        }

        public void bind(User user) {
            // Set user name
            tvFullName.setText(user.getDisplayName());

            // Set email
            tvEmail.setText(user.getEmail());

            // Set role with appropriate background color
            tvRole.setText(user.getRole());
            setRoleBackground(user.getRole());

            // Set verification status
            if (user.isEmailVerified()) {
                ivVerificationStatus.setVisibility(View.VISIBLE);
                ivVerificationStatus.setImageResource(R.drawable.ic_verified);
            } else {
                ivVerificationStatus.setVisibility(View.GONE);
            }

            // Set optional fields with visibility management
            setOptionalField(tvGender, user.getDisplayGender(), !"Not specified".equals(user.getDisplayGender()));
            setOptionalField(tvDegree, user.getDisplayDegree(), !"Not specified".equals(user.getDisplayDegree()));
            setOptionalField(tvSemester, user.getDisplaySemester(), !"Not specified".equals(user.getDisplaySemester()));

            // Set profile image
            setProfileImage(user.getPhoto());
        }

        private void setRoleBackground(String role) {
            int backgroundColor;
            switch (role.toLowerCase()) {
                case "admin":
                    backgroundColor = context.getResources().getColor(R.color.role_admin, null);
                    break;
                case "instructor":
                    backgroundColor = context.getResources().getColor(R.color.role_instructor, null);
                    break;
                case "user":
                default:
                    backgroundColor = context.getResources().getColor(R.color.role_user, null);
                    break;
            }
            tvRole.setBackgroundColor(backgroundColor);
        }

        private void setOptionalField(TextView textView, String value, boolean hasValue) {
            if (hasValue && value != null && !value.trim().isEmpty()) {
                textView.setText(value);
                textView.setVisibility(View.VISIBLE);
            } else {
                textView.setVisibility(View.GONE);
            }
        }

        private void setProfileImage(String base64Image) {
            if (base64Image != null && !base64Image.isEmpty()) {
                try {
                    // Remove data:image prefix if present
                    String cleanBase64 = base64Image;
                    if (base64Image.contains(",")) {
                        cleanBase64 = base64Image.substring(base64Image.indexOf(",") + 1);
                    }

                    byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    if (bitmap != null) {
                        ivProfile.setImageBitmap(bitmap);
                    } else {
                        ivProfile.setImageResource(R.drawable.ic_person);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ivProfile.setImageResource(R.drawable.ic_default_avatar);
                }
            } else {
                ivProfile.setImageResource(R.drawable.ic_default_avatar);
            }
        }
    }
}