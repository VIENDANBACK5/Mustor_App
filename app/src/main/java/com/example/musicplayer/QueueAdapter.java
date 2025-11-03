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
import com.example.musicplayer.R;
import com.example.musicplayer.Song;

import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {

    private final Context context;
    private final List<Song> songs;
    private OnItemClickListener clickListener;
    private OnDeleteListener deleteListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    public QueueAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_queue, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song, position, clickListener, deleteListener);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongTitle, tvArtist, tvPosition;
        ImageView ivAlbumCover, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPosition = itemView.findViewById(R.id.tvPosition);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(final Song song, int position,
                         final OnItemClickListener clickListener,
                         final OnDeleteListener deleteListener) {
            tvPosition.setText(String.valueOf(position + 1));
            tvSongTitle.setText(song.title);
            tvArtist.setText(song.artist);

            Glide.with(itemView.getContext())
                    .load(song.cover)
                    .apply(new RequestOptions().transform(new RoundedCorners(12)))
                    .into(ivAlbumCover);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(getAdapterPosition());
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(getAdapterPosition());
                }
            });
        }
    }
}