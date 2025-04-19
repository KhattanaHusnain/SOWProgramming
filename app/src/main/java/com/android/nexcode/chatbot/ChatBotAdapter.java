package com.android.nexcode.chatbot;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.R;

import java.util.List;

public class ChatBotAdapter extends RecyclerView.Adapter<ChatBotAdapter.ChatViewHolder> {
    private List<ChatBotMessage> messages;

    public ChatBotAdapter(List<ChatBotMessage> messages) {
        this.messages = messages;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chatbot_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        ChatBotMessage message = messages.get(position);
        holder.messageText.setText(message.getMessage());

        if (message.isUser()) {
            holder.messageText.setBackgroundResource(R.drawable.user_message_bg);
            holder.messageLayout.setGravity(Gravity.END);
        } else {
            holder.messageText.setBackgroundResource(R.drawable.bot_message_bg);
            holder.messageLayout.setGravity(Gravity.START);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        LinearLayout messageLayout;

        ChatViewHolder(View view) {
            super(view);
            messageText = view.findViewById(R.id.messageText);
            messageLayout = view.findViewById(R.id.messageLayout);
        }
    }
}
