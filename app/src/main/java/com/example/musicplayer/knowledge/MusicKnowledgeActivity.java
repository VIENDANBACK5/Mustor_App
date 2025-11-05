package com.example.musicplayer.knowledge;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MusicKnowledgeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MusicKnowledgeAdapter adapter;
    private ArrayList<MusicKnowledge> knowledgeList;
    private FloatingActionButton fabAddKnowledge;

    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_knowledge);

        initViews();
        setupRecyclerView();
        loadKnowledgeData();
        setupClickListeners();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        btnBack = findViewById(R.id.btnBack);
        fabAddKnowledge = findViewById(R.id.fabAddKnowledge);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        knowledgeList = new ArrayList<>();
        adapter = new MusicKnowledgeAdapter(this, knowledgeList);
        recyclerView.setAdapter(adapter);
    }

    private void loadKnowledgeData() {
        // Dữ liệu mẫu - Có thể thay thế bằng dữ liệu từ database
        knowledgeList.add(new MusicKnowledge(
                "1",
                "Lịch sử Nhạc Jazz",
                "Jazz ra đời vào đầu thế kỷ 20 tại New Orleans, kết hợp giữa nhạc blues, ragtime và âm nhạc châu Âu...",
                "Lịch sử",
                "15/10/2024",
                245
        ));

        knowledgeList.add(new MusicKnowledge(
                "2",
                "Kỹ thuật hát Vibrato",
                "Vibrato là kỹ thuật dao động nhẹ của giọng hát, tạo ra âm thanh ấm áp và phong phú hơn...",
                "Kỹ thuật",
                "20/10/2024",
                189
        ));

        knowledgeList.add(new MusicKnowledge(
                "3",
                "Các thang âm cơ bản",
                "Thang âm trưởng (Major Scale) và thang âm thứ (Minor Scale) là nền tảng của lý thuyết âm nhạc...",
                "Lý thuyết",
                "22/10/2024",
                312
        ));

        knowledgeList.add(new MusicKnowledge(
                "4",
                "Âm nhạc Cổ điển Việt Nam",
                "Nhạc cổ truyền Việt Nam có lịch sử hàng nghìn năm với các dân tộc khác nhau mang đến sự đa dạng...",
                "Văn hóa",
                "25/10/2024",
                156
        ));

        knowledgeList.add(new MusicKnowledge(
                "5",
                "Mixing và Mastering",
                "Mixing là quá trình cân bằng các track âm thanh, trong khi Mastering là bước hoàn thiện cuối cùng...",
                "Sản xuất",
                "28/10/2024",
                278
        ));

        knowledgeList.add(new MusicKnowledge(
                "6",
                "Phong cách K-Pop",
                "K-Pop không chỉ là âm nhạc mà còn là sự kết hợp nghệ thuật giữa múa, thời trang và công nghệ...",
                "Thể loại",
                "01/11/2024",
                421
        ));

        knowledgeList.add(new MusicKnowledge(
                "7",
                "Cách đọc Sheet Music",
                "Học cách đọc nốt nhạc trên khuông nhạc là bước đầu tiên để trở thành nhạc sĩ chuyên nghiệp...",
                "Lý thuyết",
                "02/11/2024",
                203
        ));

        adapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        fabAddKnowledge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MusicKnowledgeActivity.this,
                        "Tính năng thêm kiến thức đang phát triển!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}