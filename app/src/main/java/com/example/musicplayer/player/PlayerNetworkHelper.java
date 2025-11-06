package com.example.musicplayer.player;

import android.util.Log;

import com.example.musicplayer.api.DeezerApi;
import com.example.musicplayer.login.LoginActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PlayerNetworkHelper {

    private static final String TAG = "PlayerNetworkHelper";

    // Giao diện callback cho việc tải lời bài hát bất đồng bộ
    public interface LyricsCallback {
        void onLyricsFetched(String lyrics);
        void onError(String error);
    }

    public static DeezerApi setupRetrofit(LoginActivity.SessionManager sessionManager, Runnable onAuthError) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = sessionManager.getValidAccessToken();
                    okhttp3.Request.Builder builder = chain.request().newBuilder();

                    if (token != null && !token.isEmpty()) {
                        builder.header("Authorization", "Bearer " + token);
                    }

                    okhttp3.Request request = builder.build();
                    okhttp3.Response response = chain.proceed(request);

                    if (response.code() == 401) {
                        // Chạy trên UI thread để xử lý lỗi
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(onAuthError);
                    }

                    return response;
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.30.28:5030/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(DeezerApi.class);
    }

    public static void fetchLyrics(String artist, String title, LyricsCallback callback) {
        new Thread(() -> {
            try {
                String apiUrl = "https://lyrics.lewdhutao.tech/?title=" +
                        title.replace(" ", "%20") + "&artist=" + artist.replace(" ", "%20");

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    String lyrics = json.optString("lyrics", "Không tìm thấy lời bài hát");

                    // Trả kết quả về main thread
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onLyricsFetched(lyrics)
                    );
                } else {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            {
                                try {
                                    callback.onError("Không tải được lời bài hát (Code: " + conn.getResponseCode() + ")");
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi lấy lời bài hát: " + e.getMessage());
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        callback.onError("Không tải được lời bài hát")
                );
            }
        }).start();
    }
}