package com.example.musicplayer.music;

public class Song {
    public String id; // Deezer Track ID
    public String title; // Tên bài hát
    public String artist; // Tên ca sĩ
    public String cover; // URL ảnh bìa
    public String audio; // URL nhạc preview
    public String lyrics; // Lời bài hát
    public String albumName; // Tên album
    public int durationMs; // Độ dài bài hát (ms)
    public int popularity; // Độ phổ biến
    public String deezerUrl; // Link Deezer
    public String preview;

    // Constructor đầy đủ (10 tham số)
    public Song(String id, String title, String artist, String cover, String audio, String lyrics, String albumName,
                int durationMs, int popularity, String deezerUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.cover = cover;
        this.audio = audio;
        this.lyrics = lyrics;
        this.albumName = albumName;
        this.durationMs = durationMs;
        this.popularity = popularity;
        this.deezerUrl = deezerUrl;
    }

    public Song(String id, String title, String artist, String cover, String audio, int durationMs) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.cover = cover;
        this.audio = audio; // 'preview' từ PlayerActivity sẽ được gán vào 'audio'
        this.durationMs = durationMs;
        this.lyrics = "";
        this.albumName = "";
        this.popularity = 0;
        this.deezerUrl = "";
    }
}