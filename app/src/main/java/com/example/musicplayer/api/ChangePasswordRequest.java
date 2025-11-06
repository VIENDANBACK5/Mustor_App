package com.example.musicplayer.api;

import com.google.gson.annotations.SerializedName;

public class ChangePasswordRequest {
    @SerializedName("old_password")
    public String oldPassword;
    
    @SerializedName("new_password")
    public String newPassword;
    
    public ChangePasswordRequest(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
