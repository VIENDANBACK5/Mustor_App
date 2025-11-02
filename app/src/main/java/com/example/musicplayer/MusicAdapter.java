package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

    private final Context context;
    private final List<Song> songs;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String trackId);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public MusicAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song, listener);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvSongTitle, tvArtist, tvDuration, tvLikes, tvPlays, btnMore;
        ImageView ivAlbumCover;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvPlays = itemView.findViewById(R.id.tvPlays);
            btnMore = itemView.findViewById(R.id.btnMore);
        }

        public void bind(final Song song, final OnItemClickListener listener) {
            tvRank.setText(String.valueOf(getAdapterPosition() + 1));
            tvSongTitle.setText(song.title);
            tvArtist.setText(song.artist);
            tvDuration.setText("0:30"); // Default preview duration

            if (getAdapterPosition() < 3) {
                tvRank.setTextColor(0xFFFFD700);
            } else {
                tvRank.setTextColor(0xFFB0B0B0);
            }

            Glide.with(itemView.getContext())
                    .load(song.cover)
                    .apply(new RequestOptions().transform(new RoundedCorners(16)))
                    .into(ivAlbumCover);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(song.id);
                }
            });
        }
    }
}
