# ❓ CÂU HỎI MẪU - Câu hỏi thầy Java thường hỏi

> Tổng hợp câu hỏi mẫu + câu trả lời chuẩn cho bài thuyết trình

---

## 📋 CÁC LOẠI CÂU HỎI

1. [Câu hỏi về Luồng (Flow)](#câu-hỏi-về-luồng)
2. [Câu hỏi về Kỹ thuật (Technical)](#câu-hỏi-về-kỹ-thuật)
3. [Câu hỏi về Thiết kế (Design)](#câu-hỏi-về-thiết-kế)
4. [Câu hỏi về Xử lý lỗi (Error Handling)](#câu-hỏi-về-xử-lý-lỗi)
5. [Câu hỏi về Performance](#câu-hỏi-về-performance)

---

## CÂU HỎI VỀ LUỒNG

### ❓ Q1: Hãy giải thích luồng đăng nhập từ đầu đến cuối?

**✅ Câu trả lời:**

```
Luồng đăng nhập có 7 bước:

1. Khởi động app → LoginActivity.onCreate()
2. Kiểm tra token → SessionManager.isTokenValid()
   - Nếu còn hạn → Chuyển thẳng MainActivity
   - Nếu hết hạn → Hiển thị form login
3. User nhập username/password → Nhấn "Đăng nhập"
4. Validate input → Nếu rỗng → Show error, return
5. Gọi API → Retrofit POST /api/auth/login với LoginRequest
6. Nhận response → Parse AuthTokenResponse
7. Lưu token → SessionManager.saveToken() → SharedPreferences
8. Chuyển màn hình → startMainActivity()
```

**Điểm cộng:** Vẽ sơ đồ flow trên bảng hoặc slide.

---

### ❓ Q2: Tại sao phải check token ngay trong onCreate()?

**✅ Câu trả lời:**

```
Để tối ưu UX (User Experience):

1. User đã login lần trước → Token còn hạn
2. Nếu không check → User phải nhập lại username/password
3. Check token → Tự động vào app luôn (auto-login)
4. Token hết hạn (7 ngày) → Hiển thị form login lại

Ví dụ thực tế: Facebook, Instagram không bắt login lại mỗi lần mở app.
```

---

### ❓ Q3: Luồng chatbot có gì đặc biệt?

**✅ Câu trả lời:**

```
3 điểm đặc biệt:

1. **Instant UI Update**: 
   - Thêm tin nhắn user VÀO NGAY không đợi API
   - → UX tốt hơn, user không phải chờ

2. **Typing Indicator**:
   - Thêm "Đang trả lời..." để báo hiệu bot đang xử lý
   - → Tránh user nghĩ app bị treo

3. **Async API Call**:
   - OkHttp.enqueue() chạy background
   - runOnUiThread() để update UI khi có response
   - → App không freeze
```

**Minh họa:**
```
Timeline:
0s  → User gõ "Hello"
0s  → Thêm ngay vào RecyclerView (không đợi)
0s  → Show "Đang trả lời..."
0s  → Gọi API async
2s  → API response "Hi there!"
2s  → Xóa "Đang trả lời..."
2s  → Thêm "Hi there!" vào RecyclerView
```

---

### ❓ Q4: Tại sao lại lưu lịch sử NGAY KHI BẮT ĐẦU phát, không đợi nghe hết?

**✅ Câu trả lời:**

```
Vì 3 lý do:

1. **User có thể thoát app bất kỳ lúc nào**
   - Nhận điện thoại, đóng app
   - → Không lưu → Mất data

2. **User có thể skip bài giữa chừng**
   - Nghe 10 giây → Không thích → Next
   - → Vẫn cần lưu "đã nghe 10 giây"

3. **Backend có thể crash**
   - Lưu ngay → Đảm bảo data
   - Lưu cuối → Risk cao mất data

Tương tự: YouTube lưu lịch sử ngay khi click video, không đợi xem hết.
```

**Bonus:** Giải thích thêm về `play_duration_seconds` để track user nghe bao lâu.

---

## CÂU HỎI VỀ KỸ THUẬT

### ❓ Q5: Giải thích SharedPreferences? Tại sao dùng nó?

**✅ Câu trả lời:**

```
**SharedPreferences là gì?**
- Cơ chế lưu trữ key-value trên Android
- Giống HashMap nhưng lưu vào file XML
- Persistent (không mất khi tắt app)

**Tại sao dùng?**
- Lưu data nhỏ: token, settings, flags
- Nhanh (in-memory cache)
- Đơn giản (không cần SQL)

**Khi nào KHÔNG dùng?**
- Data lớn (> 1MB) → Dùng Database
- Structured data → Dùng SQLite
- Sensitive data → Dùng EncryptedSharedPreferences
```

**Code ví dụ:**
```java
// Lưu
SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
prefs.edit()
    .putString("token", "abc123")
    .putLong("expires_at", 1699999999000L)
    .apply();

// Đọc
String token = prefs.getString("token", null);
long expiresAt = prefs.getLong("expires_at", 0);
```

---

### ❓ Q6: Retrofit vs OkHttp, khác nhau như thế nào?

**✅ Câu trả lời:**

```
**OkHttp** (Low-level):
- HTTP client cơ bản
- Phải tạo request manual
- Phải parse JSON manual
- Code nhiều, dễ sai

**Retrofit** (High-level):
- Wrapper của OkHttp
- Dùng annotation (@POST, @GET)
- Auto convert JSON ↔ Object (Gson)
- Code ít, dễ maintain

**Ví dụ so sánh:**

OkHttp (manual):
```java
Request request = new Request.Builder()
    .url("http://api.example.com/login")
    .post(RequestBody.create(...))
    .build();

Response response = client.newCall(request).execute();
String json = response.body().string();
AuthResponse auth = gson.fromJson(json, AuthResponse.class);
```

Retrofit (auto):
```java
@POST("/api/auth/login")
Call<AuthTokenResponse> login(@Body LoginRequest request);

// Gọi API
authApi.login(request).enqueue(callback);
```

**Kết luận:** Retrofit = OkHttp + Gson + Annotation Magic
```

---

### ❓ Q7: RecyclerView hoạt động như thế nào?

**✅ Câu trả lời:**

```
RecyclerView dùng **ViewHolder pattern** để tối ưu performance:

**Cơ chế:**
1. getItemCount() → Hỏi có bao nhiêu item? (VD: 1000)
2. onCreateViewHolder() → Tạo ViewHolder (chỉ tạo ~15 cái)
3. onBindViewHolder() → Bind data vào ViewHolder
4. Scroll → Tái sử dụng ViewHolder (bind data mới vào view cũ)

**Ví dụ:**
- List 1000 tin nhắn
- Màn hình hiển thị 10 tin nhắn
- RecyclerView chỉ tạo 15 ViewHolder
- Scroll xuống → Bind tin nhắn thứ 11 vào ViewHolder của tin nhắn 1
- → Performance tốt!

**So sánh ListView:**
- ListView: Tạo view mới mỗi lần scroll → Lag
- RecyclerView: Tái sử dụng ViewHolder → Smooth
```

**Minh họa:**
```
RecyclerView Pool:
┌─────────┐  ┌─────────┐  ┌─────────┐
│ViewHolder│ │ViewHolder│ │ViewHolder│ ...
│   #1     │ │   #2     │ │   #3     │
└─────────┘  └─────────┘  └─────────┘

Scroll down:
- ViewHolder #1 (was item 1) → Now bind item 11
- ViewHolder #2 (was item 2) → Now bind item 12
```

---

### ❓ Q8: Tại sao API call phải async?

**✅ Câu trả lời:**

```
**Lý do:**
1. API call mất thời gian (1-5 giây)
2. Gọi đồng bộ → Block UI thread → App freeze
3. Android sẽ crash với NetworkOnMainThreadException

**Async methods:**
- Retrofit: enqueue() (chạy background thread)
- OkHttp: enqueue() (chạy background thread)
- Coroutines: launch { } (Kotlin)

**Threading:**
- Main thread (UI thread): Update UI, handle user input (60fps)
- Background thread: Network, database, file I/O

**Code ví dụ:**
```java
// ❌ WRONG: Sync call (CRASH!)
Response response = authApi.login(request).execute();

// ✅ CORRECT: Async call
authApi.login(request).enqueue(new Callback<>() {
    @Override
    public void onResponse(...) {
        // Background thread
        runOnUiThread(() -> {
            // Update UI on main thread
        });
    }
});
```
```

---

## CÂU HỎI VỀ THIẾT KẾ

### ❓ Q9: Tại sao tách HistoryManager ra khỏi PlayerActivity?

**✅ Câu trả lời:**

```
Áp dụng nguyên tắc **Separation of Concerns** (Tách biệt trách nhiệm):

**Lợi ích:**
1. **Single Responsibility**
   - PlayerActivity: Lo UI và phát nhạc
   - HistoryManager: Lo logic lưu lịch sử
   - → Mỗi class 1 nhiệm vụ rõ ràng

2. **Reusability**
   - HistoryManager có thể dùng ở nhiều nơi
   - VD: MainActivity, QueueActivity cũng lưu history

3. **Testability**
   - Unit test HistoryManager độc lập
   - Mock dễ dàng
   - Không cần khởi tạo Activity

4. **Maintainability**
   - Bug ở history → Chỉ sửa HistoryManager
   - Code dễ đọc, dễ hiểu

**So sánh:**
- ❌ Tất cả trong PlayerActivity: 1500 dòng, khó đọc
- ✅ Tách ra: PlayerActivity 500 dòng, HistoryManager 300 dòng
```

---

### ❓ Q10: Tại sao lại dùng Interface cho DeezerApi?

**✅ Câu trả lời:**

```
**Lý do:**
1. **Retrofit yêu cầu interface**
   - Retrofit đọc annotation (@POST, @GET, @Body)
   - Generate implementation tự động (runtime)
   - Developer chỉ định nghĩa, không implement

2. **Loose Coupling**
   - Không phụ thuộc vào implementation cụ thể
   - Dễ swap implementation (mock cho testing)

3. **Contract Definition**
   - Interface = Contract giữa client và server
   - Rõ ràng: Method nào, parameter gì, response ra sao

**Ví dụ:**
```java
public interface DeezerApi {
    @POST("/api/auth/login")
    Call<AuthTokenResponse> login(@Body LoginRequest request);
    
    @GET("/api/history")
    Call<HistoryResponse> getHistory(@Query("limit") int limit);
}

// Retrofit generate implementation:
DeezerApi api = retrofit.create(DeezerApi.class);
```

**Mock cho testing:**
```java
class MockDeezerApi implements DeezerApi {
    @Override
    public Call<AuthTokenResponse> login(LoginRequest request) {
        // Return fake response for testing
        return ...;
    }
}
```
```

---

### ❓ Q11: SessionManager có thể dùng pattern nào?

**✅ Câu trả lời:**

```
**Singleton Pattern** - Phù hợp nhất!

**Lý do:**
1. Chỉ cần 1 instance duy nhất
2. Truy cập toàn cục (global access)
3. Tránh conflict (nhiều instance → inconsistent data)

**Implement:**
```java
public class SessionManager {
    private static SessionManager instance;
    private final SharedPreferences prefs;
    
    // Private constructor
    private SessionManager(Context context) {
        this.prefs = context.getSharedPreferences("Session", MODE_PRIVATE);
    }
    
    // Get instance
    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }
    
    // Methods...
}

// Sử dụng:
SessionManager.getInstance(this).saveToken(...);
```

**Lưu ý:** 
- Dùng synchronized để thread-safe
- Double-check locking pattern
```

---

## CÂU HỎI VỀ XỬ LÝ LỖI

### ❓ Q12: Nếu API trả về lỗi 401 Unauthorized thì phải xử lý sao?

**✅ Câu trả lời:**

```
**Lỗi 401 = Token expired/invalid**

**Xử lý:**
1. Clear session
2. Redirect về LoginActivity
3. Show message "Phiên đăng nhập hết hạn"

**Implement bằng Interceptor:**
```java
OkHttpClient client = new OkHttpClient.Builder()
    .addInterceptor(chain -> {
        Response response = chain.proceed(chain.request());
        
        if (response.code() == 401) {
            // Token expired
            sessionManager.clear();
            
            // Redirect to login
            Intent intent = new Intent(context, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }
        
        return response;
    })
    .build();
```

**Lợi ích:**
- Xử lý tập trung (1 chỗ)
- Không cần check 401 ở mọi API call
```

---

### ❓ Q13: Nếu mất mạng khi gọi API thì sao?

**✅ Câu trả lời:**

```
**Xử lý:**
1. Catch trong onFailure()
2. Show error message cho user
3. (Optional) Retry mechanism
4. (Optional) Offline queue

**Code ví dụ:**
```java
@Override
public void onFailure(Call<Response> call, Throwable t) {
    if (t instanceof IOException) {
        // Network error
        Toast.makeText(this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
        
        // Queue for retry (nếu có)
        retryQueue.add(call);
    } else {
        // Other error (parsing, etc.)
        Log.e(TAG, "API error: " + t.getMessage());
    }
}
```

**Advanced: Offline Queue**
```java
// Lưu request vào database khi offline
if (!isNetworkAvailable()) {
    database.saveFailedRequest(request);
    Toast.makeText(this, "Lưu offline, sẽ sync sau", Toast.LENGTH_SHORT).show();
}

// Sync khi có mạng
if (isNetworkAvailable()) {
    List<Request> failedRequests = database.getFailedRequests();
    for (Request req : failedRequests) {
        retryRequest(req);
    }
}
```
```

---

### ❓ Q14: Validate input như thế nào?

**✅ Câu trả lời:**

```
**3 cấp độ validate:**

1. **Client-side (App)**
   - Kiểm tra rỗng, format, length
   - Show error ngay lập tức
   - Giảm tải server

2. **Server-side (Backend)**
   - Validate lại mọi input
   - Không tin client
   - Bảo mật

3. **Database-side**
   - Constraint (NOT NULL, UNIQUE)
   - Trigger validation

**Code ví dụ:**
```java
private boolean validateInput() {
    String username = etUsername.getText().toString().trim();
    String password = etPassword.getText().toString().trim();
    
    // Check empty
    if (username.isEmpty()) {
        etUsername.setError("Username không được rỗng");
        return false;
    }
    
    // Check length
    if (password.length() < 6) {
        etPassword.setError("Password phải >= 6 ký tự");
        return false;
    }
    
    // Check format (email)
    if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
        etUsername.setError("Email không hợp lệ");
        return false;
    }
    
    return true;
}

private void handleLogin() {
    if (!validateInput()) {
        return; // Stop if validation fails
    }
    
    // Continue with API call...
}
```
```

---

## CÂU HỎI VỀ PERFORMANCE

### ❓ Q15: Làm sao để tối ưu RecyclerView?

**✅ Câu trả lời:**

```
**5 kỹ thuật tối ưu:**

1. **Dùng ViewHolder pattern** (mặc định)
   - Tránh findViewById() nhiều lần

2. **setHasFixedSize(true)**
   - Nếu size không đổi
   - RecyclerView skip measure step
   
3. **setItemViewCacheSize()**
   - Tăng cache size
   - Giảm bind lại khi scroll ngược
   
4. **DiffUtil**
   - So sánh 2 list, chỉ update thay đổi
   - Thay vì notifyDataSetChanged()
   
5. **Lazy loading / Pagination**
   - Load 20 item mỗi lần
   - Load more khi scroll đến cuối

**Code ví dụ:**
```java
// Setup RecyclerView
recyclerView.setHasFixedSize(true);
recyclerView.setItemViewCacheSize(20);

// DiffUtil
DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
    new MyDiffCallback(oldList, newList)
);
diffResult.dispatchUpdatesTo(adapter);

// Pagination
recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        if (!recyclerView.canScrollVertically(1)) {
            // Reached bottom, load more
            loadMoreData();
        }
    }
});
```
```

---

### ❓ Q16: Token lưu trong SharedPreferences có an toàn không?

**✅ Câu trả lời:**

```
**Không hoàn toàn an toàn!**

**Vấn đề:**
- SharedPreferences lưu plain text
- Root device → Đọc được file XML
- Backup → Token bị leak

**Giải pháp:**

1. **EncryptedSharedPreferences** (Recommended)
```java
MasterKey masterKey = new MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build();

SharedPreferences prefs = EncryptedSharedPreferences.create(
    context,
    "secret_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
);

// Lưu như bình thường
prefs.edit().putString("token", token).apply();
```

2. **Android Keystore**
   - Lưu key trong hardware
   - An toàn nhất

3. **Short-lived token**
   - Token hết hạn nhanh (7 ngày)
   - Giảm thiểu damage

4. **Refresh token mechanism**
   - Access token: 15 phút
   - Refresh token: 7 ngày
   - Auto refresh access token
```

---

### ❓ Q17: Làm sao để giảm battery consumption?

**✅ Câu trả lời:**

```
**5 kỹ thuật:**

1. **Batch API calls**
   - Gộp nhiều request thành 1
   - Giảm wake lock

2. **JobScheduler / WorkManager**
   - Schedule task khi sạc pin
   - Schedule task khi có WiFi
   
3. **Giảm polling**
   - Thay vì check mỗi 10s
   - Dùng Push Notification (Firebase Cloud Messaging)
   
4. **Compress data**
   - Gzip compression
   - Giảm data transfer → Giảm radio time
   
5. **Cache**
   - Lưu data local
   - Không gọi API mỗi lần

**Code ví dụ:**
```java
// WorkManager for background task
WorkRequest uploadWork = new OneTimeWorkRequestBuilder<UploadWorker>()
    .setConstraints(new Constraints.Builder()
        .setRequiredNetworkType(NetworkType.WIFI)
        .setRequiresCharging(true)
        .build())
    .build();

WorkManager.getInstance(context).enqueue(uploadWork);
```
```

---

## CÂU HỎI NÂNG CAO

### ❓ Q18: MVVM vs MVP, nên dùng pattern nào?

**✅ Câu trả lời:**

```
**So sánh:**

| Aspect | MVVM | MVP |
|--------|------|-----|
| **View** | Activity/Fragment | Activity/Fragment |
| **Logic** | ViewModel | Presenter |
| **Data** | Model | Model |
| **Binding** | Data Binding (auto) | Manual |
| **Lifecycle** | Lifecycle-aware | Manual cleanup |
| **Testing** | Dễ | Trung bình |

**MVVM (Recommended):**
- ViewModel sống qua rotation
- Data Binding → Ít boilerplate
- LiveData → Reactive
- Google khuyến nghị

**Code MVVM:**
```java
// ViewModel
public class LoginViewModel extends ViewModel {
    private MutableLiveData<User> userLiveData = new MutableLiveData<>();
    
    public void login(String username, String password) {
        authApi.login(username, password).enqueue(response -> {
            userLiveData.setValue(response.body().user);
        });
    }
    
    public LiveData<User> getUser() {
        return userLiveData;
    }
}

// Activity
public class LoginActivity extends AppCompatActivity {
    private LoginViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        
        // Observe
        viewModel.getUser().observe(this, user -> {
            // Update UI
            Toast.makeText(this, "Welcome " + user.name, LENGTH_SHORT).show();
        });
        
        btnLogin.setOnClickListener(v -> {
            viewModel.login(username, password);
        });
    }
}
```
```

