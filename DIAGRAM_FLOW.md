# 📊 SƠ ĐỒ LUỒNG - Visual Diagrams

> Sơ đồ chi tiết với ASCII art để dễ hình dung

---

## 🔐 LUỒNG 1: ĐĂNG NHẬP (LOGIN FLOW)

### Sơ đồ tổng quan
```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                   LOGIN FLOW DIAGRAM                     ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

        ┌─────────────────┐
        │   App Start     │
        │  (MainActivity  │
        │   Launcher)     │
        └────────┬────────┘
                 │
                 ▼
        ┌─────────────────┐
        │ LoginActivity   │
        │   .onCreate()   │
        └────────┬────────┘
                 │
                 ▼
    ┌────────────────────────┐
    │ SessionManager         │
    │ .isTokenValid()        │
    │                        │
    │ Check:                 │
    │ 1. Token exists?       │
    │ 2. Not expired?        │
    └────────┬───────────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
┌───────┐        ┌──────────┐
│  YES  │        │    NO    │
└───┬───┘        └────┬─────┘
    │                 │
    ▼                 ▼
┌────────────┐   ┌──────────────────┐
│ Navigate   │   │ setContentView   │
│ MainActivity│   │ (activity_login) │
└────────────┘   └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │ User nhập:       │
                 │ - Username       │
                 │ - Password       │
                 │ Click "Login"    │
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │ handleLogin()    │
                 └────────┬─────────┘
                          │
                          ▼
                 ┌──────────────────┐
                 │ Validate input   │
                 │ isEmpty()?       │
                 └────────┬─────────┘
                          │
                   ┌──────┴──────┐
                   │             │
                   ▼             ▼
              ┌────────┐    ┌─────────┐
              │ Empty  │    │ Valid   │
              └───┬────┘    └────┬────┘
                  │              │
                  ▼              ▼
         ┌──────────────┐  ┌──────────────────────┐
         │ Toast error  │  │ Create LoginRequest  │
         └──────────────┘  │ {username, password} │
                           └──────────┬───────────┘
                                      │
                                      ▼
                           ┌─────────────────────────┐
                           │ Retrofit API Call       │
                           │ POST /api/auth/login    │
                           │                         │
                           │ authApi.login(request)  │
                           │    .enqueue(callback)   │
                           └──────────┬──────────────┘
                                      │
                         ┌────────────┴────────────┐
                         │                         │
                         ▼                         ▼
              ┌────────────────┐        ┌────────────────┐
              │  onResponse    │        │   onFailure    │
              │  HTTP 200      │        │  Network Error │
              └────────┬───────┘        └────────┬───────┘
                       │                         │
                       ▼                         ▼
         ┌────────────────────────┐    ┌──────────────────┐
         │ response.isSuccessful()│    │ Toast:           │
         │         ?              │    │ "Lỗi kết nối"    │
         └────────┬───────────────┘    └──────────────────┘
                  │
         ┌────────┴────────┐
         │                 │
         ▼                 ▼
    ┌────────┐        ┌──────────┐
    │  YES   │        │    NO    │
    │ 200 OK │        │  401/400 │
    └───┬────┘        └────┬─────┘
        │                  │
        ▼                  ▼
┌───────────────────┐  ┌────────────────┐
│ Parse response:   │  │ Toast:         │
│ - access_token    │  │ "Sai mật khẩu" │
│ - user info       │  └────────────────┘
└────────┬──────────┘
         │
         ▼
┌────────────────────────┐
│ SessionManager         │
│ .saveToken(            │
│   accessToken,         │
│   expiresInSeconds     │
│ )                      │
└────────┬───────────────┘
         │
         ▼
┌────────────────────────┐
│ SharedPreferences      │
│ .edit()                │
│ .putString("token",..) │
│ .putLong("expires",..) │
│ .commit()              │
└────────┬───────────────┘
         │
         ▼
┌────────────────────────┐
│ startMainActivity()    │
│ Intent → MainActivity  │
│ finish()               │
└────────────────────────┘
```

