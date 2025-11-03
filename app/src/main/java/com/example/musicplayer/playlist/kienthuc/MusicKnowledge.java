package com.example.musicplayer.playlist.kienthuc;

public class MusicKnowledge {
    private String id;
    private String title;
    private String content;
    private String category;
    private String date;
    private int views;

    public MusicKnowledge(String id, String title, String content, String category, String date, int views) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.date = date;
        this.views = views;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }

    public int getViews() {
        return views;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setViews(int views) {
        this.views = views;
    }
}