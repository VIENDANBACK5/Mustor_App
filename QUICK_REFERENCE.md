# 📌 QUICK REFERENCE - Tóm tắt nhanh 3 luồng

> Tài liệu tham khảo nhanh khi thầy hỏi code

---

## 🔐 LUỒNG 1: LOGIN

### Files liên quan
```
LoginActivity.java
├── SessionManager (inner class)
│   ├── saveToken()
│   ├── isTokenValid()
│   └── getValidAccessToken()
├── handleLogin()
└── DeezerApi.java (interface)
```

### Luồng ngắn gọn
```
1. Check token → isTokenValid()
2. Nếu hợp lệ → MainActivity (skip login)
3. Nếu không → Hiển thị form
4. User nhập → handleLogin()
5. API call → POST /api/auth/login
6. Lưu token → SessionManager.saveToken()
7. Chuyển MainActivity
```

### Code quan trọng nhất
```java
// Check token
if (sessionManager.isTokenValid()) {
    startMainActivity();
    return;
}

// Lưu token sau khi login
sessionManager.saveToken(
    response.body().accessToken, 
    604800 // 7 days
);
```

### Câu hỏi hay gặp
- **Q:** Tại sao phải check token trong onCreate()?
- **A:** Để auto-login nếu token còn hạn, tránh nhập lại.

- **Q:** SharedPreferences là gì?
- **A:** Lưu key-value trên disk (giống HashMap persistent).

---

## 💬 LUỒNG 2: CHATBOT

### Files liên quan
```
ChatbotActivity.java
├── sendMessage()
│   ├── Thêm tin nhắn user vào UI
│   ├── Thêm typing indicator
│   ├── Gọi API (OkHttp)
│   └── Parse JSON → Thêm phản hồi bot
├── ChatAdapter.java (RecyclerView)
└── ChatMessage.java (model)
```

### Luồng ngắn gọn
```
1. User gõ tin nhắn → sendMessage()
2. Thêm tin nhắn user vào RecyclerView (ngay lập tức)
3. Thêm "Đang trả lời..." (typing indicator)
4. API call → POST /api/chatbot/chat/public
5. onResponse → Parse JSON {"reply": "..."}
6. Xóa typing indicator
7. Thêm phản hồi bot vào RecyclerView
```

### Code quan trọng nhất
```java
// Thêm tin nhắn user ngay lập tức
runOnUiThread(() -> {
    messageList.add(userMessage);
    adapter.notifyItemInserted(messageList.size() - 1);
});

// Gọi API async
client.newCall(request).enqueue(new Callback() {
    @Override
    public void onResponse(...) {
        // Parse JSON
        String reply = responseObject.getString("reply");
        
        // Update UI
        runOnUiThread(() -> {
            messageList.add(new ChatMessage("assistant", reply));
            adapter.notifyItemInserted(messageList.size() - 1);
        });
    }
});
```

### Câu hỏi hay gặp
- **Q:** Tại sao dùng `runOnUiThread()`?
- **A:** Vì callback OkHttp chạy trên background thread, update UI phải trên UI thread.

- **Q:** Tại sao thêm typing indicator?
- **A:** UX pattern, cho user biết app đang xử lý.

---

## 🎵 LUỒNG 3: LỊCH SỬ PHÁT NHẠC

### Files liên quan
```
PlayerActivity.java
├── setupMediaPlayer()
│   └── onPrepared → saveHistoryImmediately()
└── HistoryManager.java
    ├── shouldSaveHistory() (validation)
    │   ├── Check setting enabled?
    │   ├── Check duration >= 15s?
    │   └── Check duplicate?
    ├── markAsRecentlyPlayed() (dedupe)
    └── saveHistoryAutoSave() (API call)
```

### Luồng ngắn gọn
```
1. MediaPlayer.start() → saveHistoryImmediately()
2. HistoryManager.shouldSaveHistory() (4 checks)
3. markAsRecentlyPlayed() (dedupe trong 5 phút)
4. API call → POST /api/deezer/tracks/{id}/play
5. Backend tự động: Lấy info + Lưu DB + Trả về
6. Callback → onSuccess() / onError()
```

### Code quan trọng nhất
```java
// Lưu ngay khi bắt đầu phát
mediaPlayer.setOnPreparedListener(mp -> {
    mp.start();
    saveHistoryImmediately(song, song.duration);
});

// Validation (4 checks)
public boolean shouldSaveHistory(String trackId, int duration) {
    if (!isHistoryEnabled()) return false;      // Check 1
    if (trackId == null) return false;          // Check 2
    if (duration < MIN_DURATION) return false;  // Check 3
    if (isRecentlyPlayed(trackId)) return false;// Check 4
    return true;
}

// Gọi API auto-save
deezerApi.playTrack(trackId, durationSeconds)
    .enqueue(new Callback<PlayTrackResponse>() {
        @Override
        public void onResponse(...) {
            Log.d(TAG, "✅ History saved");
        }
    });
```