---

### ❓ Q19: Dependency Injection là gì? Có nên dùng Dagger/Hilt?

**✅ Câu trả lời:**

```
**Dependency Injection (DI):**
- Không tự tạo object (new ...)
- Object được "inject" từ bên ngoài
- Giảm coupling, dễ test

**Ví dụ:**
```java
// ❌ Without DI
public class LoginActivity extends AppCompatActivity {
    private DeezerApi api = new Retrofit.Builder()...create(DeezerApi.class);
    
    // Tight coupling, khó test
}

// ✅ With DI
public class LoginActivity extends AppCompatActivity {
    @Inject DeezerApi api; // Dagger inject
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MyApp) getApplication()).getAppComponent().inject(this);
        
        // api đã được inject, sẵn sàng dùng
    }
}
```

**Hilt (Recommended):**
- Dagger nhưng đơn giản hơn
- Built-in Android components
- Google khuyến nghị

**Khi nào dùng:**
- Project lớn (> 20 class)
- Nhiều dependency
- Team > 2 người

**Khi nào KHÔNG dùng:**
- Project nhỏ
- Học tập, demo
- Overhead setup
```

---

### ❓ Q20: Coroutines vs RxJava, nên dùng cái nào?

**✅ Câu trả lời:**

```
**Coroutines (Recommended):**
- Kotlin native
- Đơn giản, dễ đọc
- Nhẹ hơn RxJava
- Google khuyến nghị

**RxJava:**
- Powerful operators
- Reactive programming
- Learning curve cao

**So sánh code:**

RxJava:
```java
disposable = api.login(request)
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(
        response -> { /* success */ },
        error -> { /* error */ }
    );
