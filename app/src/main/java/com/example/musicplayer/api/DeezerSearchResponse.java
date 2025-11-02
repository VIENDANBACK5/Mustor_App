
package com.example.musicplayer.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DeezerSearchResponse {
    @SerializedName("tracks")
    public List<DeezerTrack> tracks;

    @SerializedName("total")
    public int total;
}
