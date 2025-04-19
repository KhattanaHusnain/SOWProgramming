package com.android.nexcode.groupchat;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    List<ChatMessage> chatMessages;
    Context context;
    public ChatAdapter(Context context, List<ChatMessage> chatMessages) {
        this.context = context;
        this.chatMessages = chatMessages;
    }
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);
        // Bind chat message data to the view holder
        holder.email.setText(chatMessage.getEmail());
        if (FirebaseAuth.getInstance().getCurrentUser().getEmail().equals(chatMessage.getEmail())){
            holder.chat_layout.setGravity(Gravity.END);
        }
        holder.message.setText(chatMessage.getMessage());
        holder.message_time.setText(chatMessage.getTimeStamp());
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
    public class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView email;
        TextView message;
        TextView message_time;
        LinearLayout chat_layout;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views in the chat item layout
            email = itemView.findViewById(R.id.email);
            message = itemView.findViewById(R.id.message);
            message_time = itemView.findViewById(R.id.message_time);
            chat_layout = itemView.findViewById(R.id.chat_layout);
        }
    }
}
