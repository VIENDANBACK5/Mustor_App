
package com.example.musicplayer.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DeezerSearchResponse {
    @SerializedName("code")
    public int code;
    
    @SerializedName("data")
    public Data data;
    
    // Lớp lồng tĩnh (Data transfer object - DTO)
    public static class Data {
        @SerializedName("tracks")
        public List<DeezerTrack> tracks;

        @SerializedName("total")
        public int total;
    }
}