### Class Diagram
```
┌────────────────────────────┐
│     LoginActivity          │
├────────────────────────────┤
│ - sessionManager           │
│ - authApi: DeezerApi       │
│ - etUsername: EditText     │
│ - etPassword: EditText     │
├────────────────────────────┤
│ + onCreate()               │
│ + handleLogin()            │
│ + startMainActivity()      │
│ + setupRetrofit()          │
└──────────┬─────────────────┘
           │ contains
           ▼
┌────────────────────────────┐
│     SessionManager         │
├────────────────────────────┤
│ - prefs: SharedPreferences │
│ - KEY_ACCESS_TOKEN         │
│ - KEY_EXPIRES_AT           │
├────────────────────────────┤
│ + saveToken()              │
│ + isTokenValid()           │
│ + getValidAccessToken()    │
│ + clear()                  │
└────────────────────────────┘

┌────────────────────────────┐
│     DeezerApi              │
│     (Interface)            │
├────────────────────────────┤
│ + login()                  │
│ + register()               │
│ + getMe()                  │
└────────────────────────────┘
```

---

## 💬 LUỒNG 2: CHATBOT

### Sơ đồ tổng quan
```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                  CHATBOT FLOW DIAGRAM                    ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

        ┌────────────────────┐
        │ User clicks        │
        │ Chatbot FAB button │
        │ (MainActivity)     │
        └──────────┬─────────┘
                   │
                   ▼
        ┌────────────────────┐
        │ ChatbotActivity    │
        │   .onCreate()      │
        └──────────┬─────────┘
                   │
                   ▼
        ┌────────────────────┐
        │ Setup RecyclerView │
        │ - LayoutManager    │
        │ - ChatAdapter      │
        │ - messageList      │
        └──────────┬─────────┘
                   │
                   ▼
        ┌────────────────────┐
        │ Empty State        │
        │ (No messages yet)  │
        └──────────┬─────────┘
                   │
                   │ User types message
                   ▼
        ┌────────────────────────────┐
        │ editTextMessage.getText()  │
        │ buttonSend.onClick()       │
        └──────────┬─────────────────┘
                   │
                   ▼
        ┌────────────────────────────┐
        │ sendMessage(messageText)   │
        └──────────┬─────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────┐
│              STEP 1: Add User Message        │
│                                              │
│  ChatMessage userMsg = new ChatMessage(     │
│      "user", messageText                    │
│  );                                         │
│                                              │
│  runOnUiThread(() -> {                      │
│      messageList.add(userMsg);              │
│      adapter.notifyItemInserted();          │
│      recyclerView.scrollToPosition();       │
│      editTextMessage.setText("");           │
│  });                                        │
└──────────────────┬───────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────┐
│         STEP 2: Add Typing Indicator         │
│                                              │
│  ChatMessage typing = new ChatMessage(      │
│      "assistant", "Đang trả lời..."         │
│  );                                         │
│  typing.setTyping(true);                    │
│                                              │
│  runOnUiThread(() -> {                      │
│      messageList.add(typing);               │
│      adapter.notifyItemInserted();          │
│  });                                        │
└──────────────────┬───────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────┐
│         STEP 3: Create JSON Request          │
│                                              │
│  JSONObject jsonBody = new JSONObject();    │
│  jsonBody.put("message", messageText);      │
│                                              │
│  RequestBody body = RequestBody.create(     │
│      jsonBody.toString(),                   │
│      MediaType.JSON                         │
│  );                                         │
└──────────────────┬───────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────┐
│         STEP 4: HTTP Request (OkHttp)        │
│                                              │
│  Request request = new Request.Builder()    │
│      .url(API_POST_MESSAGE_URL)             │
│      .post(body)                            │
│      .build();                              │
│                                              │
│  client.newCall(request).enqueue(callback); │
└──────────────────┬───────────────────────────┘
                   │
          ┌────────┴────────┐
          │                 │
          ▼                 ▼
    ┌──────────┐      ┌──────────┐
    │onResponse│      │onFailure │
    └────┬─────┘      └────┬─────┘
         │                 │
         ▼                 ▼
┌─────────────────┐  ┌─────────────────────┐
│ Parse JSON:     │  │ runOnUiThread(() -> │
│                 │  │   removeTyping();   │
│ String reply =  │  │   add error msg;    │
│   json.get(     │  │ );                  │
│     "reply"     │  └─────────────────────┘
│   );            │
└────┬────────────┘
     │
     ▼
┌─────────────────────────────┐
│ runOnUiThread(() -> {       │
│   removeTyping();           │
│   ChatMessage bot =         │
│     new ChatMessage(        │
│       "assistant", reply    │
│     );                      │
│   messageList.add(bot);     │
│   adapter.notifyInserted(); │
│   recyclerView.scroll();    │
│ });                         │
└─────────────────────────────┘
```

