package com.example.musicplayer.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DeezerTrack {
    @SerializedName("id")
    public String id;

    @SerializedName("title")
    public String name;

    @SerializedName("artist")
    public String artist;

    @SerializedName("album")
    public String album;

    @SerializedName("duration")
    public int duration;

    @SerializedName("rank")
    public int popularity;

    @SerializedName("preview_url")
    public String previewUrl;

    @SerializedName("link")
    public String deezerUrl;

    @SerializedName("cover_url")
    public String imageUrl;

    // Helper method for compatibility
    public String getArtistsString() {
        return artist != null ? artist : "Unknown Artist";
    }

    // Helper to get duration in milliseconds for compatibility
    public int getDurationMs() {
        return duration * 1000;
    }
}
