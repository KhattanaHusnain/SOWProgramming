package com.android.SOWProgramming.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.SpannableString;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.SOWProgramming.models.ChatMessage;
import com.android.SOWProgramming.models.User;
import com.android.SOWProgramming.R;
import com.android.SOWProgramming.utils.Base64ImageUtils;
import com.android.SOWProgramming.utils.MessageFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private static final String TAG = "ChatAdapter";

    private final List<ChatMessage> chatMessages;
    private final Context context;
    private String currentUserEmail;
    private String userRole;
    private final LayoutInflater inflater;
    private OnMessageActionListener actionListener;
    private OnUserProfileClickListener profileClickListener;

    // Cache for user profile data
    private final Map<String, User> userCache = new HashMap<>();
    private final Map<String, Bitmap> profileImageCache = new HashMap<>();

    public interface OnMessageActionListener {
        void onDeleteMessage(ChatMessage message, int position);
        void onDeleteForEveryone(ChatMessage message, int position);
    }

    public interface OnUserProfileClickListener {
        void onUserProfileClick(String userEmail, User userData);
        void loadUserData(String userEmail, UserDataCallback callback);
    }

    public interface UserDataCallback {
        void onUserDataLoaded(User user);
        void onUserDataLoadFailed(String error);
    }

    public ChatAdapter(Context context, List<ChatMessage> chatMessages, String currentUserEmail) {
        this.context = context;
        this.chatMessages = chatMessages;
        this.currentUserEmail = currentUserEmail;
        this.inflater = LayoutInflater.from(context);
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
    }

    public void setOnUserProfileClickListener(OnUserProfileClickListener listener) {
        this.profileClickListener = listener;
    }

    public void setUserRole(String role) {
        this.userRole = role;
    }

    public void cacheUserData(String email, User userData) {
        if (email != null && userData != null) {
            userCache.put(email, userData);

            if (userData.getPhoto() != null && !userData.getPhoto().isEmpty()) {
                try {
                    Bitmap bitmap = Base64ImageUtils.base64ToBitmap(userData.getPhoto());
                    if (bitmap != null) {
                        profileImageCache.put(email, bitmap);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error caching profile image for " + email, e);
                }
            }
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);
        String messageEmail = chatMessage.getEmail();
        boolean isCurrentUser = currentUserEmail != null && currentUserEmail.equals(messageEmail);

        if (isCurrentUser) {
            setupCurrentUserMessage(holder, chatMessage, position);
        } else {
            setupOtherUserMessage(holder, chatMessage, messageEmail, position);
        }
    }

    private void setupCurrentUserMessage(ChatViewHolder holder, ChatMessage chatMessage, int position) {
        // Show current user layout, hide others
        holder.currentUserMessageContainer.setVisibility(View.VISIBLE);
        holder.othersMessageContainer.setVisibility(View.GONE);

        // Set message content
        try {
            SpannableString formattedMsg = MessageFormatter.formatMessage(chatMessage.getMessage());
            holder.messageCurrentUser.setText(formattedMsg);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting message", e);
            holder.messageCurrentUser.setText(chatMessage.getMessage());
        }

        holder.messageTimeCurrentUser.setText(chatMessage.getFormattedDateTime());
        holder.usernameCurrentUser.setText("You");

        // Setup profile image and click listener
        setupProfileImage(holder.profileImageCurrentUser, currentUserEmail);
        setupProfileClickListener(holder.profileImageCurrentUser, currentUserEmail);

        // Setup long press
        holder.setupLongPressListener(chatMessage, position, currentUserEmail, userRole, actionListener, context);

        // Enable link clicking
        holder.messageCurrentUser.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    private void setupOtherUserMessage(ChatViewHolder holder, ChatMessage chatMessage, String messageEmail, int position) {
        // Show others layout, hide current user
        holder.othersMessageContainer.setVisibility(View.VISIBLE);
        holder.currentUserMessageContainer.setVisibility(View.GONE);

        // Set message content
        try {
            SpannableString formattedMsg = MessageFormatter.formatMessage(chatMessage.getMessage());
            holder.messageOthers.setText(formattedMsg);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting message", e);
            holder.messageOthers.setText(chatMessage.getMessage());
        }

        holder.messageTimeOthers.setText(chatMessage.getFormattedDateTime());

        // Set username - get from cache or load from database
        setUsernameFromCache(holder.usernameOthers, messageEmail);

        // Setup profile image and click listener
        setupProfileImage(holder.profileImage, messageEmail);
        setupProfileClickListener(holder.profileImage, messageEmail);

        // Setup long press
        holder.setupLongPressListener(chatMessage, position, currentUserEmail, userRole, actionListener, context);

        // Enable link clicking
        holder.messageOthers.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    private void setUsernameFromCache(TextView usernameView, String userEmail) {
        if (userCache.containsKey(userEmail)) {
            User userData = userCache.get(userEmail);
            if (userData != null && userData.getFullName() != null && !userData.getFullName().isEmpty()) {
                usernameView.setText(userData.getFullName());
            } else {
                // Extract name from email as fallback
                String emailName = userEmail.split("@")[0];
                usernameView.setText(capitalizeFirst(emailName));
            }
        } else {
            // Load user data to get name
            String emailName = userEmail.split("@")[0];
            usernameView.setText(capitalizeFirst(emailName));

            if (profileClickListener != null) {
                profileClickListener.loadUserData(userEmail, new UserDataCallback() {
                    @Override
                    public void onUserDataLoaded(User user) {
                        cacheUserData(userEmail, user);
                        if (user.getFullName() != null && !user.getFullName().isEmpty()) {
                            usernameView.setText(user.getFullName());
                        }
                    }

                    @Override
                    public void onUserDataLoadFailed(String error) {
                        Log.w(TAG, "Failed to load user data for username: " + error);
                    }
                });
            }
        }
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.length() == 0) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void setupProfileImage(ImageView profileImageView, String userEmail) {
        if (profileImageView == null) return;

        // Check cache first
        if (profileImageCache.containsKey(userEmail)) {
            Bitmap cachedBitmap = profileImageCache.get(userEmail);
            if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                try {
                    profileImageView.setImageBitmap(cachedBitmap);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Error setting cached bitmap", e);
                }
            }
        }

        // Check user data cache
        if (userCache.containsKey(userEmail)) {
            User userData = userCache.get(userEmail);
            loadProfileImageFromUserData(profileImageView, userEmail, userData);
        } else {
            // Set default and load user data
            setDefaultProfileImage(profileImageView);

            if (profileClickListener != null) {
                profileClickListener.loadUserData(userEmail, new UserDataCallback() {
                    @Override
                    public void onUserDataLoaded(User user) {
                        cacheUserData(userEmail, user);
                        loadProfileImageFromUserData(profileImageView, userEmail, user);
                    }

                    @Override
                    public void onUserDataLoadFailed(String error) {
                        Log.w(TAG, "Failed to load user data for profile image: " + error);
                    }
                });
            }
        }
    }

    private void loadProfileImageFromUserData(ImageView profileImageView, String userEmail, User userData) {
        if (profileImageView == null) return;

        if (userData != null && userData.getPhoto() != null && !userData.getPhoto().isEmpty()) {
            try {
                Bitmap bitmap = Base64ImageUtils.base64ToBitmap(userData.getPhoto());
                if (bitmap != null) {
                    profileImageView.setImageBitmap(bitmap);
                    profileImageCache.put(userEmail, bitmap);
                } else {
                    setDefaultProfileImage(profileImageView);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile image from base64", e);
                setDefaultProfileImage(profileImageView);
            }
        } else {
            setDefaultProfileImage(profileImageView);
        }
    }

    private void setDefaultProfileImage(ImageView profileImageView) {
        if (profileImageView == null) return;

        try {
            profileImageView.setImageResource(R.drawable.ic_profile);
        } catch (Exception e) {
            Log.e(TAG, "Error setting default profile image", e);
        }
    }

    private void setupProfileClickListener(ImageView profileImageView, String userEmail) {
        if (profileImageView == null) return;

        profileImageView.setOnClickListener(v -> {
            if (profileClickListener != null) {
                if (userCache.containsKey(userEmail)) {
                    profileClickListener.onUserProfileClick(userEmail, userCache.get(userEmail));
                } else {
                    profileClickListener.loadUserData(userEmail, new UserDataCallback() {
                        @Override
                        public void onUserDataLoaded(User user) {
                            cacheUserData(userEmail, user);
                            profileClickListener.onUserProfileClick(userEmail, user);
                        }

                        @Override
                        public void onUserDataLoadFailed(String error) {
                            Toast.makeText(context, "Failed to load user profile: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public void setCurrentUserEmail(String email) {
        this.currentUserEmail = email;
        notifyDataSetChanged();
    }

    public void clearCaches() {
        userCache.clear();
        for (Bitmap bitmap : profileImageCache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                try {
                    bitmap.recycle();
                } catch (Exception e) {
                    Log.e(TAG, "Error recycling bitmap", e);
                }
            }
        }
        profileImageCache.clear();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        // Others' message views
        final LinearLayout othersMessageContainer;
        final TextView usernameOthers;
        final TextView messageOthers;
        final TextView messageTimeOthers;
        final ImageView profileImage;

        // Current user's message views
        final LinearLayout currentUserMessageContainer;
        final TextView usernameCurrentUser;
        final TextView messageCurrentUser;
        final TextView messageTimeCurrentUser;
        final ImageView profileImageCurrentUser;

        private ChatMessage currentMessage;
        private int currentPosition;
        private String currentUserEmail;
        private String userRole;
        private OnMessageActionListener actionListener;
        private Context context;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize others' message views
            othersMessageContainer = itemView.findViewById(R.id.others_message_container);
            usernameOthers = itemView.findViewById(R.id.username_others);
            messageOthers = itemView.findViewById(R.id.message_others);
            messageTimeOthers = itemView.findViewById(R.id.message_time_others);
            profileImage = itemView.findViewById(R.id.profile_image);

            // Initialize current user's message views
            currentUserMessageContainer = itemView.findViewById(R.id.current_user_message_container);
            usernameCurrentUser = itemView.findViewById(R.id.username_current_user);
            messageCurrentUser = itemView.findViewById(R.id.message_current_user);
            messageTimeCurrentUser = itemView.findViewById(R.id.message_time_current_user);
            profileImageCurrentUser = itemView.findViewById(R.id.profile_image_current_user);

            // Enable text selection
            if (messageOthers != null) {
                messageOthers.setTextIsSelectable(true);
            }
            if (messageCurrentUser != null) {
                messageCurrentUser.setTextIsSelectable(true);
            }
        }

        public void setupLongPressListener(ChatMessage chatMessage, int position, String currentUserEmail,
                                           String userRole, OnMessageActionListener actionListener, Context context) {
            this.currentMessage = chatMessage;
            this.currentPosition = position;
            this.currentUserEmail = currentUserEmail;
            this.userRole = userRole;
            this.actionListener = actionListener;
            this.context = context;

            itemView.setOnCreateContextMenuListener(this);
            itemView.setOnLongClickListener(v -> {
                try {
                    v.showContextMenu();
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Error showing context menu", e);
                    return false;
                }
            });
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            try {
                menu.add(0, 1, 1, "Copy");

                boolean isCurrentUserMessage = currentUserEmail != null &&
                        currentUserEmail.equals(currentMessage.getEmail());
                boolean isAdmin = "Admin".equals(userRole);

                menu.add(0, 2, 2, "Delete");

                if (isCurrentUserMessage || isAdmin) {
                    menu.add(0, 3, 3, "Delete For Everyone");
                }

                for (int i = 0; i < menu.size(); i++) {
                    MenuItem item = menu.getItem(i);
                    item.setOnMenuItemClickListener(this::onContextItemSelected);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating context menu", e);
            }
        }

        private boolean onContextItemSelected(MenuItem item) {
            try {
                switch (item.getItemId()) {
                    case 1: // Copy
                        copyMessageToClipboard();
                        return true;
                    case 2: // Delete
                        if (actionListener != null) {
                            actionListener.onDeleteMessage(currentMessage, currentPosition);
                        }
                        return true;
                    case 3: // Delete For Everyone
                        if (actionListener != null) {
                            actionListener.onDeleteForEveryone(currentMessage, currentPosition);
                        }
                        return true;
                    default:
                        return false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling context menu item selection", e);
                return false;
            }
        }

        private void copyMessageToClipboard() {
            try {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Chat Message", currentMessage.getMessage());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error copying message to clipboard", e);
                Toast.makeText(context, "Failed to copy message", Toast.LENGTH_SHORT).show();
            }
        }
    }
}