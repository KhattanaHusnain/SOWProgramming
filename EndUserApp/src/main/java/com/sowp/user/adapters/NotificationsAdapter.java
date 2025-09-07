package com.sowp.user.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.R;
import com.sowp.user.models.Notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    private Context context;
    private List<Notification> notifications;
    private SimpleDateFormat dateFormat;

    public NotificationsAdapter(Context context, List<Notification> notifications) {
        this.context = context;
        this.notifications = notifications;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        // Set notification ID
        holder.tvNotificationId.setText("Notification #" + notification.getNotificationId());

        // Set content
        holder.tvNotificationContent.setText(notification.getContent());

        // Set created date
        holder.tvCreatedDate.setText(dateFormat.format(new Date(notification.getCreatedAt())));

        // Set expiry date
        holder.tvExpiryDate.setText(dateFormat.format(new Date(notification.getExpiry())));

        // Set time ago
        holder.tvTimeAgo.setText(getTimeAgo(notification.getCreatedAt()));

        // Change color if expired
        if (notification.isExpired()) {
            holder.tvExpiryDate.setTextColor(context.getResources().getColor(R.color.error));
            holder.itemView.setAlpha(0.7f);
        } else {
            holder.tvExpiryDate.setTextColor(context.getResources().getColor(R.color.warning));
            holder.itemView.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private String getTimeAgo(long createdAt) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - createdAt;

        if (timeDiff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (timeDiff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff);
            return minutes + "m ago";
        } else if (timeDiff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(timeDiff);
            return hours + "h ago";
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(timeDiff);
            return days + "d ago";
        }
    }

    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    public void addNotification(Notification notification) {
        // Add new notification at the beginning (most recent first)
        notifications.add(0, notification);
        notifyItemInserted(0);

        // Keep only recent 20 notifications
        if (notifications.size() > 20) {
            int removeCount = notifications.size() - 20;
            for (int i = 0; i < removeCount; i++) {
                notifications.remove(notifications.size() - 1);
            }
            notifyItemRangeRemoved(20, removeCount);
        }
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvNotificationId, tvNotificationContent, tvCreatedDate, tvExpiryDate, tvTimeAgo;
        ImageView ivNotificationIcon;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNotificationId = itemView.findViewById(R.id.tv_notification_id);
            tvNotificationContent = itemView.findViewById(R.id.tv_notification_content);
            tvCreatedDate = itemView.findViewById(R.id.tv_created_date);
            tvExpiryDate = itemView.findViewById(R.id.tv_expiry_date);
            tvTimeAgo = itemView.findViewById(R.id.tv_time_ago);
            ivNotificationIcon = itemView.findViewById(R.id.iv_notification_icon);
        }
    }
}