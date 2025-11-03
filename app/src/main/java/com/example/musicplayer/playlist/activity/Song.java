package com.example.musicplayer.playlist.activity;

// Lớp này đã được tách ra thành file riêng và đổi thành public
public class Song {
    String id;
    String title;
    String artist;
    String cover;
    String preview;
    int durationMs;

    public Song(String id, String title, String artist, String cover, String preview, int durationMs) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.cover = cover;
        this.preview = preview;
        this.durationMs = durationMs;
    }
}