```

Coroutines:
```kotlin
viewModelScope.launch {
    try {
        val response = withContext(Dispatchers.IO) {
            api.login(request)
        }
        // Update UI (on Main thread)
    } catch (e: Exception) {
        // Handle error
    }
}
```

**Kết luận:**
- New project → Coroutines
- Existing RxJava project → Keep RxJava
```

---

## 📝 TIPS ĐỂ TRẢ LỜI TỐT

### ✅ Nên làm:
1. **Trả lời rõ ràng, có cấu trúc**
   - Điểm 1, 2, 3...
   - Dùng bullet points

2. **Có ví dụ cụ thể**
   - Code snippet
   - Real-world example

3. **Giải thích "Tại sao?"**
   - Không chỉ "Làm gì?"
   - Mà cả "Tại sao làm vậy?"

4. **Thừa nhận nếu không biết**
   - "Em chưa tìm hiểu kỹ phần này"
   - "Em sẽ research thêm"

5. **Vẽ sơ đồ nếu có thể**
   - Visual > Text
   - Dễ hiểu hơn

### ❌ Không nên:
1. Trả lời mơ hồ, không rõ ràng
2. Copy-paste từ Google (thầy biết ngay)
3. Nói nhiều nhưng không đúng trọng tâm
4. Bịa đặt nếu không biết

---

## 🎯 CHECKLIST TRƯỚC KHI BÀO VỆ

- [ ] Đọc qua 3 luồng chính
- [ ] Vẽ lại sơ đồ flow (không xem tài liệu)
- [ ] Giải thích được SessionManager.isTokenValid()
- [ ] Giải thích được shouldSaveHistory() 4 checks
- [ ] Giải thích được RecyclerView.Adapter
- [ ] Trả lời được 20 câu hỏi trên
- [ ] Chuẩn bị demo code (nếu có)
- [ ] Chuẩn bị slide (nếu cần)

---

**🔥 Nắm vững 20 câu này → 9-10 điểm!**
