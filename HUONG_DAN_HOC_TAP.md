# 📚 HƯỚNG DẪN HỌC TẬP - PHÂN TÍCH LUỒNG ỨNG DỤNG MUSIC PLAYER

> **Dành cho sinh viên học Java - Phân tích theo luồng nghiệp vụ**  
> Tài liệu này giúp bạn hiểu rõ 3 luồng chính: **Login**, **Chatbot**, **Lịch sử phát nhạc**

---

## 📋 MỤC LỤC

1. [LUỒNG 1: LOGIN (Đăng nhập & Xác thực)](#luồng-1-login)
2. [LUỒNG 2: CHATBOT (Trò chuyện AI)](#luồng-2-chatbot)
3. [LUỒNG 3: LỊCH SỬ PHÁT NHẠC](#luồng-3-lịch-sử-phát-nhạc)
4. [Câu hỏi thường gặp từ giảng viên](#câu-hỏi-thường-gặp)

---

## LUỒNG 1: LOGIN (Đăng nhập & Xác thực)

### 📊 SƠ ĐỒ LUỒNG

```
┌─────────────┐
│ Khởi động   │
│     App     │
└──────┬──────┘
       │
       ▼
┌─────────────────────────┐
│  LoginActivity.onCreate │
└──────┬──────────────────┘
       │
       ▼
┌──────────────────────┐
│ SessionManager.      │
│ isTokenValid()?      │
└──────┬───────────────┘
       │
       ├──── YES ──────► MainActivity (Skip login)
       │
       └──── NO ───────┐
                       ▼
              ┌────────────────┐
              │ Hiển thị form  │
              │ Login UI       │
              └────────┬───────┘
                       │
                       ▼ (User nhập username/password)
              ┌────────────────┐
              │ handleLogin()  │
              └────────┬───────┘
                       │
                       ▼
              ┌────────────────────────┐
              │ Retrofit gọi API:      │
              │ POST /api/auth/login   │
              │ Body: {username, pwd}  │
              └────────┬───────────────┘
                       │
                       ▼
              ┌────────────────┐
              │ Response?      │
              └────┬───────────┘
                   │
                   ├── SUCCESS ──┐
                   │             ▼
                   │    ┌────────────────────┐
                   │    │ SessionManager.    │
                   │    │ saveToken()        │
                   │    └────────┬───────────┘
                   │             │
                   │             ▼
                   │    ┌────────────────────┐
                   │    │ SharedPreferences  │
                   │    │ - access_token     │
                   │    │ - expires_at       │
                   │    └────────┬───────────┘
                   │             │
                   │             ▼
                   │    MainActivity
                   │
                   └── FAIL ────► Toast "Sai mật khẩu"
```

---

### 📁 CÁC FILE LIÊN QUAN

| File | Vai trò |
|------|---------|
| `LoginActivity.java` | Activity chính xử lý đăng nhập |
| `SessionManager.java` (inner class) | Quản lý token trong SharedPreferences |
| `DeezerApi.java` | Interface định nghĩa API endpoints |
| `AuthTokenResponse.java` | Model nhận response từ server |

---

### 🔍 PHÂN TÍCH CODE CHI TIẾT

#### 1. **LoginActivity.java** - Điểm bắt đầu

**Vị trí:** `app/src/main/java/com/example/musicplayer/login/LoginActivity.java`

```java
public class LoginActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private DeezerApi authApi;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ⭐ BƯỚC 1: Khởi tạo SessionManager
        sessionManager = new SessionManager(this);
        
        // ⭐ BƯỚC 2: Kiểm tra token có còn hợp lệ không?
        if (sessionManager.isTokenValid()) {
            // Đã login rồi → Chuyển thẳng sang MainActivity
            startMainActivity();
            return;
        }
        
        // ⭐ BƯỚC 3: Chưa login → Hiển thị form
        setContentView(R.layout.activity_login);
        setupRetrofit();
        initViews();
        setupListeners();
    }
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q1:** Tại sao phải check `isTokenValid()` ngay trong `onCreate()`?
- **A1:** Để tránh user phải login lại mỗi lần mở app. Nếu token còn hạn → Tự động vào app luôn.

---

#### 2. **SessionManager** - Quản lý phiên đăng nhập

**Inner class trong LoginActivity.java**

```java
public static class SessionManager {
    private static final String PREF_NAME = "AppSession";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    
    private final SharedPreferences prefs;
    
    public SessionManager(Context context) {
        // ⭐ Khởi tạo SharedPreferences
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    // ⭐ PHƯƠNG THỨC QUAN TRỌNG 1: Lưu token
    public void saveToken(String accessToken, int expiresInSeconds) {
        // Tính thời điểm hết hạn
        long expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L);
        
        // Lưu vào SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putLong(KEY_EXPIRES_AT, expiresAt);
        editor.commit(); // Dùng commit() để đảm bảo lưu ngay lập tức
    }
    
    // ⭐ PHƯƠNG THỨC QUAN TRỌNG 2: Kiểm tra token còn hợp lệ
    public boolean isTokenValid() {
        String token = prefs.getString(KEY_ACCESS_TOKEN, null);
        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0);
        long currentTime = System.currentTimeMillis();
        
        // Check 1: Token có tồn tại không?
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        // Check 2: Token còn hạn không?
        if (currentTime >= expiresAt) {
            return false; // Token đã hết hạn
        }
        
        return true; // Token hợp lệ
    }
    
    // ⭐ PHƯƠNG THỨC QUAN TRỌNG 3: Lấy token để gọi API
    public String getValidAccessToken() {
        if (!isTokenValid()) {
            return null;
        }
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    // ⭐ PHƯƠNG THỨC QUAN TRỌNG 4: Xóa session khi logout
    public void clear() {
        prefs.edit().clear().apply();
    }
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q2:** Tại sao lại lưu cả `expiresAt` thay vì chỉ lưu token?
- **A2:** Để kiểm tra token có hết hạn không. Token hết hạn → Server sẽ từ chối → User phải login lại.

- **Q3:** SharedPreferences là gì? Tại sao dùng nó?
- **A3:** SharedPreferences là cơ chế lưu trữ key-value trên Android (giống HashMap nhưng lưu vào file). Dùng để lưu dữ liệu nhỏ như token, settings.

---

#### 3. **handleLogin()** - Xử lý sự kiện đăng nhập

```java
private void handleLogin() {
    // ⭐ BƯỚC 1: Lấy dữ liệu từ EditText
    String username = etUsername.getText().toString().trim();
    String password = etPassword.getText().toString().trim();
    
    // ⭐ BƯỚC 2: Validate
    if (username.isEmpty() || password.isEmpty()) {
        Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show();
        return;
    }
    
    // ⭐ BƯỚC 3: Tạo request object
    LoginRequest loginRequest = new LoginRequest(username, password);
    
    // ⭐ BƯỚC 4: Gọi API bằng Retrofit
    authApi.login(loginRequest).enqueue(new Callback<AuthTokenResponse>() {
        @Override
        public void onResponse(Call<AuthTokenResponse> call, 
                               Response<AuthTokenResponse> response) {
            // ⭐ BƯỚC 5A: Xử lý khi thành công
            if (response.isSuccessful() && response.body() != null) {
                AuthTokenResponse tokenResponse = response.body();
                
                // Lưu token vào SessionManager
                sessionManager.saveToken(
                    tokenResponse.accessToken, 
                    TOKEN_EXPIRATION_SECONDS // 7 ngày
                );
                
                // Chuyển sang MainActivity
                startMainActivity();
            } else {
                // ⭐ BƯỚC 5B: Xử lý khi fail (sai mật khẩu)
                Toast.makeText(LoginActivity.this, 
                    "Sai username/password", 
                    Toast.LENGTH_SHORT).show();
            }
        }
        
        @Override
        public void onFailure(Call<AuthTokenResponse> call, Throwable t) {
            // ⭐ BƯỚC 6: Xử lý khi lỗi mạng
            Toast.makeText(LoginActivity.this, 
                "Lỗi kết nối: " + t.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    });
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q4:** Retrofit là gì? Tại sao không dùng HttpURLConnection?
- **A4:** Retrofit là thư viện HTTP client giúp gọi API dễ dàng hơn. Tự động convert JSON ↔ Object, xử lý async, retry logic, v.v.

- **Q5:** Tại sao lại dùng `enqueue()` thay vì `execute()`?
- **A5:** `enqueue()` chạy bất đồng bộ (async) trên background thread → không block UI. `execute()` chạy đồng bộ → sẽ crash app (NetworkOnMainThreadException).

---

#### 4. **DeezerApi.java** - Định nghĩa API Endpoint

**Vị trí:** `app/src/main/java/com/example/musicplayer/api/DeezerApi.java`

```java
public interface DeezerApi {
    // ⭐ Endpoint đăng nhập
    @POST("/api/auth/login")
    Call<LoginActivity.AuthTokenResponse> login(
        @Body LoginActivity.LoginRequest request
    );
    
    // ⭐ Endpoint đăng ký
    @POST("/api/auth/register")
    Call<LoginActivity.AuthTokenResponse> register(
        @Body LoginActivity.RegisterRequest request
    );
    
    // ⭐ Endpoint lấy thông tin user
    @GET("/api/auth/me")
    Call<LoginActivity.AuthTokenResponse.User> getMe();
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q6:** Annotation `@POST`, `@Body` là gì?
- **A6:** 
  - `@POST`: Chỉ định HTTP method là POST
  - `@Body`: Tự động convert object Java → JSON và gửi trong request body

---

#### 5. **AuthTokenResponse.java** - Model nhận response

```java
public static class AuthTokenResponse {
    @SerializedName("access_token")
    public String accessToken;  // ← Map từ JSON field "access_token"
    
    @SerializedName("user")
    public User user;
    
    public static class User {
        @SerializedName("email")
        public String email;
        
        @SerializedName("full_name")
        public String fullName;
    }
}
```

**Response JSON từ server:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "email": "user@example.com",
    "full_name": "Nguyễn Văn A"
  }
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q7:** Tại sao lại cần `@SerializedName`?
- **A7:** Vì JSON dùng snake_case (`access_token`) nhưng Java dùng camelCase (`accessToken`). Annotation này map 2 tên khác nhau.

---

### 🔐 XÁC THỰC KHI GỌI API KHÁC

Sau khi login, mọi API call khác cần gửi token trong header:

```java
// Tạo OkHttpClient với Interceptor để tự động thêm token
OkHttpClient client = new OkHttpClient.Builder()
    .addInterceptor(chain -> {
        // ⭐ Lấy token từ SessionManager
        String token = sessionManager.getValidAccessToken();
        
        if (token == null) {
            // Token hết hạn → Redirect về Login
            redirectToLogin();
            return chain.proceed(chain.request());
        }
        
        // ⭐ Thêm Authorization header
        Request newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer " + token)
            .build();
        
        return chain.proceed(newRequest);
    })
    .build();
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q8:** Interceptor là gì?
- **A8:** Interceptor là middleware can thiệp vào mọi HTTP request/response. Dùng để:
  - Thêm header chung (token, user-agent)
  - Log request/response
  - Xử lý lỗi tập trung (401 → redirect login)

---

### 📝 TÓM TẮT LUỒNG LOGIN

1. **App khởi động** → `LoginActivity.onCreate()`
2. **Check token** → `SessionManager.isTokenValid()`
   - Có token hợp lệ → MainActivity
   - Không có → Hiển thị form login
3. **User nhập** username/password → Nhấn "Đăng nhập"
4. **Gọi API** → `POST /api/auth/login` (Retrofit)
5. **Nhận response** → `AuthTokenResponse`
6. **Lưu token** → `SessionManager.saveToken()` → SharedPreferences
7. **Chuyển màn hình** → MainActivity

---

## LUỒNG 2: CHATBOT (Trò chuyện AI)

### 📊 SƠ ĐỒ LUỒNG

```
┌─────────────────┐
│ User mở Chatbot │
│   (FAB button)  │
└────────┬────────┘
         │
         ▼
┌──────────────────────┐
│ ChatbotActivity.     │
│ onCreate()           │
└────────┬─────────────┘
         │
         ▼
┌──────────────────────┐
│ Hiển thị RecyclerView│
│ (danh sách tin nhắn) │
└────────┬─────────────┘
         │
         ▼ (User nhập tin nhắn)
┌──────────────────────┐
│ sendMessage()        │
└────────┬─────────────┘
         │
         ▼
┌────────────────────────────┐
│ 1. Thêm tin nhắn user vào  │
│    RecyclerView (ngay lập  │
│    tức - UI update)        │
└────────┬───────────────────┘
         │
         ▼
┌────────────────────────────┐
│ 2. Thêm "Đang trả lời..."  │
│    (typing indicator)      │
└────────┬───────────────────┘
         │
         ▼
┌─────────────────────────────┐
│ 3. Gọi API Chatbot:         │
│    POST /api/chatbot/       │
│         chat/public         │
│    Body: {"message": "..."} │
└────────┬────────────────────┘
         │
         ▼
┌──────────────────┐
│ OkHttp async     │
│ request          │
└────────┬─────────┘
         │
         ├─── SUCCESS ──┐
         │              ▼
         │     ┌───────────────────┐
         │     │ Parse JSON        │
         │     │ {"reply": "..."}  │
         │     └────────┬──────────┘
         │              │
         │              ▼
         │     ┌───────────────────┐
         │     │ Xóa typing        │
         │     │ indicator         │
         │     └────────┬──────────┘
         │              │
         │              ▼
         │     ┌───────────────────┐
         │     │ Thêm phản hồi bot │
         │     │ vào RecyclerView  │
         │     └───────────────────┘
         │
         └─── FAILURE ──► Toast "Lỗi kết nối"
```

---

### 📁 CÁC FILE LIÊN QUAN

| File | Vai trò |
|------|---------|
| `ChatbotActivity.java` | Activity quản lý màn hình chat |
| `ChatAdapter.java` | RecyclerView Adapter hiển thị tin nhắn |
| `ChatMessage.java` | Model lưu 1 tin nhắn (role, content) |

---

### 🔍 PHÂN TÍCH CODE CHI TIẾT

#### 1. **ChatbotActivity.java** - Activity chính

**Vị trí:** `app/src/main/java/com/example/musicplayer/chatbot/ChatbotActivity.java`

```java
public class ChatbotActivity extends AppCompatActivity {
    // ⭐ Constants
    private static final String BASE_URL = "http://192.168.30.28:5030";
    private static final String API_POST_MESSAGE_URL = 
        BASE_URL + "/api/chatbot/chat/public";
    
    // ⭐ UI Components
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    
    // ⭐ Data
    private final List<ChatMessage> messageList = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);
        
        // ⭐ BƯỚC 1: Khởi tạo views
        initViews();
        
        // ⭐ BƯỚC 2: Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(adapter);
        
        // ⭐ BƯỚC 3: Setup listeners
        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
            }
        });
    }
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q9:** RecyclerView khác gì ListView?
- **A9:** RecyclerView hiệu quả hơn ListView vì:
  - Tái sử dụng ViewHolder → Giảm tạo view mới
  - Hỗ trợ animation tốt hơn
  - Flexible layout (Linear, Grid, Staggered)

---

#### 2. **sendMessage()** - Gửi tin nhắn đến Chatbot

```java
private void sendMessage(String messageText) {
    // ⭐ BƯỚC 1: Thêm tin nhắn user vào UI ngay lập tức
    ChatMessage userMessage = new ChatMessage("user", messageText);
    runOnUiThread(() -> {
        messageList.add(userMessage);
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
        editTextMessage.setText(""); // Clear input
    });
    
    // ⭐ BƯỚC 2: Thêm typing indicator "Đang trả lời..."
    ChatMessage typingMessage = new ChatMessage("assistant", "Đang trả lời...");
    typingMessage.setTyping(true);
    final int typingPosition = messageList.size();
    
    runOnUiThread(() -> {
        messageList.add(typingMessage);
        adapter.notifyItemInserted(messageList.size() - 1);
    });
    
    // ⭐ BƯỚC 3: Tạo JSON request body
    JSONObject jsonBody = new JSONObject();
    try {
        jsonBody.put("message", messageText);
    } catch (JSONException e) {
        Log.e(TAG, "Failed to create JSON", e);
        return;
    }
    
    // ⭐ BƯỚC 4: Tạo HTTP request với OkHttp
    RequestBody body = RequestBody.create(
        jsonBody.toString(),
        MediaType.get("application/json; charset=utf-8")
    );
    
    Request request = new Request.Builder()
        .url(API_POST_MESSAGE_URL)
        .post(body)
        .build();
    
    // ⭐ BƯỚC 5: Gọi API bất đồng bộ
    client.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            // ⭐ Xử lý lỗi kết nối
            runOnUiThread(() -> {
                removeTypingIndicator(typingPosition);
                ChatMessage errorMessage = new ChatMessage(
                    "system", 
                    "Lỗi: Không thể kết nối đến máy chủ"
                );
                messageList.add(errorMessage);
                adapter.notifyItemInserted(messageList.size() - 1);
            });
        }
        
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String responseBody = response.body().string();
            
            // ⭐ BƯỚC 6: Parse JSON response
            try {
                JSONObject responseObject = new JSONObject(responseBody);
                
                // Ưu tiên field "reply"
                String replyText;
                if (responseObject.has("reply")) {
                    replyText = responseObject.getString("reply");
                } else if (responseObject.has("response")) {
                    replyText = responseObject.getString("response");
                } else {
                    replyText = "Xin lỗi, định dạng phản hồi không đúng.";
                }
                
                // ⭐ BƯỚC 7: Cập nhật UI với phản hồi bot
                final String finalReply = replyText;
                runOnUiThread(() -> {
                    // Xóa typing indicator
                    removeTypingIndicator(typingPosition);
                    
                    // Thêm phản hồi thật
                    ChatMessage botMessage = new ChatMessage("assistant", finalReply);
                    messageList.add(botMessage);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                });
                
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse JSON", e);
            }
        }
    });
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q10:** Tại sao lại dùng `runOnUiThread()`?
- **A10:** Vì callback của OkHttp chạy trên background thread. Muốn update UI (RecyclerView, TextView) phải chạy trên UI thread (main thread).

- **Q11:** Tại sao phải thêm "Đang trả lời..." trước khi gọi API?
- **A11:** Để user biết app đang xử lý, tránh nghĩ app bị treo. Đây là UX pattern phổ biến.

---

#### 3. **ChatAdapter.java** - Hiển thị tin nhắn

**Vị trí:** `app/src/main/java/com/example/musicplayer/chatbot/ChatAdapter.java`

```java
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private final List<ChatMessage> messages;
    
    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // ⭐ Inflate layout cho từng item
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // ⭐ Bind dữ liệu vào view
        ChatMessage msg = messages.get(position);
        holder.roleView.setText(msg.getRole());
        holder.contentView.setText(msg.getContent());
        
        // ⭐ Đổi màu theo role
        if ("user".equals(msg.getRole())) {
            holder.roleView.setTextColor(Color.BLUE);
        } else if ("assistant".equals(msg.getRole())) {
            holder.roleView.setTextColor(Color.GREEN);
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView roleView, contentView;
        
        ViewHolder(View itemView) {
            super(itemView);
            roleView = itemView.findViewById(R.id.roleView);
            contentView = itemView.findViewById(R.id.contentView);
        }
    }
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q12:** ViewHolder pattern là gì?
- **A12:** ViewHolder giữ reference đến các view con → Tránh gọi `findViewById()` nhiều lần → Tăng performance.

---

#### 4. **ChatMessage.java** - Model tin nhắn

```java
public class ChatMessage {
    private String role;      // "user" | "assistant" | "system"
    private String content;   // Nội dung tin nhắn
    private boolean isTyping; // Flag cho typing indicator
    
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    // Getters & Setters
    public String getRole() { return role; }
    public String getContent() { return content; }
    public boolean isTyping() { return isTyping; }
    public void setTyping(boolean typing) { isTyping = typing; }
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q13:** Tại sao không dùng HashMap mà phải tạo class ChatMessage?
- **A13:** 
  - Type-safe: Compiler kiểm tra type
  - Auto-complete: IDE gợi ý method
  - Dễ bảo trì: Thêm field mới (timestamp, avatar) dễ dàng

---

### 📝 TÓM TẮT LUỒNG CHATBOT

1. **User mở chatbot** → `ChatbotActivity.onCreate()`
2. **Setup RecyclerView** → `ChatAdapter` hiển thị danh sách tin nhắn
3. **User gõ tin nhắn** → Nhấn "Gửi"
4. **Thêm tin nhắn user** vào UI ngay lập tức
5. **Thêm typing indicator** "Đang trả lời..."
6. **Gọi API** → `POST /api/chatbot/chat/public` (OkHttp async)
7. **Parse JSON** → Lấy field `"reply"`
8. **Xóa typing indicator** → Thêm phản hồi bot vào UI
9. **Scroll xuống cuối** để hiển thị tin nhắn mới

---

## LUỒNG 3: LỊCH SỬ PHÁT NHẠC

### 📊 SƠ ĐỒ LUỒNG

```
┌─────────────────┐
│ User nhấn Play  │
│ (PlayerActivity)│
└────────┬────────┘
         │
         ▼
┌──────────────────────────┐
│ setupMediaPlayer()       │
│ - Load audio URL         │
│ - Prepare MediaPlayer    │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│ onPrepared callback      │
│ → mediaPlayer.start()    │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────────┐
│ ⭐ QUAN TRỌNG:                │
│ saveHistoryImmediately()     │
│ (Lưu ngay khi bắt đầu phát)  │
└────────┬─────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ HistoryManager.                 │
│ shouldSaveHistory()?            │
│ - Check setting enabled?        │
│ - Check play duration >= 15s?   │
│ - Check duplicate?              │
└────────┬────────────────────────┘
         │
         ├─── NO ────► Skip (Log)
         │
         └─── YES ──┐
                    ▼
         ┌──────────────────────────┐
         │ HistoryManager.          │
         │ saveHistoryAutoSave()    │
         └──────────┬───────────────┘
                    │
                    ▼
         ┌──────────────────────────────┐
         │ Gọi API:                     │
         │ POST /api/deezer/tracks/     │
         │      {track_id}/play         │
         │ Query: play_duration_seconds │
         └──────────┬───────────────────┘
                    │
                    ▼
         ┌──────────────────────────┐
         │ Backend tự động:         │
         │ 1. Lấy info từ Deezer    │
         │ 2. Lưu vào DB history    │
         │ 3. Trả về track info     │
         └──────────┬───────────────┘
                    │
                    ▼
         ┌──────────────────────────┐
         │ PlayTrackResponse        │
         │ { code, message, data }  │
         └──────────┬───────────────┘
                    │
                    ▼
         ┌──────────────────────────┐
         │ Callback.onSuccess()     │
         │ → Log "✅ History saved" │
         └──────────────────────────┘
```

---

### 📁 CÁC FILE LIÊN QUAN

| File | Vai trò |
|------|---------|
| `PlayerActivity.java` | Activity phát nhạc |
| `HistoryManager.java` | Quản lý logic lưu lịch sử |
| `DeezerApi.java` | Interface định nghĩa endpoint |
| `PlayTrackResponse.java` | Model nhận response |
| `HistoryRecordRequest.java` | Model gửi request (fallback) |

---

### 🔍 PHÂN TÍCH CODE CHI TIẾT

#### 1. **PlayerActivity.java** - Điểm bắt đầu phát nhạc

**Vị trí:** `app/src/main/java/com/example/musicplayer/player/PlayerActivity.java`

```java
private void setupMediaPlayer() {
    Song song = playlist.get(currentSongIndex);
    String audioUrl = song.audio;
    
    // ⭐ BƯỚC 1: Setup MediaPlayer
    mediaPlayer = new MediaPlayer();
    mediaPlayer.setDataSource(audioUrl);
    
    // ⭐ BƯỚC 2: Prepare async
    mediaPlayer.setOnPreparedListener(mp -> {
        Log.d(TAG, "MediaPlayer prepared. Starting playback...");
        
        // Start phát nhạc
        mp.start();
        isPlaying = true;
        
        // ⭐ BƯỚC 3: LƯU LỊCH SỬ NGAY KHI BẮT ĐẦU PHÁT
        saveHistoryImmediately(song, song.duration);
        
        // Update UI
        updatePlayPauseButton();
        startSeekBarUpdate();
    });
    
    mediaPlayer.prepareAsync();
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q14:** Tại sao lại lưu lịch sử ngay khi bắt đầu phát, không đợi nghe hết bài?
- **A14:** Vì:
  - User có thể thoát app bất kỳ lúc nào
  - User có thể skip bài giữa chừng
  - Lưu ngay → Đảm bảo không mất data

---

#### 2. **saveHistoryImmediately()** - Lưu lịch sử

```java
private void saveHistoryImmediately(Song song, int songDurationMs) {
    // ⭐ Convert milliseconds → seconds
    int durationSeconds = songDurationMs / 1000;
    
    Log.d(TAG, "📝 Saving history for: " + song.title + 
               " (duration: " + durationSeconds + "s)");
    
    // ⭐ Gọi HistoryManager để lưu
    historyManager.saveHistoryAutoSave(
        deezerApi,              // API client
        song.id,                // Track ID (từ Deezer)
        durationSeconds,        // Thời lượng bài hát
        new HistoryManager.HistorySaveCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ History saved successfully");
            }
            
            @Override
            public void onError(int code, String message) {
                Log.e(TAG, "❌ History save failed: " + message);
            }
            
            @Override
            public void onSkipped(String reason) {
                Log.d(TAG, "⏭️ History skipped: " + reason);
            }
        }
    );
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q15:** Callback pattern là gì?
- **A15:** Callback = "Gọi lại". Vì API call là async, không biết khi nào xong → Truyền callback để "gọi lại" khi có kết quả.

---

#### 3. **HistoryManager.java** - Logic lưu lịch sử

**Vị trí:** `app/src/main/java/com/example/musicplayer/utils/HistoryManager.java`

```java
public class HistoryManager {
    // ⭐ Settings
    private static final boolean DEFAULT_ENABLED = true;
    private static final int DEFAULT_MIN_DURATION = 15; // 15 giây
    private static final int DEFAULT_DEDUPE_WINDOW = 300; // 5 phút
    
    private final SharedPreferences prefs;
    private final Set<String> recentTrackIds; // Lưu danh sách bài vừa phát
    
    public HistoryManager(Context context) {
        this.prefs = context.getSharedPreferences("HistorySettings", MODE_PRIVATE);
        this.recentTrackIds = new HashSet<>(
            prefs.getStringSet("recent_track_ids", new HashSet<>())
        );
    }
}
```

---

#### 4. **shouldSaveHistory()** - Kiểm tra điều kiện

```java
public boolean shouldSaveHistory(String trackId, int playDurationSeconds) {
    // ⭐ CHECK 1: Setting có bật không?
    if (!isHistoryEnabled()) {
        Log.d(TAG, "⏸️ History tracking is disabled");
        return false;
    }
    
    // ⭐ CHECK 2: Track ID có hợp lệ không?
    if (trackId == null || trackId.trim().isEmpty()) {
        Log.w(TAG, "⚠️ Invalid track ID");
        return false;
    }
    
    // ⭐ CHECK 3: Thời gian phát đủ tối thiểu chưa? (15 giây)
    int minDuration = getMinPlayDuration(); // 15
    if (playDurationSeconds < minDuration) {
        Log.d(TAG, "⏭️ Play duration (" + playDurationSeconds + "s) < minimum");
        return false;
    }
    
    // ⭐ CHECK 4: Bài này vừa phát gần đây chưa? (5 phút)
    if (isRecentlyPlayed(trackId)) {
        Log.d(TAG, "🔁 Track was recently played, skipping duplicate");
        return false;
    }
    
    Log.d(TAG, "✅ All checks passed, will save to history");
    return true;
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q16:** Tại sao phải check "recently played"?
- **A16:** Tránh trường hợp:
  - User tua đi tua lại cùng 1 bài → Lưu nhiều lần
  - User replay ngay sau khi vừa nghe xong → Trùng lặp

---

#### 5. **saveHistoryAutoSave()** - Gọi API

```java
public void saveHistoryAutoSave(
    DeezerApi deezerApi,
    String trackId,
    int playDurationSeconds,
    HistorySaveCallback callback
) {
    // ⭐ BƯỚC 1: Kiểm tra điều kiện
    if (!shouldSaveHistory(trackId, playDurationSeconds)) {
        if (callback != null) {
            callback.onSkipped("Conditions not met");
        }
        return;
    }
    
    // ⭐ BƯỚC 2: Đánh dấu track này đã được phát
    markAsRecentlyPlayed(trackId);
    
    Log.d(TAG, "📤 Auto-saving history for track: " + trackId);
    
    // ⭐ BƯỚC 3: Gọi API
    deezerApi.playTrack(trackId, playDurationSeconds)
        .enqueue(new Callback<PlayTrackResponse>() {
            @Override
            public void onResponse(Call<PlayTrackResponse> call, 
                                   Response<PlayTrackResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // ⭐ BƯỚC 4A: Thành công
                    PlayTrackResponse.TrackData track = response.body().data;
                    Log.d(TAG, "✅ History saved: " + track.title);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    // ⭐ BƯỚC 4B: Thất bại
                    Log.e(TAG, "❌ Failed to save. HTTP " + response.code());
                    if (callback != null) {
                        callback.onError(response.code(), "HTTP error");
                    }
                }
            }
            
            @Override
            public void onFailure(Call<PlayTrackResponse> call, Throwable t) {
                // ⭐ BƯỚC 5: Lỗi mạng
                Log.e(TAG, "❌ Network error: " + t.getMessage());
                if (callback != null) {
                    callback.onError(0, t.getMessage());
                }
            }
        });
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q17:** Tại sao lại gọi là "Auto-save"?
- **A17:** Vì endpoint `/api/deezer/tracks/{id}/play` tự động:
  1. Lấy thông tin bài hát từ Deezer API
  2. Lưu vào database history
  3. Trả về track info
  
  → Client không cần gửi thông tin bài hát (title, artist, ...) → Đơn giản hơn!

---

#### 6. **markAsRecentlyPlayed()** - Chống duplicate

```java
private void markAsRecentlyPlayed(String trackId) {
    // ⭐ Format: "trackId_timestamp"
    String key = trackId + "_" + System.currentTimeMillis();
    
    // ⭐ Thêm vào Set
    recentTrackIds.add(key);
    
    // ⭐ Lưu vào SharedPreferences
    prefs.edit()
        .putStringSet("recent_track_ids", recentTrackIds)
        .apply();
    
    // ⭐ Cleanup các entry cũ hơn 5 phút
    cleanupOldEntries();
}

private void cleanupOldEntries() {
    long now = System.currentTimeMillis();
    long dedupeWindow = DEFAULT_DEDUPE_WINDOW * 1000; // 5 phút
    
    // ⭐ Xóa các entry cũ
    recentTrackIds.removeIf(entry -> {
        String[] parts = entry.split("_");
        if (parts.length < 2) return true;
        
        long timestamp = Long.parseLong(parts[1]);
        return (now - timestamp) > dedupeWindow;
    });
    
    // ⭐ Lưu lại
    prefs.edit()
        .putStringSet("recent_track_ids", recentTrackIds)
        .apply();
}
```

**❓ Câu hỏi thầy có thể hỏi:**
- **Q18:** Tại sao không dùng Database mà dùng SharedPreferences?
- **A18:** Vì:
  - Dữ liệu nhỏ (chỉ lưu 5 phút gần nhất)
  - Không cần query phức tạp
  - SharedPreferences nhanh hơn, đơn giản hơn

---

#### 7. **DeezerApi.java** - Endpoint definition

```java
public interface DeezerApi {
    // ⭐ Auto-save API (Recommended)
    @POST("/api/deezer/tracks/{track_id}/play")
    Call<PlayTrackResponse> playTrack(
        @Path("track_id") String trackId,
        @Query("play_duration_seconds") int playDurationSeconds
    );
    
    // ⭐ Manual save API (Fallback)
    @POST("/api/history")
    Call<Void> addHistoryRecord(
        @Body HistoryRecordRequest request
    );
}
```

**Request URL ví dụ:**
```
POST http://192.168.30.28:5030/api/deezer/tracks/3n3Ppam7vgaVa1iaRUc9Lp/play?play_duration_seconds=220
```

---

#### 8. **PlayTrackResponse.java** - Response model

```java
public class PlayTrackResponse {
    @SerializedName("code")
    public int code;
    
    @SerializedName("message")
    public String message;
    
    @SerializedName("data")
    public TrackData data;
    
    public static class TrackData {
        @SerializedName("id")
        public String id;
        
        @SerializedName("title")
        public String title;
        
        @SerializedName("artist")
        public String artist;
        
        @SerializedName("album")
        public String album;
        
        @SerializedName("duration_ms")
        public int durationMs;
    }
}
```

**Response JSON ví dụ:**
```json
{
  "code": 200,
  "message": "Track played and saved to history",
  "data": {
    "id": "3n3Ppam7vgaVa1iaRUc9Lp",
    "title": "Mr. Brightside",
    "artist": "The Killers",
    "album": "Hot Fuss",
    "duration_ms": 222200
  }
}
```

---

### 📝 TÓM TẮT LUỒNG LỊCH SỬ

1. **User nhấn Play** → `setupMediaPlayer()`
2. **MediaPlayer prepared** → `mediaPlayer.start()`
3. **Gọi ngay** → `saveHistoryImmediately(song)`
4. **Check điều kiện** → `HistoryManager.shouldSaveHistory()`
   - Setting enabled? ✅
   - Track ID valid? ✅
   - Duration >= 15s? ✅
   - Not recently played? ✅
5. **Đánh dấu** → `markAsRecentlyPlayed(trackId)`
6. **Gọi API** → `POST /api/deezer/tracks/{id}/play`
7. **Backend xử lý**:
   - Lấy info từ Deezer
   - Lưu vào DB history
   - Trả về track info
8. **Callback** → `onSuccess()` / `onError()`

---

## CÂU HỎI THƯỜNG GẶP TỪ GIẢNG VIÊN

### 🎯 Về Kiến Trúc

**Q19:** Tại sao lại tách HistoryManager ra thay vì viết trực tiếp trong PlayerActivity?

**A19:** Áp dụng nguyên tắc **Separation of Concerns**:
- `PlayerActivity`: Chỉ lo giao diện và phát nhạc
- `HistoryManager`: Chỉ lo logic lưu lịch sử
- Lợi ích:
  - Code dễ đọc, dễ bảo trì
  - Có thể reuse HistoryManager ở nhiều nơi
  - Dễ test (unit test HistoryManager độc lập)

---

**Q20:** Tại sao lại dùng interface `DeezerApi` thay vì class?

**A20:** Vì Retrofit yêu cầu interface. Retrofit sẽ:
1. Đọc annotations (`@POST`, `@GET`, ...)
2. Generate implementation tự động (runtime)
3. Xử lý request/response

→ Developer chỉ cần định nghĩa interface, không cần implement!

---

### 🎯 Về Async Programming

**Q21:** Tại sao API call phải async? Không được gọi đồng bộ?

**A21:** Vì:
- API call mất thời gian (1-5 giây)
- Gọi đồng bộ → Block UI thread → App freeze
- Android sẽ crash với `NetworkOnMainThreadException`
- Async → Chạy background → UI smooth

---

**Q22:** `runOnUiThread()` và `Handler.post()` khác gì nhau?

**A22:**
- **Giống nhau**: Đều chạy code trên UI thread
- **Khác nhau**:
  - `runOnUiThread()`: Kiểm tra thread hiện tại, nếu đã là UI thread → chạy luôn
  - `Handler.post()`: Luôn post vào message queue
- **Khi nào dùng**:
  - `runOnUiThread()`: Trong Activity
  - `Handler.post()`: Trong Service, background thread

---

### 🎯 Về Data Persistence

**Q23:** SharedPreferences vs SQLite Database, khi nào dùng cái nào?

**A23:**
| Tiêu chí | SharedPreferences | SQLite |
|----------|-------------------|--------|
| **Dữ liệu** | Key-value đơn giản | Structured data |
| **Kích thước** | Nhỏ (< 1MB) | Lớn (GB) |
| **Query** | Không hỗ trợ | SQL query phức tạp |
| **Performance** | Nhanh (in-memory) | Chậm hơn (disk I/O) |
| **Use case** | Settings, token, flags | User list, chat history |

---

**Q24:** Tại sao token lại có thời hạn? Không lưu mãi mãi được sao?

**A24:** Vì bảo mật:
- Token bị lộ → Hacker dùng được
- Token có hạn → Hacker chỉ dùng được 7 ngày
- Token hết hạn → User phải login lại → Server verify lại identity

---

### 🎯 Về Networking

**Q25:** Retrofit vs OkHttp, khác nhau như thế nào?

**A25:**
- **OkHttp**: HTTP client thư viện thấp (low-level)
  - Tạo request manual
  - Parse response manual
- **Retrofit**: Wrapper của OkHttp (high-level)
  - Dùng annotation
  - Auto convert JSON ↔ Object
  - Dễ dùng hơn

→ Retrofit = OkHttp + Gson + Annotation Magic

---

**Q26:** Tại sao lại cần Interceptor?

**A26:** Để xử lý logic chung cho mọi request:
- Thêm Authorization header
- Logging (debug request/response)
- Xử lý lỗi 401 (redirect login)
- Retry logic
- Cache

→ Tránh phải copy-paste code này vào mỗi API call!

---

### 🎯 Về Android Components

**Q27:** Activity lifecycle là gì? Tại sao quan trọng?

**A27:**
```
onCreate() → onStart() → onResume() → [RUNNING]
         ↓                           ↓
    onDestroy() ← onStop() ← onPause()
```

**Quan trọng vì**:
- `onCreate()`: Khởi tạo 1 lần (setup views, load data)
- `onResume()`: Chạy mỗi khi quay lại (update UI, resume playback)
- `onPause()`: Dừng task tốn tài nguyên (pause music, save state)
- `onDestroy()`: Cleanup (release MediaPlayer, unbind service)

---

**Q28:** RecyclerView.Adapter hoạt động như thế nào?

**A28:**
1. `getItemCount()`: Hỏi có bao nhiêu item?
2. `onCreateViewHolder()`: Tạo ViewHolder mới (inflate layout)
3. `onBindViewHolder()`: Bind data vào ViewHolder
4. RecyclerView tái sử dụng ViewHolder (scroll → bind data mới)

**Ví dụ:**
- List 1000 item
- Màn hình hiển thị 10 item
- RecyclerView chỉ tạo ~15 ViewHolder
- Scroll → Bind data vào ViewHolder cũ → Performance tốt!

---

## 📚 TÀI LIỆU THAM KHẢO

### Sách & Khóa học
1. **"Android Programming: The Big Nerd Ranch Guide"** - Bill Phillips
2. **Udacity Android Developer Nanodegree**
3. **Google Codelabs - Android Basics**

### Documentation
1. [Android Developer Guides](https://developer.android.com/guide)
2. [Retrofit Documentation](https://square.github.io/retrofit/)
3. [OkHttp Documentation](https://square.github.io/okhttp/)

### Video Tutorials
1. **Coding in Flow** (YouTube) - RecyclerView, Retrofit
2. **Philipp Lackner** (YouTube) - MVVM, Coroutines
3. **CodingWithMitch** (YouTube) - Advanced Android

---

## 🎓 LỜI KHUYÊN KHI HỌC

1. **Đọc code từ trên xuống**: Bắt đầu từ Activity → Helper → Manager
2. **Debug từng bước**: Log mọi thứ để hiểu luồng chạy
3. **Vẽ sơ đồ**: Tự vẽ lại sơ đồ luồng để nhớ lâu
4. **Thực hành**: Clone project về, chạy thử, sửa code
5. **Hỏi "Tại sao?"**: Tại sao lại design như vậy? Có cách nào khác?

---

## 🔥 BÀI TẬP THỰC HÀNH

### Bài 1: Thêm chức năng "Remember me"
- Thêm checkbox "Ghi nhớ đăng nhập"
- Lưu username vào SharedPreferences
- Auto-fill username lần đăng nhập tiếp theo

### Bài 2: Hiển thị lịch sử chat
- Thêm API `GET /api/chatbot/history`
- Load lịch sử khi mở ChatbotActivity
- Hiển thị trong RecyclerView

### Bài 3: Xem lịch sử phát nhạc
- Thêm Activity mới: HistoryActivity
- Gọi API `GET /api/history`
- Hiển thị danh sách bài hát đã nghe

---

## 📞 LIÊN HỆ & HỖ TRỢ

Nếu có thắc mắc về code, hãy:
1. Đọc lại phần giải thích
2. Debug với Log.d()
3. Google lỗi (thường có kết quả trên StackOverflow)
4. Hỏi thầy cô hoặc bạn bè

**Chúc bạn học tốt! 🚀**
