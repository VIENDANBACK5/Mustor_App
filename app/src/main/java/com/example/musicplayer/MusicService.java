package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.LinkedList;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private ArrayList<Song> playlist = new ArrayList<>();
    private LinkedList<Song> queue = new LinkedList<>(); // Hàng đợi

    // Biến trạng thái
    private Song currentSong = null; // QUAN TRỌNG: Nguồn chân lý cho bài hát đang phát
    private int currentSongIndex = -1; // -1 nghĩa là đang phát từ queue
    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private int repeatMode = 0; // 0: No, 1: All, 2: One

    private final IBinder binder = new MusicBinder();
    private MusicServiceCallback callback;

    public interface MusicServiceCallback {
        void onPlaybackStateChanged(boolean isPlaying);
        void onSongChanged(Song song, int position);
        void onProgressChanged(int currentPosition, int duration);
        void onQueueUpdated(); // Callback khi queue thay đổi
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "✅ MusicService created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // ========== PUBLIC API FOR ACTIVITIES ==========

    public void setCallback(MusicServiceCallback callback) {
        this.callback = callback;
    }

    /**
     * THÊM MỚI: Trả về callback để MainActivity đồng bộ
     */
    public MusicServiceCallback getCallback() {
        return this.callback;
    }

    /**
     * Đặt playlist mới và bắt đầu phát
     */
    public void setPlaylist(ArrayList<Song> playlist, int startIndex) {
        this.playlist = new ArrayList<>(playlist);
        // Xóa queue khi playlist mới được set
        this.queue.clear();
        if (callback != null) callback.onQueueUpdated();

        playSong(startIndex);
    }

    /**
     * THÊM MỚI: Phát một bài hát cụ thể (dùng cho MiniPlayer khi thoát PlayerActivity)
     * Đây là phương thức mà PlayerActivity cần gọi trong showMiniPlayer()
     */
    // XÓA HÀM CŨ NÀY (hoặc sửa nó, nhưng thêm hàm mới dễ hơn)
// public void play(Song song) { ... }

    /**
     * THÊM MỚI (hoặc Sửa):
     * Phát một bài hát, BẮT ĐẦU TỪ MỘT VỊ TRÍ CỤ THỂ.
     */
    public void play(Song song, int startPositionMs) {
        if (song == null || song.audio == null || song.audio.isEmpty()) {
            Log.e(TAG, "⚠️ Invalid song to play, skipping.");
            return;
        }

        this.currentSong = song;
        this.currentSongIndex = -1;

        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                );
                mediaPlayer.setOnCompletionListener(mp -> handleSongCompletion());
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: " + what);
                    playNext();
                    return true;
                });
            }

            mediaPlayer.reset();
            mediaPlayer.setDataSource(this.currentSong.audio);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {

                // ⭐ ĐÂY LÀ PHẦN SỬA ĐỔI QUAN TRỌNG
                if (startPositionMs > 0) {
                    mp.seekTo(startPositionMs);
                }
                // ⭐ KẾT THÚC SỬA ĐỔI

                mp.start();
                isPlaying = true;
                updateNotification();
                if (callback != null) {
                    callback.onPlaybackStateChanged(true);
                    callback.onSongChanged(this.currentSong, -1);
                }
                Log.d(TAG, "🎵 Playing single song from " + startPositionMs + "ms: " + this.currentSong.title);
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Error playing single song: " + e.getMessage());
        }
    }

    // CŨNG CẬP NHẬT HÀM play(Song song) cũ (nếu bạn vẫn dùng nó ở đâu đó)
