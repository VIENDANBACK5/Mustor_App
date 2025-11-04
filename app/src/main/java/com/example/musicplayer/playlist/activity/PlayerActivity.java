package com.example.musicplayer.playlist.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.MusicService;
import com.example.musicplayer.R;
// Đảm bảo import đúng lớp Song (com.example.musicplayer.Song)
import com.example.musicplayer.Song;
import com.example.musicplayer.api.DeezerApi;
import com.example.musicplayer.login.LoginActivity;
import com.example.musicplayer.utils.HistoryManager;
import com.example.musicplayer.profile.FavoritesManager;

import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity
        implements FavoritesManager.FavoritesChangeListener {

    private static final String TAG = "PlayerActivity";
    private int startPositionMs = 0;

    // UI (Được quản lý bởi Helper)
    private PlayerUIHelper uiHelper;

    // Playback
    private MediaPlayer mediaPlayer; // Sẽ được khởi tạo trong onServiceConnected
    private boolean isPlaying = false;
    private boolean isPreparing = false; // Chống lỗi state -38
    private boolean isShuffle = false;
    private int repeatMode = 0;
    private Handler handler = new Handler();
    private Runnable updateSeekBar;
    private int lastPlaybackPosition = 0;

    // ⭐ SỬA LỖI NHẢY BÀI: Thêm cờ (flag) để theo dõi lần tải đầu tiên
    private boolean isFirstLoad = true;

    // Data
    private ArrayList<Song> playlist;
    private int currentSongIndex = 0;

    // Helpers & Managers
    private DeezerApi deezerApi;
    private LoginActivity.SessionManager sessionManager;
    private HistoryManager historyManager;
    private FavoritesManager favoritesManager;

    // History Tracking
    private long songStartTime = 0;
    private String currentPlayingTrackId = null;

    // MusicService
    private MusicService musicService;
    private boolean serviceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            Log.d(TAG, "✅ Connected to MusicService");

            // ⭐ SỬA LỖI ĐÈ NHẠC
            // 1. Tắt MiniPlayer ngay khi kết nối
            hideMiniPlayer();

            // 2. CHỈ KHỞI TẠO PLAYER CỦA ACTIVITY SAU KHI TẮT MINIPLAYER
            // (Dùng mediaPlayer == null để đảm bảo logic này chỉ chạy
            // khi Activity được khởi tạo lần đầu)
            if (mediaPlayer == null) {
                Log.d(TAG, "Service connected. Initializing local player...");
                // Tải dữ liệu và cài đặt
                loadPlaylistData();
                loadCurrentSong();
                setupMediaPlayer();
                setupControls();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            musicService = null;
            Log.d(TAG, "❌ Disconnected from MusicService");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 PlayerActivity onCreate() started");
        setContentView(R.layout.activity_player);

        // ⭐ BÀN GIAO THỜI GIAN (Hand-off)
        // Lấy vị trí bắt đầu từ Intent mà MainActivity gửi sang
        startPositionMs = getIntent().getIntExtra("start_position_ms", 0);
        if (startPositionMs > 0) {
            Log.d(TAG, "Received start position: " + startPositionMs + "ms");
        }
        // ⭐ KẾT THÚC BÀN GIAO

        // Khởi tạo Managers
        sessionManager = new LoginActivity.SessionManager(this);
        historyManager = new HistoryManager(this);
        favoritesManager = FavoritesManager.getInstance(this);
        favoritesManager.addListener(this);

        if (!sessionManager.isTokenValid()) {
            Log.e(TAG, "❌ Token is invalid or expired!");
            redirectToLogin();
            return;
        }

        // Khởi tạo Helpers
        uiHelper = new PlayerUIHelper(this);
        deezerApi = PlayerNetworkHelper.setupRetrofit(sessionManager, this::redirectToLogin);

        // Bắt đầu kết nối service
        bindMusicService();

        // (Lệnh hideMiniPlayer() đã có trong onServiceConnected)
        hideMiniPlayer();

        Log.d(TAG, "✅ PlayerActivity onCreate() completed (waiting for service)");
    }

    private void bindMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void redirectToLogin() {
        sessionManager.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadPlaylistData() {
        playlist = new ArrayList<>();
        ArrayList<String> titles = getIntent().getStringArrayListExtra("playlist_titles");
        ArrayList<String> artists = getIntent().getStringArrayListExtra("playlist_artists");
        ArrayList<String> covers = getIntent().getStringArrayListExtra("playlist_covers");
        ArrayList<String> previews = getIntent().getStringArrayListExtra("playlist_previews");
        ArrayList<String> ids = getIntent().getStringArrayListExtra("playlist_ids");
        ArrayList<Integer> durations = getIntent().getIntegerArrayListExtra("playlist_durations");
        currentSongIndex = getIntent().getIntExtra("current_index", 0);

        if (titles != null && ids != null) {
            for (int i = 0; i < titles.size(); i++) {
                // Tạo Song bằng constructor 6 tham số (previews.get(i) sẽ được gán cho trường 'audio')
                playlist.add(new Song(ids.get(i), titles.get(i), artists.get(i),
                        covers.get(i), previews.get(i), durations.get(i)));
            }
        }
        Log.d(TAG, "Playlist loaded: " + playlist.size() + " songs");
    }

    private void loadCurrentSong() {
        if (playlist == null || playlist.isEmpty() || currentSongIndex >= playlist.size()) {
            Toast.makeText(this, "Không có bài hát!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Song song = playlist.get(currentSongIndex);
        Log.d(TAG, "Loading: " + song.title);

        boolean isFavorite = favoritesManager.isFavorite(song.title, song.artist);

        // Ủy quyền cho UIHelper cập nhật UI
        uiHelper.loadSongUI(song, isFavorite, () -> {
            // Callback (nếu cần)
        });

        // Ủy quyền cho NetworkHelper tải lời
        if (song.artist != null && song.title != null) {
            PlayerNetworkHelper.fetchLyrics(song.artist, song.title, new PlayerNetworkHelper.LyricsCallback() {
                @Override
                public void onLyricsFetched(String lyrics) {
                    uiHelper.setLyrics(lyrics);
                }
                @Override
                public void onError(String error) {
                    uiHelper.setLyrics(error);
                }
            });
        }
    }

    private void updateLikeButtonState() {
        if (playlist == null || playlist.isEmpty() || currentSongIndex >= playlist.size()) return;
        Song song = playlist.get(currentSongIndex);
        boolean isFavorite = favoritesManager.isFavorite(song.title, song.artist);
        uiHelper.updateLikeButton(isFavorite);
    }

    private void setupMediaPlayer() {
        Song song = playlist.get(currentSongIndex);

        String audioUrl = song.audio;
        Log.d(TAG, "Setup MediaPlayer: " + audioUrl);

        // (Logic xử lý lỗi URL rỗng của bạn...)
        if (audioUrl == null || audioUrl.isEmpty()) {
            if (isFirstLoad) {
                Log.e(TAG, "Cannot play: No audio URL for the first song.");
                Toast.makeText(this, "Không thể phát: Bài hát không có URL nhạc.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Log.w(TAG, "Skipping song: No audio URL.");
                Toast.makeText(this, "Không có URL nhạc! Đang chuyển bài...", Toast.LENGTH_SHORT).show();
                handleSongCompletion();
            }
            return;
        }

        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
            } else {
                mediaPlayer = new MediaPlayer();
            }

            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            mediaPlayer.setDataSource(audioUrl);

            mediaPlayer.setOnPreparedListener(mp -> {
                isFirstLoad = false;
                isPreparing = false;
                Log.d(TAG, "✅ Ready to play!");
                int duration = mp.getDuration();

                uiHelper.updateTimers(-1, duration);
                uiHelper.updateSeekBar(-1, duration);

                // ⭐ BÀN GIAO THỜI GIAN (Hand-off)
                // Nếu có mốc thời gian được gửi đến, tua (seek) đến vị trí đó
                if (startPositionMs > 0) {
                    try {
                        mp.seekTo(startPositionMs);
                        Log.d(TAG, "Seeking to received position: " + startPositionMs + "ms");
                        // Cập nhật UI ngay lập tức
                        uiHelper.updateTimers(startPositionMs, duration);
                        uiHelper.updateSeekBar(startPositionMs, duration);
                    } catch (Exception e) {
                        Log.e(TAG, "Error seeking to start position: " + e.getMessage());
                    }
                    // Reset lại cờ sau khi seek, để nó không seek lại khi xoay màn hình
                    startPositionMs = 0;
                }
                // ⭐ KẾT THÚC BÀN GIAO

                mp.start();
                isPlaying = true;
                uiHelper.updatePlayPauseButton(true);

                songStartTime = System.currentTimeMillis();
                currentPlayingTrackId = song.id;
                Log.d(TAG, "⏱️ Song playback started: " + song.title);

                saveHistoryImmediately(song, duration);
                startSeekBarUpdater();
                uiHelper.startDiscAnimation();

                Toast.makeText(this, "♫ " + song.title, Toast.LENGTH_SHORT).show();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPreparing = false;
                Log.e(TAG, "❌ Error: " + what);
                Toast.makeText(this, "Lỗi phát nhạc!", Toast.LENGTH_LONG).show();
                return true;
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Song completed");
                songStartTime = 0;
                currentPlayingTrackId = null;
                handleSongCompletion();
            });

            isPreparing = true;
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            isPreparing = false;
            Log.e(TAG, "Exception: " + e.getMessage());
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveHistoryImmediately(Song song, int songDurationMs) {
        if (song == null || song.id == null || song.id.isEmpty()) {
            Log.w(TAG, "⚠️ Cannot save history: Invalid song");
            return;
        }
        if (deezerApi == null || historyManager == null) {
            Log.w(TAG, "⚠️ Cannot save history: API not initialized");
            return;
        }
        String validToken = sessionManager.getValidAccessToken();
        if (validToken == null) {
            Log.w(TAG, "⚠️ Cannot save history: No valid token");
            return;
        }

        final int playDurationSeconds = songDurationMs / 1000;
        Log.d(TAG, "📝 Saving history immediately: " + song.title + " (" + playDurationSeconds + "s)");

        historyManager.saveHistoryAutoSave(
                deezerApi,
                song.id,
                playDurationSeconds,
                new HistoryManager.HistorySaveCallback() {
                    @Override public void onSuccess() { Log.d(TAG, "✅ History saved"); }
                    @Override public void onError(int code, String message) { Log.e(TAG, "❌ History save failed"); }
                    @Override public void onSkipped(String reason) { Log.d(TAG, "⏭️ History skipped"); }
                }
        );
    }

    private void handleSongCompletion() {
        if (serviceBound && musicService != null) {
            ArrayList<Song> queue = musicService.getQueue();
            if (!queue.isEmpty()) {
                Log.d(TAG, "🎵 Playing next song from queue (" + queue.size() + " songs)");
                Song queueSong = queue.get(0);
                musicService.removeFromQueue(0);
                playQueueSong(queueSong);
                return;
            }
        }

        if (repeatMode == 2) {
            playCurrentSong();
        } else if (repeatMode == 1 || currentSongIndex < playlist.size() - 1) {
            playNext();
        } else {
            isPlaying = false;
            uiHelper.updatePlayPauseButton(false);
            uiHelper.stopDiscAnimation();
            uiHelper.updateSeekBar(0, -1);
            uiHelper.updateTimers(0, -1);
        }
    }

    private void playQueueSong(Song queueSong) {
        currentSongIndex = playlist.size();
        playlist.add(queueSong);

        uiHelper.imgCover.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            loadCurrentSong();
            setupMediaPlayer();
            uiHelper.imgCover.animate().alpha(1f).setDuration(300).start();
        }).start();

        Toast.makeText(this, "🎵 From queue: " + queueSong.title, Toast.LENGTH_SHORT).show();
    }

    private void setupControls() {
        uiHelper.btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer == null || isPreparing) return;
            try {
                if (isPlaying) {
                    mediaPlayer.pause();
                    uiHelper.stopDiscAnimation();
                } else {
                    mediaPlayer.start();
                    uiHelper.startDiscAnimation();
                }
                isPlaying = !isPlaying;
                uiHelper.updatePlayPauseButton(isPlaying);
                // Hiệu ứng click
                v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                        .start();
            } catch (Exception e) {
                Log.e(TAG, "Play/Pause Error: " + e.getMessage());
            }
        });

        uiHelper.btnNext.setOnClickListener(v -> {
            uiHelper.animateButton(v);
            playNext();
        });

        uiHelper.btnPrevious.setOnClickListener(v -> {
            uiHelper.animateButton(v);
            playPrevious();
        });

        uiHelper.btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            uiHelper.animateButton(v);
            uiHelper.updateShuffleButton(isShuffle);
            Toast.makeText(this, isShuffle ? "🔀 Phát ngẫu nhiên" : "▶ Phát tuần tự", Toast.LENGTH_SHORT).show();
        });

        uiHelper.btnRepeat.setOnClickListener(v -> {
            repeatMode = (repeatMode + 1) % 3;
            uiHelper.animateButton(v);
            updateRepeatButtonVisual();
        });

        uiHelper.btnLike.setOnClickListener(v -> {
            uiHelper.animateButton(v);
            if (playlist == null || playlist.isEmpty()) return;
            Song song = playlist.get(currentSongIndex);
            boolean isFavorite = favoritesManager.isFavorite(song.title, song.artist);

            if (isFavorite) {
                if (favoritesManager.removeFavorite(song.title, song.artist)) {
                    uiHelper.updateLikeButton(false);
                    Toast.makeText(this, "🤍 Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (favoritesManager.addFavorite(song.title, song.artist, song.cover, song.audio)) {
                    uiHelper.updateLikeButton(true);
                    Toast.makeText(this, "❤️ Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                }
            }
        });

        uiHelper.btnDownload.setOnClickListener(v -> {
            uiHelper.animateButton(v);
            Toast.makeText(this, "⬇️ Tính năng tải xuống đang phát triển", Toast.LENGTH_SHORT).show();
        });

        uiHelper.btnBack.setOnClickListener(v -> finish());

        uiHelper.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && !isPreparing) {
                    try {
                        mediaPlayer.seekTo(progress);
                        uiHelper.updateTimers(progress, -1);
                    } catch (Exception e) {
                        Log.e(TAG, "Seek error: " + e.getMessage());
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void playNext() {
        if (playlist.size() <= 1) {
            Toast.makeText(this, "Không có bài tiếp theo", Toast.LENGTH_SHORT).show();
            return;
        }

        songStartTime = 0;
        currentPlayingTrackId = null;

        if (isShuffle) {
            int randomIndex;
            do {
                randomIndex = (int) (Math.random() * playlist.size());
            } while (randomIndex == currentSongIndex && playlist.size() > 1);
            currentSongIndex = randomIndex;
        } else {
            currentSongIndex = (currentSongIndex + 1) % playlist.size();
        }

        uiHelper.imgCover.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            loadCurrentSong();
            setupMediaPlayer();
            uiHelper.imgCover.animate().alpha(1f).setDuration(300).start();
        }).start();
    }

    private void playPrevious() {
        if (playlist.size() <= 1) {
            Toast.makeText(this, "Không có bài trước", Toast.LENGTH_SHORT).show();
            return;
        }

        songStartTime = 0;
        currentPlayingTrackId = null;

        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 3000 && !isPreparing) {
            mediaPlayer.seekTo(0);
            return;
        }

        currentSongIndex = (currentSongIndex - 1 + playlist.size()) % playlist.size();

        uiHelper.imgCover.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            loadCurrentSong();
            setupMediaPlayer();
            uiHelper.imgCover.animate().alpha(1f).setDuration(300).start();
        }).start();
    }

    private void playCurrentSong() {
        loadCurrentSong();
        setupMediaPlayer();
    }

    private void updateRepeatButtonVisual() {
        uiHelper.updateRepeatButton(repeatMode);
        switch (repeatMode) {
            case 0: Toast.makeText(this, "🔁 Tắt lặp lại", Toast.LENGTH_SHORT).show(); break;
            case 1: Toast.makeText(this, "🔁 Lặp lại tất cả", Toast.LENGTH_SHORT).show(); break;
            case 2: Toast.makeText(this, "🔂 Lặp lại một bài", Toast.LENGTH_SHORT).show(); break;
        }
    }

    private void startSeekBarUpdater() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying && !isPreparing) {
                    try {
                        int currentPos = mediaPlayer.getCurrentPosition();
                        uiHelper.updateSeekBar(currentPos, -1);
                        uiHelper.updateTimers(currentPos, -1);
                        handler.postDelayed(this, 500);
                    } catch (Exception e) {
                        Log.e(TAG, "Update error: " + e.getMessage());
                    }
                }
            }
        };
        handler.post(updateSeekBar);
    }

    @Override
    public void onFavoritesChanged() {
        runOnUiThread(() -> {
            Log.d(TAG, "📝 Favorites changed, updating UI...");
            updateLikeButtonState();
        });
    }

    // Logic Vòng đời (Lifecycle)

    @Override
    protected void onResume() {
        super.onResume();
        // Cập nhật nút like nếu mediaPlayer đã được khởi tạo
        if (mediaPlayer != null) {
            updateLikeButtonState();
        }
        // Tắt MiniPlayer khi quay lại Activity (lúc này service đã bind)
        hideMiniPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // KHI TẠM DỪNG ACTIVITY
        if (mediaPlayer != null) {
            // ⭐ SỬA LỖI: Lưu lại vị trí trước khi pause
            if (isPlaying) {
                try {
                    lastPlaybackPosition = mediaPlayer.getCurrentPosition(); // Lưu vị trí
                } catch (Exception e) {
                    lastPlaybackPosition = 0;
                }
                mediaPlayer.pause();
            }
            // ⭐ KẾT THÚC SỬA

            isPlaying = false;
            uiHelper.stopDiscAnimation();
            uiHelper.updatePlayPauseButton(false);
            Log.d(TAG, "⏸️ PlayerActivity paused at " + lastPlaybackPosition);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Hiện MiniPlayer khi thoát (cả Home và Back)
        if (mediaPlayer != null) {
            showMiniPlayer();
            Log.d(TAG, "👋 Leaving PlayerActivity, showing MiniPlayer");
        }

        if (songStartTime > 0) {
            Log.d(TAG, "💾 App stopped");
            songStartTime = 0;
            currentPlayingTrackId = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (favoritesManager != null) favoritesManager.removeListener(this);
        if (serviceBound) unbindService(serviceConnection);

        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Release error: " + e.getMessage());
            }
            mediaPlayer = null;
        }
        if (handler != null) handler.removeCallbacks(updateSeekBar);
        Log.d(TAG, "🛑 PlayerActivity destroyed");
    }

    /**
     * Ẩn MiniPlayer khi đang ở trong PlayerActivity
     */
    private void hideMiniPlayer() {
        if (serviceBound && musicService != null) {
            musicService.pause();
            Log.d(TAG, "🔇 MiniPlayer paused");
        }
    }

    /**
     * Hiện lại MiniPlayer khi thoát PlayerActivity
     */
    private void showMiniPlayer() {
        if (serviceBound && musicService != null && playlist != null && !playlist.isEmpty()) {
            Song currentSong = playlist.get(currentSongIndex);

            // ⭐ SỬA LỖI: Gọi hàm play mới với mốc thời gian
            Log.d(TAG, "🔊 Resuming MiniPlayer at " + lastPlaybackPosition + "ms");
            musicService.play(currentSong, lastPlaybackPosition);
            // ⭐ KẾT THÚC SỬA
        }
    }
}