package com.example.musicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton; // Thêm import
import android.widget.ImageView; // Thêm import
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Thêm import
import com.example.musicplayer.api.DeezerApi;
import com.example.musicplayer.api.DeezerSearchResponse;
import com.example.musicplayer.api.DeezerTrack;
import com.example.musicplayer.chatbot.ChatbotActivity;
import com.example.musicplayer.libary.LibraryActivity;
import com.example.musicplayer.login.LoginActivity;
import com.example.musicplayer.player.PlayerActivity;
import com.example.musicplayer.knowledge.MusicKnowledgeActivity;
import com.example.musicplayer.favorites.FavoritesActivity;
import com.example.musicplayer.history.HistoryActivity;
import com.example.musicplayer.profile.ProfileActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        MusicAdapter.OnItemClickListener,
        MusicAdapter.OnQueueActionListener {

    private static final String TAG = "MainActivity";
    private static final String API_BASE_URL = "http://192.168.30.28:5030/";
    private static final long SEARCH_DELAY = 500;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private EditText etSearchBar;
    private TextView tvQueueBadge;

    private ArrayList<Song> songList;
    private ArrayList<Song> allSongs;
    private MusicAdapter adapter;
    private DeezerApi deezerApi;
    private LoginActivity.SessionManager sessionManager;
    private LoginActivity.AuthTokenResponse.User currentUser;

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Music Service
    private MusicService musicService;
    private boolean serviceBound = false;

    // Biến UI cho Mini-Player
    private View miniPlayerContainer;
    private ImageView imgMiniCover;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private ImageButton btnMiniPlayPause;
    private ImageButton btnMiniPrevious;
    private ImageButton btnMiniNext;
    private ImageButton btnMiniQueue;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setCallback(new MusicService.MusicServiceCallback() {

                @Override
                public void onPlaybackStateChanged(boolean isPlaying) {
                    // Cập nhật nút Play/Pause trên mini-player
                    runOnUiThread(() -> {
                        if (isPlaying) {
                            btnMiniPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                        } else {
                            btnMiniPlayPause.setImageResource(android.R.drawable.ic_media_play);
                        }
                    });
                }

                @Override
                public void onSongChanged(Song song, int position) {
                    // Cập nhật thông tin bài hát trên mini-player
                    runOnUiThread(() -> {
                        if (song != null) {
                            tvMiniTitle.setText(song.title);
                            tvMiniArtist.setText(song.artist);

                            if (song.cover != null && !song.cover.isEmpty()) {
                                Glide.with(MainActivity.this)
                                        .load(song.cover)
                                        .placeholder(R.drawable.ic_album_placeholder) // Tạo một placeholder
                                        .into(imgMiniCover);
                            } else {
                                imgMiniCover.setImageResource(R.drawable.ic_album_placeholder);
                            }

                            // Hiển thị mini-player
                            miniPlayerContainer.setVisibility(View.VISIBLE);
                        } else {
                            // Không có bài hát -> ẩn mini-player
                            miniPlayerContainer.setVisibility(View.GONE);
                        }
                    });
                }

                @Override
                public void onProgressChanged(int currentPosition, int duration) {
                    // Mini-player không cần thanh progress, bỏ qua
                }

                @Override
                public void onQueueUpdated() {
//                    runOnUiThread(() -> updateQueueBadge());
                }
            });
            serviceBound = true;

            // Cập nhật UI ngay khi kết nối
            updateUIFromService();
//            updateQueueBadge();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            musicService = null;
            if (miniPlayerContainer != null) {
                miniPlayerContainer.setVisibility(View.GONE); // Ẩn đi khi mất kết nối
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new LoginActivity.SessionManager(this);

        setupRetrofit();
        setupDrawer();
        setupRecyclerView();
        setupTopBar();
        setupSearchBar();
        setupTabs();
        setupChatbot();
//        setupQueueButton();
        setupMiniPlayer(); // Thêm phương thức setup mini-player

        loadInitialData();
        bindMusicService();
    }

    private void bindMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

//    private void setupQueueButton() {
//        View btnQueue = findViewById(R.id.btnQueue);
//        tvQueueBadge = findViewById(R.id.tvQueueBadge);
//
//        btnQueue.setOnClickListener(v -> {
//            Intent intent = new Intent(this, QueueActivity.class);
//            startActivity(intent);
//        });
//    }

//    private void updateQueueBadge() {
//        if (musicService != null) {
//            int queueSize = musicService.getQueue().size();
//            if (queueSize > 0) {
//                tvQueueBadge.setVisibility(View.VISIBLE);
//                tvQueueBadge.setText(String.valueOf(queueSize));
//            } else {
//                tvQueueBadge.setVisibility(View.GONE);
//            }
//        }
//    }

    @Override
    public void onAddToQueue(Song song) {
        if (serviceBound && musicService != null) {
            musicService.addToQueue(song);
            Toast.makeText(this, "Đã thêm vào hàng đợi: " + song.title, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Music service not available", Toast.LENGTH_SHORT).show();
        }
    }

    // (Các phương thức setupRetrofit, loadInitialData, loadUserProfile, v.v... giữ nguyên)
    // ...
    private void setupRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = sessionManager.getAccessToken();
                    Request.Builder builder = chain.request().newBuilder();
                    if (token != null) {
                        builder.header("Authorization", "Bearer " + token);
                    }
                    return chain.proceed(builder.build());
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        deezerApi = retrofit.create(DeezerApi.class);
    }

    private void loadInitialData() {
//        loadUserProfile();
        loadRecommendedSongs();
    }

    // private void loadUserProfile() {
    //     deezerApi.getMe().enqueue(new Callback<LoginActivity.AuthTokenResponse.User>() {
    //         @Override
    //         public void onResponse(@NonNull Call<LoginActivity.AuthTokenResponse.User> call,
    //                                @NonNull Response<LoginActivity.AuthTokenResponse.User> response) {
    //             if (response.isSuccessful() && response.body() != null) {
    //                 currentUser = response.body();
    //                 updateDrawerHeader();
    //             } else {
    //                 handleLogout();
    //             }
    //         }

    //         @Override
    //         public void onFailure(@NonNull Call<LoginActivity.AuthTokenResponse.User> call, @NonNull Throwable t) {
    //             Toast.makeText(MainActivity.this, "Could not load user profile", Toast.LENGTH_SHORT).show();
    //         }
    //     });
    // }

    // private void updateDrawerHeader() {
    //     if (currentUser != null) {
    //         View headerView = navigationView.getHeaderView(0);
    //         TextView tvUserName = headerView.findViewById(R.id.tvUserName);
    //         TextView tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
    //         tvUserName.setText(currentUser.fullName);
    //         tvUserEmail.setText(currentUser.email);
    //     }
    // }

    private void loadRecommendedSongs() {
        deezerApi.getChart(20).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    if (responseBody.has("data")) {
                        JsonObject data = responseBody.getAsJsonObject("data");
                        if (data.has("top_tracks")) {
                            com.google.gson.JsonArray topTracks = data.getAsJsonArray("top_tracks");
                            
                            if (topTracks == null || topTracks.size() == 0) {
                                Toast.makeText(MainActivity.this, "No chart data available", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            List<DeezerTrack> tracks = new ArrayList<>();

                            for (int i = 0; i < topTracks.size(); i++) {
                                JsonObject track = topTracks.get(i).getAsJsonObject();
                                DeezerTrack deezerTrack = new DeezerTrack();
                                deezerTrack.id = track.get("id").getAsString();
                                deezerTrack.name = track.get("title").getAsString();
                                deezerTrack.artist = track.get("artist").getAsString();
                                deezerTrack.previewUrl = track.has("preview_url")
                                        ? track.get("preview_url").getAsString()
                                        : null;
                                deezerTrack.imageUrl = track.has("cover_url") ? track.get("cover_url").getAsString()
                                        : null;
                                deezerTrack.duration = track.has("duration") ? track.get("duration").getAsInt() : 0;
                                tracks.add(deezerTrack);
                            }

                            updateSongList(tracks);
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load chart", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Failed to load chart", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchSongs(String query) {
        deezerApi.searchTracks(query, 20, 0).enqueue(new Callback<DeezerSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<DeezerSearchResponse> call,
                                   @NonNull Response<DeezerSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DeezerSearchResponse searchResponse = response.body();
                    // ✅ Fix: API returns { "code": 200, "data": { "tracks": [...] } }
                    if (searchResponse.data != null 
                            && searchResponse.data.tracks != null 
                            && !searchResponse.data.tracks.isEmpty()) {
                        updateSongList(searchResponse.data.tracks);
                    } else {
                        Toast.makeText(MainActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                        songList.clear();
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<DeezerSearchResponse> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Search failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSongList(List<DeezerTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            Log.w(TAG, "⚠️ updateSongList called with null or empty tracks");
            return;
        }
        
        songList.clear();
        for (DeezerTrack track : tracks) {
            songList.add(new Song(track.id, track.name, track.getArtistsString(), track.imageUrl, track.previewUrl, "",
                    track.album, track.getDurationMs(), track.popularity, track.deezerUrl));
        }
        allSongs.clear();
        allSongs.addAll(songList);
        adapter.notifyDataSetChanged();
        
        Log.d(TAG, "✅ Updated song list with " + tracks.size() + " tracks");
    }
    // ...

    /**
     * PHƯƠNG THỨC MỚI: Khởi tạo các view và listener cho Mini-Player
     * (Đã tích hợp logic cho nút "X" - btnMiniClose)
     */
    private void setupMiniPlayer() {
        miniPlayerContainer = findViewById(R.id.miniPlayerContainer);
        imgMiniCover = findViewById(R.id.imgMiniCover);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);

        // Các nút điều khiển
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        btnMiniPrevious = findViewById(R.id.btnMiniPrevious);
        btnMiniNext = findViewById(R.id.btnMiniNext);
        btnMiniQueue = findViewById(R.id.btnMiniQueue);

        // ⭐ TÍCH HỢP: Tìm nút "X" (Close)
        // (Hãy đảm bảo ID này khớp với file layout XML của bạn)
        ImageButton btnMiniClose = findViewById(R.id.btnMiniClose);

        // Ban đầu ẩn đi
        miniPlayerContainer.setVisibility(View.GONE);

        // --- Gán sự kiện Click ---

        // Click vào toàn bộ mini-player -> mở PlayerActivity
        miniPlayerContainer.setOnClickListener(v -> openPlayerActivityFromMiniPlayer());

        // Click vào nút play/pause
        btnMiniPlayPause.setOnClickListener(v -> {
            if (serviceBound && musicService != null) {
                musicService.togglePlayPause();
            }
        });

        // Click vào nút Previous
        btnMiniPrevious.setOnClickListener(v -> {
            if (serviceBound && musicService != null) {
                musicService.playPrevious(); // Gọi hàm của service
            }
        });

        // Click vào nút Next
        btnMiniNext.setOnClickListener(v -> {
            if (serviceBound && musicService != null) {
                musicService.playNext(); // Gọi hàm của service
            }
        });

        // Click vào nút Queue
        btnMiniQueue.setOnClickListener(v -> {
            // Mở QueueActivity (giống hệt nút ở top bar)
            Intent intent = new Intent(MainActivity.this, QueueActivity.class);
            startActivity(intent);
        });

        // ⭐ TÍCH HỢP: Gán sự kiện click cho nút "X"
        if (btnMiniClose != null) {
            btnMiniClose.setOnClickListener(v -> {
                if (serviceBound && musicService != null) {

                    // 1. Dừng nhạc (hàm pause() mạnh đã có reset())
                    musicService.pause();

                    // 2. Ẩn giao diện MiniPlayer
                    miniPlayerContainer.setVisibility(View.GONE);

                    // 3. Dừng Service hoàn toàn (để xóa notification)
                    Intent stopIntent = new Intent(MainActivity.this, MusicService.class);
                    stopService(stopIntent);
                }
            });
        } else {
            // Ghi log cảnh báo nếu không tìm thấy nút, giúp bạn debug
            Log.w(TAG, "setupMiniPlayer: btnMiniClose (nút 'X') không được tìm thấy."
                    + " Hãy kiểm tra ID trong file XML layout của mini-player.");
        }
    }

    /**
     * PHƯƠNG THỨC MỚI: Cập nhật UI mini-player dựa trên trạng thái HIỆN TẠI của service.
     */
    private void updateUIFromService() {
        if (serviceBound && musicService != null) {
            // Giả sử MusicService có các phương thức này
            Song currentSong = musicService.getCurrentSong();
            boolean isPlaying = musicService.isPlaying();

            if (currentSong != null) {
                // Gọi lại các callback để đồng bộ UI
                musicService.getCallback().onSongChanged(currentSong, musicService.getCurrentIndex());
                musicService.getCallback().onPlaybackStateChanged(isPlaying);
                miniPlayerContainer.setVisibility(View.VISIBLE);
            } else {
                miniPlayerContainer.setVisibility(View.GONE);
            }
        }
    }

    /**
     * PHƯƠNG THỨC MỚI: Mở PlayerActivity dựa trên trạng thái của Service.
     */
    /**
     * PHƯƠNG THỨC MỚI: Mở PlayerActivity dựa trên trạng thái của Service.
     * (Đã sửa lỗi IndexOutOfBoundsException VÀ thêm logic bàn giao thời gian)
     */
    private void openPlayerActivityFromMiniPlayer() {
        if (serviceBound && musicService != null && musicService.getCurrentSong() != null) {

            Song currentSong = musicService.getCurrentSong();
            ArrayList<Song> currentPlaylist = musicService.getPlaylist();
            int currentIndex = musicService.getCurrentIndex();

            if (currentPlaylist == null) {
                currentPlaylist = new ArrayList<>();
            }

            // ⭐ Xử lý lỗi IndexOutOfBoundsException (-1)
            if (currentIndex == -1) {
                boolean foundInPlaylist = false;
                for (int i = 0; i < currentPlaylist.size(); i++) {
                    if (currentPlaylist.get(i).id.equals(currentSong.id)) {
                        currentIndex = i;
                        foundInPlaylist = true;
                        break;
                    }
                }

                if (!foundInPlaylist) {
                    currentPlaylist.add(currentSong);
                    currentIndex = currentPlaylist.size() - 1;
                    Log.d(TAG, "Added queue song to playlist. New index: " + currentIndex);
                }
            }
            // ⭐ Kết thúc xử lý lỗi

            if (currentPlaylist.isEmpty()) {
                Toast.makeText(this, "Lỗi: Không tìm thấy playlist", Toast.LENGTH_SHORT).show();
                return;
            }

            // Xây dựng Intent Extras
            ArrayList<String> playlistTitles = new ArrayList<>();
            ArrayList<String> playlistArtists = new ArrayList<>();
            ArrayList<String> playlistCovers = new ArrayList<>();
            ArrayList<String> playlistPreviews = new ArrayList<>();
            ArrayList<String> playlistIds = new ArrayList<>();
            ArrayList<Integer> playlistDurations = new ArrayList<>();

            for (Song song : currentPlaylist) {
                playlistTitles.add(song.title);
                playlistArtists.add(song.artist);
                playlistCovers.add(song.cover);
                playlistPreviews.add(song.audio != null ? song.audio : "");
                playlistIds.add(song.id);
                playlistDurations.add(song.durationMs);
            }

            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
            intent.putStringArrayListExtra("playlist_titles", playlistTitles);
            intent.putStringArrayListExtra("playlist_artists", playlistArtists);
            intent.putStringArrayListExtra("playlist_covers", playlistCovers);
            intent.putStringArrayListExtra("playlist_previews", playlistPreviews);
            intent.putStringArrayListExtra("playlist_ids", playlistIds);
            intent.putIntegerArrayListExtra("playlist_durations", playlistDurations);
            intent.putExtra("current_index", currentIndex);

            // ⭐ BÀN GIAO THỜI GIAN (Hand-off)
            // Lấy vị trí hiện tại của MiniPlayer và gửi nó qua Intent
            int currentPosition = musicService.getCurrentPosition();
            intent.putExtra("start_position_ms", currentPosition);
            Log.d(TAG, "Opening PlayerActivity at " + currentPosition + "ms");
            // ⭐ KẾT THÚC BÀN GIAO

            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);

        } else {
            Toast.makeText(this, "Không có nhạc để phát", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * ĐÃ REFACTOR: onClick trên item của RecyclerView
     */
    @Override
    public void onItemClick(String trackId) {
        if (!serviceBound) {
            Toast.makeText(this, "Dịch vụ chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tìm index của bài hát được click từ songList hiện tại
        int clickedIndex = -1;
        for (int i = 0; i < songList.size(); i++) {
            if (songList.get(i).id.equals(trackId)) {
                clickedIndex = i;
                break;
            }
        }

        if (clickedIndex != -1) {
            // 1. YÊU CẦU SERVICE PHÁT PLAYLIST MỚI
            // (Bạn cần thêm phương thức này vào MusicService)
            musicService.setPlaylist(new ArrayList<>(songList), clickedIndex);

            // 2. Mở PlayerActivity
            // Phương thức này sẽ lấy playlist TỪ SERVICE và mở
            openPlayerActivityFromMiniPlayer();

        } else {
            Toast.makeText(this, "Could not find the clicked song.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        songList = new ArrayList<>();
        allSongs = new ArrayList<>();
        adapter = new MusicAdapter(this, songList);
        adapter.setOnItemClickListener(this);
        adapter.setOnQueueActionListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);
        findViewById(R.id.btnMenu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setupTopBar() {
        findViewById(R.id.btnProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogout() {
        Log.d(TAG, "🚀 Đang xử lý Đăng xuất...");

        // Dừng MusicService
        if (serviceBound && musicService != null) {
            // Yêu cầu service dừng phát nhạc (hàm pause() của bạn đã có reset())
            musicService.pause();

            // Hủy liên kết (unbind) khỏi service
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi unbind service: " + e.getMessage());
            }
            serviceBound = false;
        }

        // Ra lệnh cho Service tự tắt hoàn toàn
        // (Service sẽ chạy onDestroy() và giải phóng MediaPlayer)
        Intent stopIntent = new Intent(this, MusicService.class);
        stopService(stopIntent);

        // Xóa session và chuyển Activity
        sessionManager.clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Kết thúc MainActivity
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_library) {
            startActivity(new Intent(this, LibraryActivity.class));
        } else if (id == R.id.nav_favorites) {
            startActivity(new Intent(this, FavoritesActivity.class));
        } else if (id == R.id.nav_playlists) {
            startActivity(new Intent(this, MusicKnowledgeActivity.class));
        } else if (id == R.id.nav_history) {
            startActivity(new Intent(this, HistoryActivity.class));
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupSearchBar() {
        etSearchBar = findViewById(R.id.etSearch);
        etSearchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                searchHandler.removeCallbacks(searchRunnable);

                if (query.isEmpty()) {
                    if (songList.size() != allSongs.size()) {
                        songList.clear();
                        songList.addAll(allSongs);
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    searchRunnable = () -> searchSongs(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupTabs() {
        TextView tabAll = findViewById(R.id.tabAll);
        TextView tabVPop = findViewById(R.id.tabVPop);
        TextView tabKPop = findViewById(R.id.tabKPop);
        TextView tabUSUK = findViewById(R.id.tabUSUK);

        View.OnClickListener tabClickListener = v -> {
            resetAllTabs(tabAll, tabVPop, tabKPop, tabUSUK);
            TextView clickedTab = (TextView) v;
            clickedTab.setTextColor(0xFFFFFFFF);
            clickedTab.setBackgroundResource(R.drawable.tab_selected);
            clearSearchBar();

            int id = v.getId();
            if (id == R.id.tabAll) {
                loadRecommendedSongs();
            } else if (id == R.id.tabVPop) {
                searchSongs("V-Pop");
            } else if (id == R.id.tabKPop) {
                searchSongs("K-Pop");
            } else if (id == R.id.tabUSUK) {
                searchSongs("US-UK");
            }
        };

        tabAll.setOnClickListener(tabClickListener);
        tabVPop.setOnClickListener(tabClickListener);
        tabKPop.setOnClickListener(tabClickListener);
        tabUSUK.setOnClickListener(tabClickListener);
    }

    private void setupChatbot() {
        FloatingActionButton btnChatbot = findViewById(R.id.btnChatBot);
        btnChatbot.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatbotActivity.class);
            startActivity(intent);
        });
    }

    private void resetAllTabs(TextView... tabs) {
        for (TextView tab : tabs) {
            if (tab != null) {
                tab.setTextColor(0xFFAAAAAA);
                tab.setBackgroundResource(R.drawable.tab_unselected);
            }
        }
    }

    private void clearSearchBar() {
        if (etSearchBar != null) {
            etSearchBar.setText("");
            etSearchBar.clearFocus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cập nhật queue và mini-player khi quay lại
//        updateQueueBadge();
        updateUIFromService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}