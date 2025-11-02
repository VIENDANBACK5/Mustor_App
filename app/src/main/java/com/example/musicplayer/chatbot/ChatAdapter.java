package com.example.musicplayer.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.roleView.setText(msg.getRole());
        holder.contentView.setText(msg.getContent());

        // style simple theo role
        String role = msg.getRole();
        if ("user".equalsIgnoreCase(role)) {
            holder.roleView.setTextColor(holder.itemView.getResources().getColor(android.R.color.holo_blue_dark));
        } else if ("assistant".equalsIgnoreCase(role)) {
            holder.roleView.setTextColor(holder.itemView.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.roleView.setTextColor(holder.itemView.getResources().getColor(android.R.color.darker_gray));
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView roleView, contentView;
        ViewHolder(View itemView) {
            super(itemView);
            roleView = itemView.findViewById(R.id.roleView);
            contentView = itemView.findViewById(R.id.contentView);
        }
    }
}
