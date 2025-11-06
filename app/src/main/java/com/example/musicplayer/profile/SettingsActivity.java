package com.example.musicplayer.profile;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.R;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        btnBack = findViewById(R.id.btnBack);

        setupBack();

    // Ví dụ: Xử lý click chọn chất lượng streaming
        findViewById(R.id.btnStreamingQuality).setOnClickListener(v -> {
            Toast.makeText(this, "Mở cài đặt chất lượng stream", Toast.LENGTH_SHORT).show();
        });


        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            Toast.makeText(this, "Đăng xuất...", Toast.LENGTH_SHORT).show();

        });
    }

    private void setupBack(){
        btnBack.setOnClickListener(v -> finish());
    }
}