### RecyclerView Flow
```
┌─────────────────────────────────────────────────┐
│          RecyclerView Update Flow               │
└─────────────────────────────────────────────────┘

                messageList (ArrayList)
                ┌───────────────┐
                │ [0] user msg  │
                │ [1] bot msg   │
                │ [2] user msg  │
                │ [3] bot msg   │
                │ [4] NEW!      │ ← add(newMessage)
                └───────┬───────┘
                        │
                        ▼
                adapter.notifyItemInserted(4)
                        │
                        ▼
        ┌───────────────────────────────┐
        │ RecyclerView.Adapter          │
        ├───────────────────────────────┤
        │ onCreateViewHolder() ────┐    │
        │   (inflate layout)       │    │
        │                          ▼    │
        │ onBindViewHolder() ──► Set data
        │   (holder.textView    to view
        │    .setText(msg))            │
        │                              │
        │ getItemCount() ──► return 5  │
        └──────────────┬───────────────┘
                       │
                       ▼
            ┌──────────────────────┐
            │ Screen (RecyclerView)│
            ├──────────────────────┤
            │ [  user: "Hi"     ]  │
            │ [  bot:  "Hello!" ]  │
            │ [  user: "Help"   ]  │
            │ [  bot:  "Sure!"  ]  │
            │ [  user: "Thanks" ]  │ ← NEW!
            └──────────────────────┘
```

---

## 🎵 LUỒNG 3: LỊCH SỬ PHÁT NHẠC

