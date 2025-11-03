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
import com.example.musicplayer.api.DeezerApi;
import com.example.musicplayer.login.LoginActivity;
import com.example.musicplayer.utils.HistoryManager;
import com.example.musicplayer.profile.FavoritesManager;

import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity
        implements FavoritesManager.FavoritesChangeListener {

    private static final String TAG = "PlayerActivity";

    // UI (Được quản lý bởi Helper)
    private PlayerUIHelper uiHelper;

    // Playback
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private int repeatMode = 0;
    private Handler handler = new Handler();
    private Runnable updateSeekBar;

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
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            musicService = null;
            Log.d(TAG, "❌ Disconnected from MusicService");
        }
    };

    // (Lớp Song đã được chuyển ra file riêng)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🚀 PlayerActivity onCreate() started");
        setContentView(R.layout.activity_player);

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

        // Tải dữ liệu và cài đặt
        loadPlaylistData();
        loadCurrentSong();
        setupMediaPlayer();
        setupControls();
        bindMusicService();

        Log.d(TAG, "✅ PlayerActivity onCreate() completed");
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

    // (setupRetrofit đã được chuyển sang PlayerNetworkHelper)

    // (initViews đã được chuyển sang PlayerUIHelper)

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
                playlist.add(new Song(ids.get(i), titles.get(i), artists.get(i),
                        covers.get(i), previews.get(i), durations.get(i)));
            }
        }
        Log.d(TAG, "Playlist loaded: " + playlist.size() + " songs");
    }

    private void loadCurrentSong() {
        if (playlist == null || playlist.isEmpty()) {
            Toast.makeText(this, "Không có bài hát!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Song song = playlist.get(currentSongIndex);
        Log.d(TAG, "Loading: " + song.title);

        boolean isFavorite = favoritesManager.isFavorite(song.title, song.artist);

        // Ủy quyền cho UIHelper cập nhật UI
        uiHelper.loadSongUI(song, isFavorite, () -> {
            // Callback này được gọi khi ảnh bìa đã được tải xong
            // (Hiện tại không cần làm gì thêm ở đây, nhưng có thể hữu ích)
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

    // (setBlurredBackground, extractDominantColor, animateColorChange, adjustAlpha đã được chuyển sang PlayerUIHelper)

    private void setupMediaPlayer() {
        Song song = playlist.get(currentSongIndex);
        String preview = song.preview;
        Log.d(TAG, "Setup MediaPlayer: " + preview);

        if (preview == null || preview.isEmpty()) {
            Toast.makeText(this, "Không có URL nhạc! Đang chuyển bài...", Toast.LENGTH_SHORT).show();
            handleSongCompletion();
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

            mediaPlayer.setDataSource(preview);

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "✅ Ready to play!");
                int duration = mp.getDuration();

                uiHelper.updateTimers(-1, duration);
                uiHelper.updateSeekBar(-1, duration);

                mp.start();
                isPlaying = true;
                uiHelper.updatePlayPauseButton(true);

                songStartTime = System.currentTimeMillis();
                currentPlayingTrackId = song.id;
                Log.d(TAG, "⏱️ Song playback started: " + song.title);

                // ✅ LƯU LỊCH SỬ NGAY KHI BẮT ĐẦU PHÁT (không cần đợi nghe hết)
                saveHistoryImmediately(song, duration);

                startSeekBarUpdater();
                uiHelper.startDiscAnimation();

                Toast.makeText(this, "♫ " + song.title, Toast.LENGTH_SHORT).show();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "❌ Error: " + what);
                Toast.makeText(this, "Lỗi phát nhạc!", Toast.LENGTH_LONG).show();
                return true;
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Song completed");
                // ℹ️ History đã được lưu ngay khi bắt đầu phát
                songStartTime = 0;
                currentPlayingTrackId = null;
                handleSongCompletion();
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 🎵 LƯU LỊCH SỬ NGAY KHI BẮT ĐẦU PHÁT BÀI HÁT
     * Gọi API POST /api/deezer/tracks/{id}/play với duration là thời lượng bài hát
     * Không cần đợi nghe hết, đánh dấu luôn khi ấn play
     */
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

        // Sử dụng thời lượng bài hát (duration) làm play_duration_seconds
        final int playDurationSeconds = songDurationMs / 1000;

        Log.d(TAG, "📝 Saving history immediately: " + song.title + " (" + playDurationSeconds + "s)");
        
        historyManager.saveHistoryAutoSave(
                deezerApi,
                song.id,
                playDurationSeconds,
                new HistoryManager.HistorySaveCallback() {
                    @Override 
                    public void onSuccess() { 
                        Log.d(TAG, "✅ History saved successfully on play start"); 
                    }
                    @Override 
                    public void onError(int code, String message) { 
                        Log.e(TAG, "❌ History save failed: " + code + " - " + message); 
                    }
                    @Override 
                    public void onSkipped(String reason) { 
                        Log.d(TAG, "⏭️ History skipped: " + reason); 
                    }
                }
        );
    }

    private void handleSongCompletion() {
        if (serviceBound && musicService != null) {
            ArrayList<com.example.musicplayer.Song> queue = musicService.getQueue();
            if (!queue.isEmpty()) {
                Log.d(TAG, "🎵 Playing next song from queue (" + queue.size() + " songs)");
                com.example.musicplayer.Song queueSong = queue.get(0);
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

    private void playQueueSong(com.example.musicplayer.Song queueSong) {
        Song song = new Song(
                queueSong.id,
                queueSong.title,
                queueSong.artist,
                queueSong.cover,
                queueSong.audio,
                queueSong.durationMs
        );

        currentSongIndex = playlist.size();
        playlist.add(song);

        uiHelper.imgCover.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            loadCurrentSong();
            setupMediaPlayer();
            uiHelper.imgCover.animate().alpha(1f).setDuration(300).start();
        }).start();

        Toast.makeText(this, "🎵 From queue: " + song.title, Toast.LENGTH_SHORT).show();
    }

    private void setupControls() {
        // Các listeners vẫn nằm trong Activity vì chúng xử lý logic/trạng thái
        uiHelper.btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer == null) return;
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
                // Hiệu ứng click nhỏ
                v.animate()
                        .scaleX(0.85f).scaleY(0.85f)
                        .setDuration(100)
                        .withEndAction(() ->
                                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        ).start();
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
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
                if (favoritesManager.addFavorite(song.title, song.artist, song.cover, song.preview)) {
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
                if (fromUser && mediaPlayer != null) {
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

    // (animateButton đã được chuyển sang PlayerUIHelper)

    private void playNext() {
        if (playlist.size() <= 1) {
            Toast.makeText(this, "Không có bài tiếp theo", Toast.LENGTH_SHORT).show();
            return;
        }

        // ℹ️ History sẽ được lưu tự động khi bài mới bắt đầu phát
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

        // ℹ️ History sẽ được lưu tự động khi bài mới bắt đầu phát
        songStartTime = 0;
        currentPlayingTrackId = null;

        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 3000) {
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

    // (fetchLyrics đã được chuyển sang PlayerNetworkHelper)

    private void startSeekBarUpdater() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
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

    // (formatTime, startDiscAnimation, stopDiscAnimation đã được chuyển sang PlayerUIHelper)

    @Override
    public void onFavoritesChanged() {
        runOnUiThread(() -> {
            Log.d(TAG, "📝 Favorites changed, updating UI...");
            updateLikeButtonState();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (favoritesManager != null) favoritesManager.removeListener(this);
        if (serviceBound) unbindService(serviceConnection);

        // ℹ️ History đã được lưu khi bắt đầu phát, không cần lưu lại khi destroy

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Release error: " + e.getMessage());
            }
            mediaPlayer = null;
        }
        if (handler != null) handler.removeCallbacks(updateSeekBar);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            uiHelper.stopDiscAnimation();
            uiHelper.updatePlayPauseButton(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLikeButtonState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ℹ️ History đã được lưu khi bắt đầu phát, không cần lưu lại khi stop
        if (songStartTime > 0) {
            Log.d(TAG, "💾 App stopped");
            songStartTime = 0;
            currentPlayingTrackId = null;
        }
    }
}