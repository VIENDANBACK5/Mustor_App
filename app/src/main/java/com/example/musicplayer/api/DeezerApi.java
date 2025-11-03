package com.example.musicplayer.api;

import com.example.musicplayer.login.LoginActivity;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DeezerApi {

    // ==================== AUTH ENDPOINTS ====================

    @POST("/api/auth/register")
    Call<LoginActivity.AuthTokenResponse> register(@Body LoginActivity.RegisterRequest request);

    @POST("/api/auth/login")
    Call<LoginActivity.AuthTokenResponse> login(@Body LoginActivity.LoginRequest request);

    @GET("/api/auth/me")
    Call<LoginActivity.AuthTokenResponse.User> getMe();

    // ==================== PROFILE ENDPOINTS ====================
    
    @PUT("/api/auth/me")
    Call<Void> updateProfile(@Body LoginActivity.UpdateProfileRequest request);    // ==================== DEEZER MUSIC ENDPOINTS ====================

    // Play track và tự động save vào history (auto-save)
    @POST("/api/deezer/tracks/{track_id}/play")
    Call<PlayTrackResponse> playTrack(
            @Path("track_id") String trackId,
            @Query("play_duration_seconds") int playDurationSeconds);

    // Get track details
    @GET("/api/deezer/tracks/{track_id}")
    Call<DeezerTrack> getTrackDetails(@Path("track_id") String trackId);

    // Search tracks
    @GET("/api/deezer/search/tracks")
    Call<DeezerSearchResponse> searchTracks(
            @Query("q") String query,  // ✅ Fixed: uses "q" as per documentation
            @Query("limit") int limit,
            @Query("offset") int offset);

    // Get Deezer chart (top tracks)
    @GET("/api/deezer/chart")
    Call<JsonObject> getChart(@Query("limit") int limit);

    // Get artist info
    @GET("/api/deezer/artists/{artist_id}")
    Call<JsonObject> getArtist(@Path("artist_id") String artistId);

    // ==================== HISTORY ENDPOINTS ====================

    // Manual history save (fallback method)
    @POST("/api/history")
    Call<Void> addHistoryRecord(@Body HistoryRecordRequest request);

    // Get listening history
    @GET("/api/history")
    Call<HistoryResponse> getHistory(
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("track_id") String trackId // Optional filter by track_id
    );

    // Get history statistics
    @GET("/api/history/stats")
    Call<HistoryStatsResponse> getHistoryStats();
}