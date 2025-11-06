package com.example.musicplayer.history;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.example.musicplayer.api.HistoryResponse;
import com.example.musicplayer.api.DeezerApi;
import com.example.musicplayer.login.LoginActivity;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private HistoryAdapter adapter;
    private List<HistoryResponse.HistoryItem> historyList = new ArrayList<>();
    private DeezerApi deezerApi;
    private LoginActivity.SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        sessionManager = new LoginActivity.SessionManager(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupRetrofit();
        setupRecyclerView();
        fetchHistory();

    // Thêm listener nhấn giữ để làm mới
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setOnLongClickListener(v -> {
            android.util.Log.d("HistoryActivity", "Đã kích hoạt làm mới thủ công");
            fetchHistory();
            return true;
        });
    }

    private void setupRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = sessionManager.getAccessToken();
                    android.util.Log.d("HistoryActivity", "Token from session: "
                            + (token != null ? "Found (" + token.length() + " chars)" : "NULL"));

                    okhttp3.Request.Builder builder = chain.request().newBuilder();
                    if (token != null) {
                        builder.header("Authorization", "Bearer " + token);
                        android.util.Log.d("HistoryActivity", "Added Authorization header");
                    } else {
                        android.util.Log.w("HistoryActivity", "No token found, request will be sent without auth");
                    }
                    return chain.proceed(builder.build());
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.30.28:5030/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        deezerApi = retrofit.create(DeezerApi.class);
    }

    private void setupRecyclerView() {
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyList);
        recyclerViewHistory.setAdapter(adapter);
    }

    private void fetchHistory() {
        // Pass null for trackId to get all history (not filtered)
        android.util.Log.d("HistoryActivity", "Fetching history from API...");
        deezerApi.getHistory(20, 0, null).enqueue(new Callback<HistoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<HistoryResponse> call, @NonNull Response<HistoryResponse> response) {
                android.util.Log.d("HistoryActivity", "Response code: " + response.code());

            // Ghi log phản hồi thô
                try {
                    if (response.body() != null) {
                android.util.Log.d("HistoryActivity",
                    "Phản hồi thô: " + new com.google.gson.Gson().toJson(response.body()));
                    }
                } catch (Exception e) {
                android.util.Log.e("HistoryActivity", "Ghi log phản hồi thất bại", e);
                }

                if (response.isSuccessful() && response.body() != null) {
                    HistoryResponse historyResponse = response.body();
                android.util.Log.d("HistoryActivity", "Đã nhận body phản hồi");
                android.util.Log.d("HistoryActivity", "Danh sách lịch sử: "
                    + (historyResponse.history != null ? historyResponse.history.size() + " mục" : "null"));
                android.util.Log.d("HistoryActivity", "Tổng: " + historyResponse.total);

                    if (historyResponse.history != null && !historyResponse.history.isEmpty()) {
                        historyList.clear();
                        historyList.addAll(historyResponse.history);
                        adapter.notifyDataSetChanged();
                        android.util.Log.d("HistoryActivity",
                                "Updated RecyclerView with " + historyList.size() + " items");

                        // Log first item details
                        if (!historyList.isEmpty()) {
                            HistoryResponse.HistoryItem first = historyList.get(0);
                            android.util.Log.d("HistoryActivity",
                                    "First item: " + first.trackName + " by " + first.artistName);
                        }
                    } else {
                        android.util.Log.w("HistoryActivity", "History list is empty or null");
                        Toast.makeText(HistoryActivity.this, "Chưa có lịch sử phát nhạc", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    android.util.Log.e("HistoryActivity", "Tải lịch sử thất bại: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                        android.util.Log.e("HistoryActivity", "Error body: " + errorBody);
                    } catch (Exception e) {
                        android.util.Log.e("HistoryActivity", "Đọc body lỗi thất bại", e);
                    }
                    Toast.makeText(HistoryActivity.this, "Tải lịch sử thất bại: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<HistoryResponse> call, @NonNull Throwable t) {
                android.util.Log.e("HistoryActivity", "Gọi API lịch sử thất bại: " + t.getMessage(), t);
                Toast.makeText(HistoryActivity.this, "Gọi API lịch sử thất bại: " + t.getMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }
}
