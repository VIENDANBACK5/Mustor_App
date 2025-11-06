package com.example.musicplayer.profile;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.R;

public class AboutActivity extends AppCompatActivity {

    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        btnBack = findViewById(R.id.btnBack);

        TextView tvVersion = findViewById(R.id.tvVersion);
        TextView tvDescription = findViewById(R.id.tvDescription);

        if (tvVersion != null) {
            tvVersion.setText("Phiên bản 0.0.1");
        }

        if (tvDescription != null) {
            tvDescription.setText("Muse - Ứng dụng nghe nhạc trực tuyến\n\n" +
                    "Tính năng:\n" +
                    "• Nghe nhạc trực tuyến\n" +
                    "• Yêu thích bài hát\n" +
                    "• Lịch sử phát\n" +
                    "• Và nhiều tính năng khác...");
        }

        setupBack();
    }

    private void setupBack(){
        btnBack.setOnClickListener(v -> finish());
    }
}