### Sơ đồ tổng quan
```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃            MUSIC HISTORY SAVE FLOW DIAGRAM               ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

        ┌────────────────────┐
        │ User nhấn Play     │
        │ (PlayerActivity)   │
        └──────────┬─────────┘
                   │
                   ▼
        ┌────────────────────────────┐
        │ setupMediaPlayer()         │
        │ - Get audio URL            │
        │ - mediaPlayer.setDataSource│
        └──────────┬─────────────────┘
                   │
                   ▼
        ┌────────────────────────────┐
        │ mediaPlayer.prepareAsync() │
        └──────────┬─────────────────┘
                   │
                   ▼
        ┌────────────────────────────┐
        │ onPreparedListener         │
        │ (MediaPlayer callback)     │
        └──────────┬─────────────────┘
                   │
                   ▼
        ┌────────────────────────────┐
        │ mediaPlayer.start()        │
        │ isPlaying = true           │
        └──────────┬─────────────────┘
                   │
                   ▼
┌────────────────────────────────────────────────┐
│      ⭐ CRITICAL: Save History IMMEDIATELY     │
│                                                │
│  saveHistoryImmediately(song, durationMs)     │
└──────────────────┬─────────────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────────────┐
│     HistoryManager.saveHistoryAutoSave()      │
└──────────────────┬─────────────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────────────┐
│       STEP 1: shouldSaveHistory()?            │
│                                                │
│  ┌──────────────────────────────────────┐    │
│  │ CHECK 1: isHistoryEnabled()?         │    │
│  │ (SharedPreferences setting)          │    │
│  └──────────────┬───────────────────────┘    │
│                 │                             │
│  ┌──────────────▼───────────────────────┐    │
│  │ CHECK 2: trackId != null?            │    │
│  └──────────────┬───────────────────────┘    │
│                 │                             │
│  ┌──────────────▼───────────────────────┐    │
│  │ CHECK 3: duration >= 15 seconds?     │    │
│  │ (Minimum play duration)              │    │
│  └──────────────┬───────────────────────┘    │
│                 │                             │
│  ┌──────────────▼───────────────────────┐    │
│  │ CHECK 4: isRecentlyPlayed()?         │    │
│  │ (Dedupe window: 5 minutes)           │    │
│  │                                      │    │
│  │ recentTrackIds.contains(trackId)?    │    │
│  └──────────────┬───────────────────────┘    │
└─────────────────┬──────────────────────────────┘
                  │
         ┌────────┴────────┐
         │                 │
         ▼                 ▼
    ┌────────┐        ┌─────────┐
    │  FAIL  │        │  PASS   │
    │ (Any   │        │ (All 4  │
    │  check)│        │ checks) │
    └───┬────┘        └────┬────┘
        │                  │
        ▼                  ▼
┌───────────────┐  ┌──────────────────────────┐
│ callback.     │  │ markAsRecentlyPlayed()   │
│ onSkipped()   │  │                          │
│               │  │ String key = trackId +   │
│ Log & return  │  │   "_" + timestamp;       │
└───────────────┘  │ recentTrackIds.add(key); │
                   │ save to SharedPrefs;     │
                   └────────┬─────────────────┘
                            │
                            ▼
                   ┌──────────────────────────┐
                   │ Retrofit API Call        │
                   │ POST /api/deezer/tracks/ │
                   │      {track_id}/play     │
                   │                          │
                   │ Query params:            │
                   │ play_duration_seconds    │
                   └────────┬─────────────────┘
                            │
                   ┌────────┴────────┐
                   │                 │
                   ▼                 ▼
         ┌──────────────┐   ┌──────────────┐
         │ onResponse   │   │  onFailure   │
         │ HTTP 200     │   │ Network err  │
         └──────┬───────┘   └──────┬───────┘
                │                  │
                ▼                  ▼
    ┌──────────────────────┐  ┌─────────────────┐
    │ Parse:               │  │ callback.       │
    │ PlayTrackResponse    │  │ onError()       │
    │                      │  │                 │
    │ {                    │  │ Log error       │
    │   code: 200,         │  └─────────────────┘
    │   message: "saved",  │
    │   data: {            │
    │     id, title,       │
    │     artist, album,   │
    │     duration_ms      │
    │   }                  │
    │ }                    │
    └──────┬───────────────┘
           │
           ▼
    ┌──────────────────────┐
    │ callback.onSuccess() │
    │                      │
    │ Log.d("✅ History    │
    │   saved: " + title)  │
    └──────────────────────┘
```

