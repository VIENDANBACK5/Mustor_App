
package com.example.musicplayer.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response wrapper for Deezer search API
 * Structure: { "code": 200, "data": { "tracks": [...], "total": 50 } }
 */
public class DeezerSearchResponse {
    @SerializedName("code")
    public int code;
    
    @SerializedName("data")
    public Data data;
    
    // ✅ Static nested class (best practice for DTOs)
    public static class Data {
        @SerializedName("tracks")
        public List<DeezerTrack> tracks;

        @SerializedName("total")
        public int total;
    }
}