// để nó gọi hàm mới này với 0 giây.
    public void play(Song song) {
        play(song, 0); // Gọi hàm mới với 0 giây
    }


    /**
     * CHỈNH SỬA: Đổi tên từ playPause() -> togglePlayPause()
     */
    public void togglePlayPause() {
        if (mediaPlayer == null || currentSong == null) return;

        try {
            if (isPlaying) {
                mediaPlayer.pause();
                isPlaying = false;
            } else {
                mediaPlayer.start();
                isPlaying = true;
            }
            updateNotification();
            if (callback != null) callback.onPlaybackStateChanged(isPlaying);
        } catch (Exception e) {
            Log.e(TAG, "Error togglePlayPause: " + e.getMessage());
        }
    }

    /**
     * THÊM MỚI: Tạm dừng nhạc (dùng cho PlayerActivity khi ẩn MiniPlayer)
     * Đây là phương thức mà PlayerActivity cần gọi trong hideMiniPlayer()
     */
    public void pause() {
        if (mediaPlayer != null) {
            try {
                // Nếu nó đang phát, tạm dừng.
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }

                // QUAN TRỌNG: Nếu nó đang "preparing", lệnh reset()
                // sẽ hủy tiến trình đó và ngăn onPreparedListener
                // tự động phát nhạc.
                mediaPlayer.reset();

                isPlaying = false;
                updateNotification(); // Cập nhật thông báo (nếu cần)
                if (callback != null) callback.onPlaybackStateChanged(false);
                Log.d(TAG, "⏸️ Music reset/paused by external request");

            } catch (Exception e) {
                // Có thể ném lỗi nếu reset() được gọi ở trạng thái không phù hợp,
                // nhưng nó an toàn trong trường hợp này.
                Log.e(TAG, "Error aggressive pause/reset: " + e.getMessage());
            }
        }
    }

    /**
     * Phát bài hát tiếp theo (ưu tiên từ queue)
     */
    public void playNext() {
        // Nếu có bài trong queue, phát bài đó trước
        if (!queue.isEmpty()) {
            Song nextSong = queue.poll(); // Lấy và xóa bài đầu queue
            playQueueSong(nextSong);
            if (callback != null) {
                callback.onQueueUpdated();
            }
            return;
        }

        // Không có queue, phát bài tiếp theo trong playlist
        if (playlist.isEmpty()) return;

        if (isShuffle) {
            int randomIndex;
            do {
                randomIndex = (int) (Math.random() * playlist.size());
            } while (randomIndex == currentSongIndex && playlist.size() > 1);
            playSong(randomIndex);
        } else {
            playSong((currentSongIndex + 1) % playlist.size());
        }
    }

    /**
     * Phát bài hát trước đó (bỏ qua queue)
     */
    public void playPrevious() {
        if (playlist.isEmpty()) return;

        // Nếu đang phát từ queue, quay lại bài playlist trước đó
        if (currentSongIndex == -1 && !playlist.isEmpty()) {
            playSong(0); // Quay về bài đầu playlist
            return;
        }

        playSong((currentSongIndex - 1 + playlist.size()) % playlist.size());
    }

    // ========== QUEUE MANAGEMENT (Giữ nguyên) ==========

    public void addToQueue(Song song) {
        queue.add(song);
        if (callback != null) callback.onQueueUpdated();
        Log.d(TAG, "➕ Added to queue: " + song.title);
    }

    public ArrayList<Song> getQueue() {
        return new ArrayList<>(queue);
    }

    public void removeFromQueue(int position) {
        if (position >= 0 && position < queue.size()) {
            Song removed = new ArrayList<>(queue).get(position);
            queue.remove(removed);
            if (callback != null) callback.onQueueUpdated();
            Log.d(TAG, "➖ Removed from queue: " + removed.title);
        }
    }

    public void clearQueue() {
        queue.clear();
        if (callback != null) callback.onQueueUpdated();
        Log.d(TAG, "🗑️ Queue cleared");
    }

    public void moveQueueItem(int fromPosition, int toPosition) {
        // (Logic giữ nguyên)
        if (fromPosition >= 0 && fromPosition < queue.size() &&
                toPosition >= 0 && toPosition < queue.size()) {
            ArrayList<Song> queueList = new ArrayList<>(queue);
            Song song = queueList.remove(fromPosition);
            queueList.add(toPosition, song);
            queue.clear();
            queue.addAll(queueList);
            if (callback != null) {
                callback.onQueueUpdated();
            }
        }
    }

    // ========== GETTERS & SETTERS (Cập nhật) ==========

    public void seekTo(int position) {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.seekTo(position);
        }
    }

    public int getCurrentPosition() {
        return (mediaPlayer != null && isPlaying) ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * CHỈNH SỬA: Trả về 'currentSong' (từ queue hoặc playlist)
     */
    public Song getCurrentSong() {
        return this.currentSong;
    }

    /**
     * Trả về index của bài hát (trong playlist)
     * Sẽ trả về -1 nếu đang phát từ queue
     */
    public int getCurrentIndex() {
        return currentSongIndex;
    }

    public ArrayList<Song> getPlaylist() {
        return new ArrayList<>(playlist);
    }

    public void setShuffle(boolean shuffle) {
        this.isShuffle = shuffle;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public void setRepeatMode(int mode) {
        this.repeatMode = mode;
    }

    public int getRepeatMode() {
        return repeatMode;
    }


    // ========== PRIVATE CORE LOGIC ==========

    /**
     * CHỈNH SỬA: Phát bài hát từ playlist
     */
    private void playSong(int index) {
        if (playlist == null || playlist.isEmpty() || index < 0 || index >= playlist.size()) {
            Log.e(TAG, "⚠️ Invalid playlist or index");
            return;
        }

        currentSongIndex = index;
        this.currentSong = playlist.get(currentSongIndex); // Cập nhật bài hát hiện tại

        if (this.currentSong == null || this.currentSong.audio == null || this.currentSong.audio.isEmpty()) {
            Log.e(TAG, "❌ Song has no audio URL. Skipping.");
            playNext(); // Tự động bỏ qua và phát bài tiếp
            return;
        }

        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                );
                mediaPlayer.setOnCompletionListener(mp -> handleSongCompletion());
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: " + what);
                    playNext(); // Thử phát bài tiếp theo nếu có lỗi
                    return true;
                });
            }

            mediaPlayer.reset();
            mediaPlayer.setDataSource(this.currentSong.audio);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                updateNotification();
                if (callback != null) {
                    callback.onPlaybackStateChanged(true);
                    callback.onSongChanged(this.currentSong, currentSongIndex);
                }
                Log.d(TAG, "🎵 Playing from playlist: " + this.currentSong.title);
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Error playing song: " + e.getMessage());
        }
    }

    /**
     * CHỈNH SỬA: Phát bài hát từ queue
     */
    private void playQueueSong(Song song) {
        if (song == null || song.audio == null || song.audio.isEmpty()) {
            Log.e(TAG, "⚠️ Invalid queue song, skipping.");
            playNext(); // Thử phát bài tiếp
            return;
        }

        this.currentSong = song; // Cập nhật bài hát hiện tại
        this.currentSongIndex = -1; // Đánh dấu là phát từ queue

        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                );
                mediaPlayer.setOnCompletionListener(mp -> handleSongCompletion());
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: " + what);
                    playNext(); // Thử phát bài tiếp theo nếu có lỗi
                    return true;
                });
            }

            mediaPlayer.reset();
            mediaPlayer.setDataSource(this.currentSong.audio);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                updateNotification();
                if (callback != null) {
                    callback.onPlaybackStateChanged(true);
                    callback.onSongChanged(this.currentSong, -1); // -1 = từ queue
                }
                Log.d(TAG, "🎵 Playing from queue: " + this.currentSong.title);
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Error playing queue song: " + e.getMessage());
        }
    }

    /**
     * CHỈNH SỬA: Xử lý khi kết thúc bài hát (đã sửa lỗi)
     */
    private void handleSongCompletion() {
        if (repeatMode == 2) {
            // Repeat One: Phát lại chính bài hát vừa xong
            if (currentSongIndex != -1) {
                playSong(currentSongIndex); // Phát lại từ playlist
            } else if (currentSong != null) {
                playQueueSong(currentSong); // Phát lại bài từ queue
            }
        } else if (!queue.isEmpty()) {
            // Ưu tiên 1: Luôn phát từ queue nếu có
            playNext(); // playNext() sẽ tự động lấy từ queue
        } else if (repeatMode == 1) {
            // Ưu tiên 2: Lặp lại tất cả (và queue rỗng)
            playNext(); // playNext() sẽ xử lý vòng lặp/xáo trộn
        } else if (!isShuffle && currentSongIndex == playlist.size() - 1) {
            // Không lặp, không xáo trộn, và là bài cuối cùng
            isPlaying = false;
            currentSong = null;
            currentSongIndex = -1;
            if (callback != null) {
                callback.onPlaybackStateChanged(false);
                callback.onSongChanged(null, -1); // Gửi null để UI ẩn đi
            }
            stopForeground(true); // Dừng thông báo
        } else {
            // Mặc định: phát bài tiếp theo
            playNext();
        }
    }


    // ========== NOTIFICATION (Giữ nguyên) ==========

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void updateNotification() {
        Song song = getCurrentSong(); // Đã được sửa để lấy đúng bài hát
        if (song == null) {
            stopForeground(true);
            return;
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // TODO: Thêm các action Play/Pause/Next vào thông báo

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setSmallIcon(R.drawable.ic_music_note) // Thay bằng icon của bạn
                // .setLargeIcon(bitmap) // Bạn có thể tải ảnh bìa ở đây
                .setContentIntent(pendingIntent)
                .setOngoing(isPlaying) // Thông báo không thể bị trượt đi khi đang phát
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        Log.d(TAG, "🛑 MusicService destroyed");
    }
}