package com.example.musicplayer.profile;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.R;

public class ChangePasswordActivity extends AppCompatActivity {
    private ImageButton btnBack;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changepassword);

        btnBack = findViewById(R.id.btnBack);

        setupBack();
    }
    
    private void setupBack(){
        btnBack.setOnClickListener(v -> finish());
    }

}
