package com.example.musicplayer.api;

import com.google.gson.annotations.SerializedName;

/**
 * Response for POST /api/deezer/tracks/{track_id}/play
 * Tự động lưu lịch sử và trả về thông tin track
 */
public class PlayTrackResponse {
    @SerializedName("code")
    public int code;

    @SerializedName("message")
    public String message;

    @SerializedName("data")
    public TrackData data;

    public static class TrackData {
        @SerializedName("id")
        public String id;

        @SerializedName("title")
        public String title;

        @SerializedName("artist")
        public String artist;

        @SerializedName("artist_id")
        public String artistId;

        @SerializedName("album")
        public String album;

        @SerializedName("duration")
        public int duration; // seconds (not ms)

        @SerializedName("rank")
        public int rank;

        @SerializedName("preview_url")
        public String previewUrl;

        @SerializedName("cover_url")
        public String coverUrl;

        @SerializedName("cover_xl")
        public String coverXl;

        @SerializedName("link")
        public String link;

        @SerializedName("release_date")
        public String releaseDate;
        
        // For backward compatibility
        public String name;
    }
}
