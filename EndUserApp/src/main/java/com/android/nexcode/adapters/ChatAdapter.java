package com.android.nexcode.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.SpannableString;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.models.ChatMessage;
import com.android.nexcode.R;
import com.android.nexcode.utils.MessageFormatter;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final List<ChatMessage> chatMessages;
    private final Context context;
    private String currentUserEmail;
    private String userRole; // "admin" or "user"
    private final LayoutInflater inflater;
    private OnMessageActionListener actionListener;

    // Interface for handling message actions
    public interface OnMessageActionListener {
        void onDeleteMessage(ChatMessage message, int position);
        void onDeleteForEveryone(ChatMessage message, int position);
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

    public void setUserRole(String role) {
        this.userRole = role;
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

        // Bind chat message data to the view holder
        String messageEmail = chatMessage.getEmail();
        holder.email.setText(messageEmail);
        SpannableString formattedMsg = MessageFormatter.formatMessage(chatMessage.getMessage());
        holder.message.setText(formattedMsg);
        holder.messageTime.setText(chatMessage.getFormattedDateTime());

        // Set up long press listener with position and message data
        holder.setupLongPressListener(chatMessage, position, currentUserEmail, userRole, actionListener, context);

        // Optimize comparison by using cached current user email
        if (currentUserEmail != null && currentUserEmail.equals(messageEmail)) {
            holder.chatLayout.setGravity(Gravity.END);
        } else {
            holder.chatLayout.setGravity(Gravity.START);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public void setCurrentUserEmail(String email) {
        this.currentUserEmail = email;
        notifyDataSetChanged();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        final TextView email;
        final TextView message;
        final TextView messageTime;
        final LinearLayout chatLayout;

        private ChatMessage currentMessage;
        private int currentPosition;
        private String currentUserEmail;
        private String userRole;
        private OnMessageActionListener actionListener;
        private Context context;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            email = itemView.findViewById(R.id.email);
            message = itemView.findViewById(R.id.message);
            messageTime = itemView.findViewById(R.id.message_time);
            chatLayout = itemView.findViewById(R.id.chat_layout);

            // Enable link clicking and text selection
            message.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            message.setTextIsSelectable(true);
        }

        public void setupLongPressListener(ChatMessage chatMessage, int position, String currentUserEmail,
                                           String userRole, OnMessageActionListener actionListener, Context context) {
            this.currentMessage = chatMessage;
            this.currentPosition = position;
            this.currentUserEmail = currentUserEmail;
            this.userRole = userRole;
            this.actionListener = actionListener;
            this.context = context;

            // Set context menu listener on the message TextView
            itemView.setOnCreateContextMenuListener(this);

            // Also set long click listener for better UX
            itemView.setOnLongClickListener(v -> {
                v.showContextMenu();
                return true;
            });
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            // Add Copy option (always available)
            menu.add(0, 1, 1, "Copy");

            boolean isCurrentUserMessage = currentUserEmail != null &&
                    currentUserEmail.equals(currentMessage.getEmail());
            boolean isAdmin = "Admin".equals(userRole);

            menu.add(0, 2, 2, "Delete");


            // Add Delete For Everyone option (only for own messages or admin)
            if (isCurrentUserMessage || isAdmin) {
                menu.add(0, 3, 3, "Delete For Everyone");
            }

            // Set menu item click listener
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                item.setOnMenuItemClickListener(this::onContextItemSelected);
            }
        }

        private boolean onContextItemSelected(MenuItem item) {
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
        }

        private void copyMessageToClipboard() {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Chat Message", currentMessage.getMessage());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}