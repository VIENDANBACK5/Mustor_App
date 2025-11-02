package com.example.musicplayer.login;

import com.google.gson.annotations.SerializedName;

public class AuthTokenResponse {

    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("token_type")
    public String tokenType;

    @SerializedName("user")
    public User user;

    public static class User {
        @SerializedName("id")
        public int id;

        @SerializedName("username")
        public String username;

        @SerializedName("email")
        public String email;

        @SerializedName("full_name")
        public String fullName;
    }
}