### Câu hỏi hay gặp
- **Q:** Tại sao lưu ngay, không đợi nghe hết?
- **A:** User có thể thoát app/skip bài bất kỳ lúc nào → Lưu ngay đảm bảo data.

- **Q:** "Auto-save" là gì?
- **A:** Backend tự lấy info từ Deezer API → Client chỉ gửi track_id.

- **Q:** Tại sao check "recently played"?
- **A:** Tránh duplicate nếu user tua đi tua lại hoặc replay ngay.

---

## 🎯 KIẾN THỨC NỀN TẢNG

### Android Core
| Concept | Giải thích | Ví dụ |
|---------|-----------|--------|
| **Activity** | Màn hình UI | LoginActivity |
| **Intent** | Chuyển màn hình | `startActivity(intent)` |
| **SharedPreferences** | Lưu key-value | Token, settings |
| **RecyclerView** | Danh sách hiệu quả | Chat messages |

### Networking
| Library | Vai trò | Use case |
|---------|---------|----------|
| **Retrofit** | REST API client | Gọi API với annotation |
| **OkHttp** | HTTP client | Gọi API low-level |
| **Gson** | JSON parser | JSON ↔ Object |

### Design Patterns
| Pattern | Mô tả | Ví dụ |
|---------|-------|--------|
| **Singleton** | 1 instance duy nhất | `SessionManager` |
| **Callback** | Async result handler | API callbacks |
| **Adapter** | Data ↔ View | `ChatAdapter` |
| **ViewHolder** | Tái sử dụng view | RecyclerView |

---

## 🔥 CÂU HỎI THẦY HAY HỎI

### 1. Về Async
**Q:** Tại sao API call phải async?  
**A:** Vì mất thời gian → Block UI thread → App freeze.

**Q:** `runOnUiThread()` vs `Handler.post()`?  
**A:** Giống nhau, khác: `runOnUiThread()` check thread hiện tại, `Handler.post()` luôn post queue.

### 2. Về Data
**Q:** SharedPreferences vs Database?  
**A:** SharedPrefs = key-value nhỏ, Database = structured data lớn.

**Q:** Tại sao token có hạn?  
**A:** Bảo mật: Token lộ → Hacker chỉ dùng được 7 ngày.

### 3. Về Architecture
**Q:** Tại sao tách HistoryManager?  
**A:** Separation of Concerns → Dễ đọc, reuse, test.

**Q:** Interface DeezerApi làm gì?  
**A:** Retrofit đọc annotation → Generate implementation → Xử lý request/response.

### 4. Về Threading
**Q:** Main thread vs Background thread?  
**A:** Main = UI thread (60fps), Background = Network, disk I/O.

**Q:** `enqueue()` vs `execute()`?  
**A:** `enqueue()` = async, `execute()` = sync (crash app).

---

## 📋 CHECKLIST ÔN TẬP

### Login
- [ ] Giải thích được SessionManager.isTokenValid()
- [ ] Biết cách lưu token vào SharedPreferences
- [ ] Hiểu Retrofit annotation (@POST, @Body)
- [ ] Giải thích được tại sao phải check token trong onCreate()

### Chatbot
- [ ] Giải thích được RecyclerView.Adapter
- [ ] Biết cách thêm item vào RecyclerView
- [ ] Hiểu tại sao dùng runOnUiThread()
- [ ] Giải thích được callback pattern

### Lịch sử
- [ ] Giải thích được 4 bước validation trong shouldSaveHistory()
- [ ] Hiểu cơ chế dedupe (markAsRecentlyPlayed)
- [ ] Biết tại sao lưu ngay khi bắt đầu phát
- [ ] Giải thích được auto-save API

---

## 🚀 DEMO CODE NHANH

### 1. Gọi API với Retrofit
```java
// Define interface
@POST("/api/auth/login")
Call<AuthTokenResponse> login(@Body LoginRequest request);

// Call API
authApi.login(loginRequest).enqueue(new Callback<>() {
    @Override
    public void onResponse(...) {
        // Success
    }
});
```

### 2. Lưu vào SharedPreferences
```java
SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
prefs.edit()
    .putString("token", "abc123")
    .putLong("expires_at", System.currentTimeMillis() + 3600000)
    .apply();
```

### 3. Update RecyclerView
```java
messageList.add(newMessage);
adapter.notifyItemInserted(messageList.size() - 1);
recyclerView.scrollToPosition(messageList.size() - 1);
```

### 4. Check token validity
```java
String token = prefs.getString("token", null);
long expiresAt = prefs.getLong("expires_at", 0);
boolean valid = token != null && System.currentTimeMillis() < expiresAt;
```

---

**✅ Nắm vững 4 phần này → Trả lời mọi câu hỏi của thầy!**
