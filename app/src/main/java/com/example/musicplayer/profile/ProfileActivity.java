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
import com.example.musicplayer.api.HistoryResponse;
import com.example.musicplayer.api.HistoryStatsResponse;
import com.example.musicplayer.favorites.FavoritesManager;
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

    private FavoritesManager favoritesManager;

    private DeezerApi deezerApi;

    private ImageButton btnBackProfile;

    private TextView tvUserName, tvUserEmail, tvSongsPlayedCount, tvFavoritesCount;

    private LinearLayout btnEditProfile, btnChangePassword, btnSetting,btnAbout,  btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

    // Khởi tạo SessionManager trước khi xử lý đăng xuất hoặc xác thực
        sessionManager = new LoginActivity.SessionManager(this);
        favoritesManager = FavoritesManager.getInstance(this);

    // Thiết lập Retrofit / client API với Interceptor thêm header xác thực
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
        btnSetting = findViewById(R.id.btnSetting);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnAbout = findViewById(R.id.btnAbout);
        btnLogout = findViewById(R.id.btnLogout);


        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvSongsPlayedCount = findViewById(R.id.tvSongsPlayedCount);
        tvFavoritesCount = findViewById(R.id.tvFavoritesCount);

    // TODO: Tải dữ liệu hồ sơ người dùng
        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            tvTitle.setText("Hồ sơ cá nhân");
        }

        tvFavoritesCount.setText(String.valueOf(favoritesManager.getFavoritesCount()));

        loadUserProfile();
        loadHistoryStats();

        setupBackButton();
        setupEditButton();
        setupChangeButton();
        setupAboutButton();
        setupSettingButton();
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
                Toast.makeText(ProfileActivity.this, "Không thể tải hồ sơ người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadHistoryStats() {
        // Lấy tổng số bài đã nghe từ /api/history (field "total")
        deezerApi.getHistory(1, 0, null).enqueue(new Callback<HistoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<HistoryResponse> call, @NonNull Response<HistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int total = response.body().total; // Tổng số bản ghi history
                    // Format với dấu phẩy: 1,234
                    String formatted = NumberFormat.getIntegerInstance().format(total);
                    if (tvSongsPlayedCount != null) {
                        tvSongsPlayedCount.setText(formatted);
                    }
                    android.util.Log.d("ProfileActivity", "Tổng lịch sử đã tải: " + total);
                } else {
                    android.util.Log.e("ProfileActivity", "History API thất bại: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<HistoryResponse> call, @NonNull Throwable t) {
                android.util.Log.e("ProfileActivity", "Lỗi History API: " + t.getMessage());
                Toast.makeText(ProfileActivity.this, "Không thể tải lịch sử nghe nhạc", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLogoutButton() {
        btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(ProfileActivity.this)
                    .setTitle("Xác nhận đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        handleLogout();
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void setupBackButton() {
        btnBackProfile.setOnClickListener(v -> finish());
    }

    private void setupSettingButton(){
        btnSetting.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            Toast.makeText(this, "Tính năng chưa phát triển", Toast.LENGTH_SHORT).show();
            startActivity(intent);
        });
    }

    private void setupChangeButton(){
        btnChangePassword.setOnClickListener(v ->{
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);
        });
    }

    private void setupAboutButton(){
        btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        });
    }

    private void setupEditButton(){
        btnEditProfile.setOnClickListener(v -> {
            if (currentUser != null) {
                Intent intent = new Intent(this, EditProfileActivity.class);
                intent.putExtra("USER_NAME", currentUser.fullName);
                intent.putExtra("USER_EMAIL", currentUser.email);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Dữ liệu người dùng chưa được tải", Toast.LENGTH_SHORT).show();
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
