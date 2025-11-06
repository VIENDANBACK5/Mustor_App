package com.example.musicplayer.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.R;
import com.example.musicplayer.api.ChangePasswordRequest;
import com.example.musicplayer.api.DeezerApi;
import com.example.musicplayer.login.LoginActivity;
import com.google.android.material.textfield.TextInputEditText;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChangePasswordActivity extends AppCompatActivity {
    private static final String TAG = "ChangePasswordActivity";
    private static final String API_BASE_URL = "http://192.168.30.28:5030/";
    
    private ImageButton btnBack;
    private TextInputEditText etCurrentPassword, etNewPassword, etAgainNewPassword;
    private Button btnSaveChanges;
    
    private DeezerApi deezerApi;
    private LoginActivity.SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changepassword);

        initViews();
        setupRetrofit();
        setupListeners();
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etAgainNewPassword = findViewById(R.id.etAgainNewPassword);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        
        sessionManager = new LoginActivity.SessionManager(this);
    }
    
    private void setupRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    String token = sessionManager.getAccessToken();
                    
                    if (token != null && !token.isEmpty()) {
                        okhttp3.Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    }
                    return chain.proceed(original);
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        deezerApi = retrofit.create(DeezerApi.class);
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnSaveChanges.setOnClickListener(v -> {
            changePassword();
        });
    }
    
    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etAgainNewPassword.getText().toString().trim();
        
        // Validation
        if (TextUtils.isEmpty(currentPassword)) {
            etCurrentPassword.setError("Vui lòng nhập mật khẩu hiện tại");
            etCurrentPassword.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("Vui lòng nhập mật khẩu mới");
            etNewPassword.requestFocus();
            return;
        }
        
        if (newPassword.length() < 6) {
            etNewPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etNewPassword.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            etAgainNewPassword.setError("Vui lòng nhập lại mật khẩu mới");
            etAgainNewPassword.requestFocus();
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            etAgainNewPassword.setError("Mật khẩu không khớp");
            etAgainNewPassword.requestFocus();
            return;
        }
        
        if (currentPassword.equals(newPassword)) {
            etNewPassword.setError("Mật khẩu mới phải khác mật khẩu cũ");
            etNewPassword.requestFocus();
            return;
        }
        
        // Call API
        Log.d(TAG, "🔐 Changing password...");
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Đang xử lý...");
        
        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);
        
        deezerApi.changePassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnSaveChanges.setEnabled(true);
                btnSaveChanges.setText("Lưu thay đổi");
                
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Password changed successfully");
                    Toast.makeText(ChangePasswordActivity.this, 
                            "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                    
                    // Clear fields
                    etCurrentPassword.setText("");
                    etNewPassword.setText("");
                    etAgainNewPassword.setText("");
                    
                    // Close activity after 1 second
                    btnSaveChanges.postDelayed(() -> finish(), 1000);
                    
                } else {
                    Log.e(TAG, "❌ Password change failed: " + response.code());
                    String errorMsg = "Đổi mật khẩu thất bại";
                    
                    if (response.code() == 400) {
                        errorMsg = "Mật khẩu hiện tại không đúng";
                    } else if (response.code() == 401) {
                        errorMsg = "Phiên đăng nhập hết hạn";
                    }
                    
                    Toast.makeText(ChangePasswordActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSaveChanges.setEnabled(true);
                btnSaveChanges.setText("Lưu thay đổi");
                
                Log.e(TAG, "❌ Network error: " + t.getMessage());
                Toast.makeText(ChangePasswordActivity.this, 
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
