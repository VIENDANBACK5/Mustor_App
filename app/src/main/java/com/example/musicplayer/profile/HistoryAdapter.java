package com.example.musicplayer.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.api.HistoryResponse;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<HistoryResponse.HistoryItem> historyItems;

    public HistoryAdapter(List<HistoryResponse.HistoryItem> historyItems) {
        this.historyItems = historyItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryResponse.HistoryItem item = historyItems.get(position);
        holder.textViewTrackName.setText(item.trackName);

        // Format: Artist - Time (e.g., "Hoang Thuy Linh - 2025-11-02 17:29")
        String artistAndTime = item.artistName + " - " + formatTime(item.listenedAt);
        holder.textViewArtistAndTime.setText(artistAndTime);
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    private String formatTime(String listenedAt) {
        // Convert "2025-11-02T17:29:31.041548" to "2025-11-02 17:29"
        if (listenedAt != null && listenedAt.length() >= 16) {
            return listenedAt.substring(0, 16).replace("T", " ");
        }
        return listenedAt;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTrackName;
        public TextView textViewArtistAndTime;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewTrackName = itemView.findViewById(R.id.textViewTrackName);
            textViewArtistAndTime = itemView.findViewById(R.id.textViewArtistAndTime);
        }
    }
}
