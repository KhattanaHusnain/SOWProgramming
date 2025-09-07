package com.sowp.user.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.models.ChatMessage;
import com.sowp.user.models.User;
import com.sowp.user.R;
import com.sowp.user.services.ImageService;
import com.sowp.user.services.MessageFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private static final int VIEW_TYPE_CURRENT_USER = 1;
    private static final int VIEW_TYPE_OTHER_USER = 2;
    private static final int MAX_CACHED_MESSAGES = 100;
    private static final int MAX_CACHED_IMAGES = 20;
    private static final int MAX_IMAGE_SIZE = 100;
    private static final int MAX_BASE64_SIZE = 1024 * 1024;

    private MessageFormatter formatter;
    private final List<ChatMessage> chatMessages;
    private final Context context;
    private String currentUserEmail;
    private String userRole;
    private final LayoutInflater inflater;
    private OnMessageActionListener actionListener;
    private OnUserProfileClickListener profileClickListener;

    private final Map<String, User> userCache = new HashMap<>();
    private final Map<String, Bitmap> profileImageCache = new HashMap<>();
    private final Map<String, SpannableString> formattedMessageCache = new HashMap<>();

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
        this.formatter = new MessageFormatter();
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
                    Bitmap bitmap = ImageService.base64ToBitmap(userData.getPhoto());
                    if (bitmap != null) {
                        if (bitmap.getWidth() > MAX_IMAGE_SIZE || bitmap.getHeight() > MAX_IMAGE_SIZE) {
                            bitmap = Bitmap.createScaledBitmap(bitmap, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE, true);
                        }
                        profileImageCache.put(email, bitmap);
                    }
                } catch (OutOfMemoryError e) {
                    clearOldestProfileImages();
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessages.get(position);
        boolean isCurrentUser = currentUserEmail != null && currentUserEmail.equals(message.getEmail());
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

        SpannableString formattedMessage = getFormattedMessage(messageText, chatMessage.getId());

        if (isCurrentUser) {
            setupCurrentUserMessage(holder, chatMessage, formattedMessage, position);
        } else {
            setupOtherUserMessage(holder, chatMessage, formattedMessage, messageEmail, position);
        }
    }

    private SpannableString getFormattedMessage(String messageText, String messageId) {
        String cacheKey = messageText + "_" + messageId;

        if (formattedMessageCache.containsKey(cacheKey)) {
            return formattedMessageCache.get(cacheKey);
        }

        SpannableString formattedMessage = formatter.formatComplete(messageText, context);
        formattedMessageCache.put(cacheKey, formattedMessage);

        if (formattedMessageCache.size() > MAX_CACHED_MESSAGES) {
            clearOldestFormattedMessages();
        }

        return formattedMessage;
    }

    private void setupCurrentUserMessage(ChatViewHolder holder, ChatMessage chatMessage,
                                         SpannableString formattedMessage, int position) {
        holder.currentUserMessageContainer.setVisibility(View.VISIBLE);
        holder.othersMessageContainer.setVisibility(View.GONE);

        holder.messageCurrentUser.setText(formattedMessage);
        holder.messageCurrentUser.setMovementMethod(LinkMovementMethod.getInstance());
        holder.messageCurrentUser.setHighlightColor(Color.TRANSPARENT);
        holder.messageTimeCurrentUser.setText(chatMessage.getFormattedDateTime());

        if (holder.usernameCurrentUser != null) {
            holder.usernameCurrentUser.setText("You");
            holder.usernameCurrentUser.setVisibility(View.GONE);
        }

        holder.setupLongPressListener(chatMessage, position, currentUserEmail, userRole, actionListener, context);
    }

    private void setupOtherUserMessage(ChatViewHolder holder, ChatMessage chatMessage,
                                       SpannableString formattedMessage, String messageEmail, int position) {
        holder.othersMessageContainer.setVisibility(View.VISIBLE);
        holder.currentUserMessageContainer.setVisibility(View.GONE);

        holder.messageOthers.setText(formattedMessage);
        holder.messageOthers.setMovementMethod(LinkMovementMethod.getInstance());
        holder.messageOthers.setHighlightColor(Color.TRANSPARENT);
        holder.messageTimeOthers.setText(chatMessage.getFormattedDateTime());

        setUsernameFromCache(holder.usernameOthers, messageEmail);
        setupProfileImage(holder.profileImage, messageEmail);
        setupProfileClickListener(holder.profileImage, messageEmail);
        holder.setupLongPressListener(chatMessage, position, currentUserEmail, userRole, actionListener, context);
    }

    private void setUsernameFromCache(TextView usernameView, String userEmail) {
        if (usernameView == null) return;

        if (userCache.containsKey(userEmail)) {
            User userData = userCache.get(userEmail);
            if (userData != null && userData.getFullName() != null && !userData.getFullName().isEmpty()) {
                usernameView.setText(userData.getFullName());
            } else {
                String emailName = userEmail.split("@")[0];
                usernameView.setText(capitalizeFirst(emailName));
            }
            usernameView.setVisibility(View.VISIBLE);
        } else {
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

        if (profileImageCache.containsKey(userEmail)) {
            Bitmap cachedBitmap = profileImageCache.get(userEmail);
            if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                try {
                    profileImageView.setImageBitmap(cachedBitmap);
                    return;
                } catch (Exception e) {
                }
            }
        }

        if (userCache.containsKey(userEmail)) {
            User userData = userCache.get(userEmail);
            loadProfileImageFromUserData(profileImageView, userEmail, userData);
        } else {
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
                    }
                });
            }
        }
    }

    private void loadProfileImageFromUserData(ImageView profileImageView, String userEmail, User userData) {
        if (profileImageView == null) return;

        if (userData != null && userData.getPhoto() != null && !userData.getPhoto().isEmpty()) {
            try {
                String base64Image = userData.getPhoto();
                if (base64Image.length() > MAX_BASE64_SIZE) {
                    setDefaultProfileImage(profileImageView);
                    return;
                }

                Bitmap bitmap = ImageService.base64ToBitmap(base64Image);
                if (bitmap != null) {
                    if (bitmap.getWidth() > MAX_IMAGE_SIZE || bitmap.getHeight() > MAX_IMAGE_SIZE) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE, true);
                    }

                    profileImageView.setImageBitmap(bitmap);
                    profileImageCache.put(userEmail, bitmap);
                } else {
                    setDefaultProfileImage(profileImageView);
                }
            } catch (OutOfMemoryError e) {
                setDefaultProfileImage(profileImageView);
                clearOldestProfileImages();
            } catch (Exception e) {
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

    public void preloadGroupMembers(List<String> memberEmails) {
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
                        }
                    });
                }
            }
        }
    }

    public void clearCaches() {
        userCache.clear();

        for (Bitmap bitmap : profileImageCache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                try {
                    bitmap.recycle();
                } catch (Exception e) {
                }
            }
        }
        profileImageCache.clear();
        formattedMessageCache.clear();
    }

    public void clearFormattedMessageCache() {
        formattedMessageCache.clear();
    }

    private void clearOldestFormattedMessages() {
        if (formattedMessageCache.size() > 50) {
            int toRemove = formattedMessageCache.size() - 40;
            var iterator = formattedMessageCache.entrySet().iterator();
            for (int i = 0; i < toRemove && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    private void clearOldestProfileImages() {
        if (profileImageCache.size() > MAX_CACHED_IMAGES) {
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

    public static class ChatViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        final LinearLayout othersMessageContainer;
        final TextView usernameOthers;
        final TextView messageOthers;
        final TextView messageTimeOthers;
        final ImageView profileImage;

        final LinearLayout currentUserMessageContainer;
        final TextView usernameCurrentUser;
        final TextView messageCurrentUser;
        final TextView messageTimeCurrentUser;

        private ChatMessage currentMessage;
        private int currentPosition;
        private String currentUserEmail;
        private String userRole;
        private OnMessageActionListener actionListener;
        private Context context;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            othersMessageContainer = itemView.findViewById(R.id.others_message_container);
            usernameOthers = itemView.findViewById(R.id.username_others);
            messageOthers = itemView.findViewById(R.id.message_others);
            messageTimeOthers = itemView.findViewById(R.id.message_time_others);
            profileImage = itemView.findViewById(R.id.profile_image);

            currentUserMessageContainer = itemView.findViewById(R.id.current_user_message_container);
            usernameCurrentUser = itemView.findViewById(R.id.username_current_user);
            messageCurrentUser = itemView.findViewById(R.id.message_current_user);
            messageTimeCurrentUser = itemView.findViewById(R.id.message_time_current_user);

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
            }
        }

        private boolean onContextItemSelected(MenuItem item) {
            try {
                switch (item.getItemId()) {
                    case 1:
                        copyMessageToClipboard();
                        return true;
                    case 2:
                        if (actionListener != null) {
                            actionListener.onDeleteMessage(currentMessage, currentPosition);
                        }
                        return true;
                    case 3:
                        if (actionListener != null) {
                            actionListener.onDeleteForEveryone(currentMessage, currentPosition);
                        }
                        return true;
                    default:
                        return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        private void copyMessageToClipboard() {
            try {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Chat Message", currentMessage.getMessage());
                clipboard.setPrimaryClip(clip);
            } catch (Exception e) {
            }
        }
    }
}