### Backend Flow (Auto-save API)
```
┌─────────────────────────────────────────────────┐
│          Backend Processing Flow                │
│     (POST /api/deezer/tracks/{id}/play)         │
└─────────────────────────────────────────────────┘

Client sends:
┌────────────────────────────────┐
│ POST /api/deezer/tracks/       │
│      3n3Ppam7vgaVa1iaRUc9Lp/   │
│      play?                     │
│      play_duration_seconds=220 │
│                                │
│ Headers:                       │
│   Authorization: Bearer xxx    │
└────────────┬───────────────────┘
             │
             ▼
    ┌──────────────────┐
    │ Backend API      │
    └────────┬─────────┘
             │
    ┌────────▼─────────────────────────┐
    │ STEP 1: Extract track_id         │
    │   trackId = "3n3Ppam7vgaVa..."   │
    └────────┬─────────────────────────┘
             │
    ┌────────▼─────────────────────────┐
    │ STEP 2: Call Deezer API          │
    │   GET https://api.deezer.com/    │
    │       track/{trackId}            │
    │                                  │
    │   Response:                      │
    │   {                              │
    │     id, title, artist, album,    │
    │     duration, preview, ...       │
    │   }                              │
    └────────┬─────────────────────────┘
             │
    ┌────────▼─────────────────────────┐
    │ STEP 3: Save to Database         │
    │   INSERT INTO history (          │
    │     user_id,                     │
    │     track_id,                    │
    │     track_name,                  │
    │     artist_name,                 │
    │     album,                       │
    │     duration_ms,                 │
    │     play_duration_seconds,       │
    │     played_at                    │
    │   ) VALUES (...)                 │
    └────────┬─────────────────────────┘
             │
    ┌────────▼─────────────────────────┐
    │ STEP 4: Return Response          │
    │   HTTP 200 OK                    │
    │   {                              │
    │     "code": 200,                 │
    │     "message": "Track played...", │
    │     "data": { ... }              │
    │   }                              │
    └──────────────────────────────────┘
```

### Dedupe Mechanism
```
┌─────────────────────────────────────────────────┐
│         Duplicate Prevention (Dedupe)           │
└─────────────────────────────────────────────────┘

SharedPreferences: "recent_track_ids"
┌─────────────────────────────────────┐
│ Set<String> recentTrackIds = {      │
│   "abc123_1699999100000",           │ ← trackId + timestamp
│   "def456_1699999200000",           │
│   "abc123_1699999250000"  ← DUPLICATE!
│ }                                   │
└─────────────────────────────────────┘

Timeline:
├────────┬────────┬────────┬────────┤
0s      100s     200s     250s     300s (5min)
│        │        │        │
│        │        │        └─ Play "abc123" again
│        │        │           → Check: isRecentlyPlayed("abc123")?
│        │        │           → YES (found at 100s)
│        │        │           → SKIP save (duplicate)
│        │        │
│        │        └─ Play "def456"
│        │           → Add "def456_1699999200000"
│        │
│        └─ Play "abc123" first time
│           → Add "abc123_1699999100000"
│
└─ Dedupe window start

After 300s (5 minutes):
├────────┬────────┤
250s            550s
│               │
│               └─ cleanupOldEntries()
│                  → Remove entries older than 5 min
│                  → "abc123_1699999100000" removed
│
└─ Play "abc123" again
   → Check: isRecentlyPlayed("abc123")?
   → NO (old entry removed)
   → SAVE (allowed)
```

---

## 🔄 SEQUENCE DIAGRAM - Tổng hợp 3 luồng

### Login Sequence
```
User      LoginActivity   SessionManager   API Server   SharedPrefs
 │               │               │               │           │
 ├──Open app────►│               │               │           │
 │               ├──isTokenValid?─►               │           │
 │               │               ├──Check token───►           │
 │               │               ◄─────null───────┤           │
 │               ◄──Show form────┤               │           │
 │               │               │               │           │
 ├─Enter creds──►│               │               │           │
 │               ├──POST /login──────────────────►│           │
 │               │               │               │           │
 │               ◄──200 OK + token───────────────┤           │
 │               ├──saveToken────►               │           │
 │               │               ├──Store token───────────────►
 │               │               ◄──OK────────────────────────┤
 │               ◄──Saved────────┤               │           │
 ◄──MainActivity─┤               │               │           │
```

### Chatbot Sequence
```
User      ChatbotActivity   OkHttp   Chatbot API   RecyclerView
 │               │             │           │             │
 ├─Type message─►│             │           │             │
 │               ├─Add user msg────────────────────────►│
 │               ├─Add typing──────────────────────────►│
 │               │             │           │             │
 │               ├─POST /chat──►           │             │
 │               │             ├──Send─────►             │
 │               │             │           │             │
 │               │             ◄──Reply────┤             │
 │               ◄─onResponse──┤           │             │
 │               ├─Remove typing────────────────────────►│
 │               ├─Add bot msg──────────────────────────►│
 ◄─See reply────┤             │           │             │
```

