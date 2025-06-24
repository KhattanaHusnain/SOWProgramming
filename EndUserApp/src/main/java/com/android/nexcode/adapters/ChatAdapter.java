package com.android.nexcode.adapters;

import android.content.Context;
import android.text.SpannableString;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private final LayoutInflater inflater;

    public ChatAdapter(Context context, List<ChatMessage> chatMessages, String currentUserEmail) {
        this.context = context;
        this.chatMessages = chatMessages;
        this.currentUserEmail = currentUserEmail;
        this.inflater = LayoutInflater.from(context); // Cache the inflater
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


    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        final TextView email;
        final TextView message;
        final TextView messageTime;
        final LinearLayout chatLayout;

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
    }
}