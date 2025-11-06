package com.example.musicplayer.chatbot;

public class ChatMessage {
    private String role;
    private String content;
    private boolean isTyping;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.isTyping = false;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isTyping() {
        return isTyping;
    }

    public void setTyping(boolean typing) {
        isTyping = typing;
    }
}