### History Sequence
```
User    PlayerActivity  HistoryManager  DeezerApi  Backend  Database
 │            │               │            │          │        │
 ├─Press Play─►               │            │          │        │
 │            ├─start()       │            │          │        │
 │            ├─saveHistory───►            │          │        │
 │            │               ├─shouldSave?│          │        │
 │            │               │  (4 checks)│          │        │
 │            │               ├─YES────────┤          │        │
 │            │               ├─POST /play─►          │        │
 │            │               │            ├─Send─────►        │
 │            │               │            │          ├─Fetch──►
 │            │               │            │          │ Deezer │
 │            │               │            │          ◄─Info───┤
 │            │               │            │          ├─INSERT─►
 │            │               │            │          ◄─OK─────┤
 │            │               │            ◄─200 OK──┤        │
 │            │               ◄─onSuccess──┤          │        │
 │            ◄─Callback OK───┤            │          │        │
 ◄─Play music─┤               │            │          │        │
```

---

## 📊 STATE DIAGRAMS

### SessionManager States
```
┌─────────────┐
│   NO TOKEN  │
└──────┬──────┘
       │ saveToken()
       ▼
┌─────────────┐
│ VALID TOKEN │◄──────┐
└──────┬──────┘       │
       │              │ saveToken()
       │ isTokenValid()│ (refresh)
       │              │
       ├─YES──────────┘
       │
       │ Time passes...
       ▼
┌─────────────┐
│EXPIRED TOKEN│
└──────┬──────┘
       │ clear()
       ▼
┌─────────────┐
│   NO TOKEN  │
└─────────────┘
```

### MediaPlayer States
```
┌──────────┐
│   IDLE   │
└────┬─────┘
     │ setDataSource()
     ▼
┌──────────┐
│INITIALIZED│
└────┬─────┘
     │ prepareAsync()
     ▼
┌──────────┐
│PREPARING │
└────┬─────┘
     │ onPrepared()
     ▼
┌──────────┐
│ PREPARED │
└────┬─────┘
     │ start()
     ▼
┌──────────┐  ⭐ LƯU LỊCH SỬ TẠI ĐÂY!
│ STARTED  │─────────────►[saveHistory]
└────┬─────┘
     │ pause()
     ▼
┌──────────┐
│ PAUSED   │
└────┬─────┘
     │ stop()
     ▼
┌──────────┐
│ STOPPED  │
└────┬─────┘
     │ release()
     ▼
┌──────────┐
│   END    │
└──────────┘
```

---

## 🗺️ NAVIGATION MAP

```
┌────────────────────────────────────────────────┐
│            App Navigation Map                  │
└────────────────────────────────────────────────┘

                 ┌──────────────┐
                 │ SplashScreen │
                 └──────┬───────┘
                        │
           ┌────────────┴────────────┐
           │                         │
           ▼                         ▼
    ┌──────────────┐         ┌──────────────┐
    │LoginActivity │         │ MainActivity │
    │              │────────►│   (Home)     │
    └──────┬───────┘  login  └──────┬───────┘
           │                         │
           │ register                │
           ▼                         │
    ┌──────────────┐                │
    │RegisterActivity│               │
    └──────────────┘                │
                                    │
        ┌───────────────────────────┼───────────────┐
        │                           │               │
        ▼                           ▼               ▼
┌───────────────┐          ┌───────────────┐  ┌──────────────┐
│PlayerActivity │          │ChatbotActivity│  │LibraryActivity│
│   (Play)      │          │   (Chat AI)   │  │  (Favorites) │
└───────┬───────┘          └───────────────┘  └──────────────┘
        │
        ├─────────► [saveHistory] ──────► Backend API
        │
        └─────────► QueueActivity
                    (Playlist)
```

---

**✅ Các sơ đồ này giúp visualize rõ ràng luồng chạy của code!**
