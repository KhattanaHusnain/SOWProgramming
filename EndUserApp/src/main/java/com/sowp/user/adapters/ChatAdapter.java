package com.sowp.user.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
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

import com.sowp.user.models.ChatMessage;
import com.sowp.user.models.User;
import com.sowp.user.R;
import com.sowp.user.utils.Base64ImageUtils;
import com.sowp.user.utils.MessageFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private static final String TAG = "ChatAdapter";

    // Instance variables
    private MessageFormatter formatter;
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
    private final Map<String, SpannableString> formattedMessageCache = new HashMap<>();

    // View types for optimization
    private static final int VIEW_TYPE_CURRENT_USER = 1;
    private static final int VIEW_TYPE_OTHER_USER = 2;

    // Interfaces
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

    /**
     * Constructor
     */
    public ChatAdapter(Context context, List<ChatMessage> chatMessages, String currentUserEmail) {
        this.context = context;
        this.chatMessages = chatMessages;
        this.formatter = new MessageFormatter(); // Initialize formatter with repository
        this.currentUserEmail = currentUserEmail;
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * Set message action listener
     */
    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
    }

    /**
     * Set user profile click listener
     */
    public void setOnUserProfileClickListener(OnUserProfileClickListener listener) {
        this.profileClickListener = listener;
    }

    /**
     * Set user role
     */
    public void setUserRole(String role) {
        this.userRole = role;
    }

    /**
     * Cache user data for improved performance
     */
    public void cacheUserData(String email, User userData) {
        if (email != null && userData != null) {
            userCache.put(email, userData);

            if (userData.getPhoto() != null && !userData.getPhoto().isEmpty()) {
                try {
                    Bitmap bitmap = Base64ImageUtils.base64ToBitmap(userData.getPhoto());
                    if (bitmap != null) {
                        // Scale down if too large to prevent memory issues
                        int maxSize = 100; // dp
                        if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                            bitmap = Bitmap.createScaledBitmap(bitmap, maxSize, maxSize, true);
                        }
                        profileImageCache.put(email, bitmap);
                    }
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "OutOfMemoryError caching profile image for " + email, e);
                    clearOldestProfileImages();
                } catch (Exception e) {
                    Log.e(TAG, "Error caching profile image for " + email, e);
                }
            }
        }
    }

    /**
     * Get view type for optimization
     */
    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessages.get(position);
        boolean isCurrentUser = currentUserEmail != null &&
                currentUserEmail.equals(message.getEmail());
        return isCurrentUser ? VIEW_TYPE_CURRENT_USER : VIEW_TYPE_OTHER_USER;
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
        String messageText = chatMessage.getMessage();
        String messageEmail = chatMessage.getEmail();
        boolean isCurrentUser = getItemViewType(position) == VIEW_TYPE_CURRENT_USER;

        // Get or create formatted message with links
        SpannableString formattedMessage = getFormattedMessage(messageText, chatMessage.getId());

        if (isCurrentUser) {
            setupCurrentUserMessage(holder, chatMessage, formattedMessage, position);
        } else {
            setupOtherUserMessage(holder, chatMessage, formattedMessage, messageEmail, position);
        }
    }

    /**
     * Get formatted message from cache or create new one
     */
    private SpannableString getFormattedMessage(String messageText, String messageId) {
        String cacheKey = messageText + "_" + messageId;

        if (formattedMessageCache.containsKey(cacheKey)) {
            return formattedMessageCache.get(cacheKey);
        }

        // Use the new main formatting method
        SpannableString formattedMessage = formatter.formatComplete(messageText, context);
        formattedMessageCache.put(cacheKey, formattedMessage);

        // Limit cache size to prevent memory issues
        if (formattedMessageCache.size() > 100) {
            clearOldestFormattedMessages();
        }

        return formattedMessage;
    }

    /**
     * Setup current user message display
     */
    private void setupCurrentUserMessage(ChatViewHolder holder, ChatMessage chatMessage,
                                         SpannableString formattedMessage, int position) {
        // Show current user layout, hide others
        holder.currentUserMessageContainer.setVisibility(View.VISIBLE);
        holder.othersMessageContainer.setVisibility(View.GONE);

        // Set message content with clickable links
        holder.messageCurrentUser.setText(formattedMessage);
        holder.messageCurrentUser.setMovementMethod(LinkMovementMethod.getInstance());
        holder.messageCurrentUser.setHighlightColor(Color.TRANSPARENT);

        holder.messageTimeCurrentUser.setText(chatMessage.getFormattedDateTime());

        // Show or hide username for current user (usually hidden)
        if (holder.usernameCurrentUser != null) {
            holder.usernameCurrentUser.setText("You");
            holder.usernameCurrentUser.setVisibility(View.GONE); // Usually hidden for current user
        }

        // Setup long press context menu
        holder.setupLongPressListener(chatMessage, position, currentUserEmail, userRole, actionListener, context);
    }

    /**
     * Setup other user message display
     */
    private void setupOtherUserMessage(ChatViewHolder holder, ChatMessage chatMessage,
                                       SpannableString formattedMessage, String messageEmail, int position) {
        // Show others layout, hide current user
        holder.othersMessageContainer.setVisibility(View.VISIBLE);
        holder.currentUserMessageContainer.setVisibility(View.GONE);

        // Set message content with clickable links
        holder.messageOthers.setText(formattedMessage);
        holder.messageOthers.setMovementMethod(LinkMovementMethod.getInstance());
        holder.messageOthers.setHighlightColor(Color.TRANSPARENT);

        holder.messageTimeOthers.setText(chatMessage.getFormattedDateTime());

        // Set username - get from cache or load from database
        setUsernameFromCache(holder.usernameOthers, messageEmail);

        // Setup profile image and click listener
        setupProfileImage(holder.profileImage, messageEmail);
        setupProfileClickListener(holder.profileImage, messageEmail);

        // Setup long press context menu
        holder.setupLongPressListener(chatMessage, position, currentUserEmail, userRole, actionListener, context);
    }

    /**
     * Set username from cache or load it
     */
    private void setUsernameFromCache(TextView usernameView, String userEmail) {
        if (usernameView == null) return;

        if (userCache.containsKey(userEmail)) {
            User userData = userCache.get(userEmail);
            if (userData != null && userData.getFullName() != null && !userData.getFullName().isEmpty()) {
                usernameView.setText(userData.getFullName());
            } else {
                // Extract name from email as fallback
                String emailName = userEmail.split("@")[0];
                usernameView.setText(capitalizeFirst(emailName));
            }
            usernameView.setVisibility(View.VISIBLE);
        } else {
            // Load user data to get name
            String emailName = userEmail.split("@")[0];
            usernameView.setText(capitalizeFirst(emailName));
            usernameView.setVisibility(View.VISIBLE);

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

    /**
     * Capitalize first letter of string
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.length() == 0) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Setup profile image
     */
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

    /**
     * Load profile image from user data
     */
    private void loadProfileImageFromUserData(ImageView profileImageView, String userEmail, User userData) {
        if (profileImageView == null) return;

        if (userData != null && userData.getPhoto() != null && !userData.getPhoto().isEmpty()) {
            try {
                // Add size check to prevent OutOfMemoryError
                String base64Image = userData.getPhoto();
                if (base64Image.length() > 1024 * 1024) { // 1MB limit
                    Log.w(TAG, "Profile image too large for user: " + userEmail);
                    setDefaultProfileImage(profileImageView);
                    return;
                }

                Bitmap bitmap = Base64ImageUtils.base64ToBitmap(base64Image);
                if (bitmap != null) {
                    // Scale down if too large
                    int maxSize = 100; // dp
                    if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, maxSize, maxSize, true);
                    }

                    profileImageView.setImageBitmap(bitmap);
                    profileImageCache.put(userEmail, bitmap);
                } else {
                    setDefaultProfileImage(profileImageView);
                }
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "OutOfMemoryError loading profile image", e);
                setDefaultProfileImage(profileImageView);
                // Clear some cache to free memory
                clearOldestProfileImages();
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile image from base64", e);
                setDefaultProfileImage(profileImageView);
            }
        } else {
            setDefaultProfileImage(profileImageView);
        }
    }

    /**
     * Set default profile image
     */
    private void setDefaultProfileImage(ImageView profileImageView) {
        if (profileImageView == null) return;

        try {
            profileImageView.setImageResource(R.drawable.ic_profile);
        } catch (Exception e) {
            Log.e(TAG, "Error setting default profile image", e);
        }
    }

    /**
     * Setup profile image click listener
     */
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

    /**
     * Set current user email and refresh display
     */
    public void setCurrentUserEmail(String email) {
        this.currentUserEmail = email;
        notifyDataSetChanged();
    }

    /**
     * Preload group member data for better performance
     */
    public void preloadGroupMembers(List<String> memberEmails) {
        // Pre-load user data for all group members to improve performance
        if (profileClickListener != null) {
            for (String email : memberEmails) {
                if (!userCache.containsKey(email)) {
                    profileClickListener.loadUserData(email, new UserDataCallback() {
                        @Override
                        public void onUserDataLoaded(User user) {
                            cacheUserData(email, user);
                        }

                        @Override
                        public void onUserDataLoadFailed(String error) {
                            Log.w(TAG, "Failed to preload user data for: " + email);
                        }
                    });
                }
            }
        }
    }

    /**
     * Clear all caches and free memory
     */
    public void clearCaches() {
        userCache.clear();

        // Recycle bitmaps before clearing
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
        formattedMessageCache.clear();
    }

    /**
     * Clear formatted message cache
     */
    public void clearFormattedMessageCache() {
        formattedMessageCache.clear();
    }

    /**
     * Clear oldest formatted messages to manage memory
     */
    private void clearOldestFormattedMessages() {
        if (formattedMessageCache.size() > 50) {
            // Simple FIFO approach - remove oldest entries
            int toRemove = formattedMessageCache.size() - 40;
            var iterator = formattedMessageCache.entrySet().iterator();
            for (int i = 0; i < toRemove && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    /**
     * Clear oldest profile images to manage memory
     */
    private void clearOldestProfileImages() {
        if (profileImageCache.size() > 20) { // Keep only 20 recent images
            // Remove oldest entries (simple FIFO approach)
            int toRemove = profileImageCache.size() - 15;
            var iterator = profileImageCache.entrySet().iterator();
            for (int i = 0; i < toRemove && iterator.hasNext(); i++) {
                var entry = iterator.next();
                Bitmap bitmap = entry.getValue();
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                iterator.remove();
            }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        clearCaches();
    }

    /**
     * ViewHolder class for chat items
     */
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

        // Context menu data
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

            // Enable text selection
            if (messageOthers != null) {
                messageOthers.setTextIsSelectable(true);
            }
            if (messageCurrentUser != null) {
                messageCurrentUser.setTextIsSelectable(true);
            }
        }

        /**
         * Setup long press listener for context menu
         */
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

        /**
         * Handle context menu item selection
         */
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

        /**
         * Copy message to clipboard
         */
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