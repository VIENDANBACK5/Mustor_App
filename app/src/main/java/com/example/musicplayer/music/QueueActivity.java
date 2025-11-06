package com.example.musicplayer.music;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;

import java.util.ArrayList;

public class QueueActivity extends AppCompatActivity
        implements MusicService.MusicServiceCallback {

    private RecyclerView recyclerView;
    private TextView tvEmptyQueue, tvQueueCount;
    private QueueAdapter adapter;
    private ArrayList<Song> queueList = new ArrayList<>();

    private MusicService musicService;
    private boolean serviceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setCallback(QueueActivity.this);
            serviceBound = true;
            loadQueue();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            musicService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        setupViews();
        setupRecyclerView();
        bindMusicService();
    }

    private void setupViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClearQueue).setOnClickListener(v -> clearQueue());

        tvEmptyQueue = findViewById(R.id.tvEmptyQueue);
        tvQueueCount = findViewById(R.id.tvQueueCount);
        recyclerView = findViewById(R.id.recyclerViewQueue);
    }

    private void setupRecyclerView() {
        adapter = new QueueAdapter(this, queueList);
        adapter.setOnItemClickListener(position -> playFromQueue(position));
        adapter.setOnDeleteListener(position -> removeFromQueue(position));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Drag & Drop
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();

                if (musicService != null) {
                    musicService.moveQueueItem(fromPos, toPos);
                }

                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Không sử dụng swipe
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void bindMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void loadQueue() {
        if (musicService != null) {
            queueList.clear();
            queueList.addAll(musicService.getQueue());
            updateUI();
        }
    }

    private void updateUI() {
        if (queueList.isEmpty()) {
            tvEmptyQueue.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            tvQueueCount.setText("Queue (0)");
        } else {
            tvEmptyQueue.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            tvQueueCount.setText("Queue (" + queueList.size() + ")");
        }
        adapter.notifyDataSetChanged();
    }

    private void playFromQueue(int position) {
        // Phát các bài từ vị trí này trở đi
        if (musicService != null && position >= 0 && position < queueList.size()) {
            // Xóa các bài trước vị trí click
            for (int i = 0; i < position; i++) {
                musicService.removeFromQueue(0);
            }
            // Phát bài tiếp theo (sẽ lấy từ queue)
            musicService.playNext();
            Toast.makeText(this, "Playing from queue", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeFromQueue(int position) {
        if (musicService != null && position >= 0 && position < queueList.size()) {
            musicService.removeFromQueue(position);
        }
    }

    private void clearQueue() {
        if (musicService != null) {
            musicService.clearQueue();
            Toast.makeText(this, "Queue cleared", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onQueueUpdated() {
        runOnUiThread(this::loadQueue);
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {}

    @Override
    public void onSongChanged(Song song, int position) {}

    @Override
    public void onProgressChanged(int currentPosition, int duration) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}