package com.example.musicplayer.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;
import com.example.musicplayer.api.DeezerApi;
import com.example.musicplayer.api.HistoryStatsResponse;
import com.example.musicplayer.login.LoginActivity;
import com.example.musicplayer.login.UpdateUserRequest;
import java.text.NumberFormat;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileActivity extends AppCompatActivity {

    private LoginActivity.SessionManager sessionManager;
    private LoginActivity.AuthTokenResponse.User currentUser;

    private DeezerApi deezerApi;

    private ImageButton btnBackProfile;

    private TextView tvUserName, tvUserEmail;
    private TextView tvSongsPlayedCount;

    private LinearLayout btnEditProfile, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize session manager before any logout or auth actions
        sessionManager = new LoginActivity.SessionManager(this);

        // Setup Retrofit / API client with auth interceptor
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = sessionManager.getAccessToken();
                    Request.Builder builder = chain.request().newBuilder();
                    if (token != null) {
                        builder.addHeader("Authorization", "Bearer " + token);
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

        btnBackProfile = findViewById(R.id.btnBackProfile);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvSongsPlayedCount = findViewById(R.id.tvSongsPlayedCount);

        // TODO: Load user profile data
        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            tvTitle.setText("Hồ sơ cá nhân");
        }

        loadUserProfile();
        loadHistoryStats();

        setupBackButton();
        setupEditButton();
        setupLogoutButton();
    }

    private void loadUserProfile() {
        deezerApi.getMe().enqueue(new Callback<LoginActivity.AuthTokenResponse.User>() {
            @Override
            public void onResponse(@NonNull Call<LoginActivity.AuthTokenResponse.User> call,
                                   @NonNull Response<LoginActivity.AuthTokenResponse.User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    updateUser();
                } else {
                    handleLogout();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginActivity.AuthTokenResponse.User> call, @NonNull Throwable t) {
                Toast.makeText(ProfileActivity.this, "Could not load user profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadHistoryStats() {
        // Fetch history statistics (total plays, unique tracks, etc.)
        deezerApi.getHistoryStats().enqueue(new Callback<HistoryStatsResponse>() {
            @Override
            public void onResponse(@NonNull Call<HistoryStatsResponse> call, @NonNull Response<HistoryStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    int totalPlays = response.body().data.totalPlays;
                    // Format with locale-aware grouping
                    String formatted = NumberFormat.getIntegerInstance().format(totalPlays);
                    if (tvSongsPlayedCount != null) {
                        tvSongsPlayedCount.setText(formatted);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<HistoryStatsResponse> call, @NonNull Throwable t) {
                Toast.makeText(ProfileActivity.this, "Could not load history total", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLogoutButton() {
        btnLogout.setOnClickListener(v -> handleLogout());
    }

    private void setupBackButton() {
        btnBackProfile.setOnClickListener(v -> finish());
    }
    private void setupEditButton(){
        btnEditProfile.setOnClickListener(v -> {
            if (currentUser != null) {
                Intent intent = new Intent(this, EditProfileActivity.class);
                intent.putExtra("USER_NAME", currentUser.fullName);
                intent.putExtra("USER_EMAIL", currentUser.email);
                startActivity(intent);
            } else {
                Toast.makeText(this, "User data not loaded yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUser(){
        if (currentUser != null) {
            tvUserName.setText(currentUser.fullName);
            tvUserEmail.setText(currentUser.email);
        }
    }

    private void handleLogout(){
        sessionManager.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
