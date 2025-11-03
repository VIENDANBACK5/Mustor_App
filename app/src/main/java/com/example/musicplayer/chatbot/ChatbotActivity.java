package com.example.musicplayer.chatbot;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatbotActivity extends AppCompatActivity {

    private static final String TAG = "ChatbotActivity";
    private static final String BASE_URL = "http://192.168.30.28:5030";
    private static final String API_GET_HISTORY_URL = BASE_URL + "/api/chatbot/stream";
    private static final String API_POST_MESSAGE_URL = BASE_URL + "/api/chatbot/chat/public";

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient();
    private EditText editTextMessage;
    private ImageButton buttonSend, btnBackChatbot, btnClearChat;
    private LinearLayout emptyState;
    private boolean isWaitingForResponse = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        initViews();
        setupRecyclerView();
        setupListeners();
        updateEmptyState();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewChat);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        btnBackChatbot = findViewById(R.id.btnBackChatbot);
        btnClearChat = findViewById(R.id.btnClearChat);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        buttonSend.setOnClickListener(v -> {
            String messageText = editTextMessage.getText().toString().trim();
            if (!messageText.isEmpty() && !isWaitingForResponse) {
                sendMessage(messageText);
            }
        });

        btnBackChatbot.setOnClickListener(v -> finish());

        btnClearChat.setOnClickListener(v -> clearChat());
    }

    private void updateEmptyState() {
        if (messageList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void clearChat() {
        runOnUiThread(() -> {
            messageList.clear();
            adapter.notifyDataSetChanged();
            updateEmptyState();
        });
    }

    private void sendMessage(String messageText) {
        // Ẩn empty state và hiển thị RecyclerView
        updateEmptyState();

        // Thêm tin nhắn của người dùng
        ChatMessage userMessage = new ChatMessage("user", messageText);
        runOnUiThread(() -> {
            messageList.add(userMessage);
            adapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
            editTextMessage.setText("");
            updateEmptyState();
        });

        // Thêm tin nhắn "đang trả lời..."
        ChatMessage typingMessage = new ChatMessage("assistant", "Đang trả lời...");
        typingMessage.setTyping(true);
        final int typingPosition = messageList.size();

        runOnUiThread(() -> {
            messageList.add(typingMessage);
            adapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
        });

        // Vô hiệu hóa nút gửi
        setInputEnabled(false);

        // Gửi tin nhắn đến API
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("message", messageText);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON body", e);
            removeTypingIndicator(typingPosition);
            setInputEnabled(true);
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_POST_MESSAGE_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed: " + e.getMessage());
                runOnUiThread(() -> {
                    removeTypingIndicator(typingPosition);
                    ChatMessage errorMessage = new ChatMessage(
                            "system",
                            "Lỗi: Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng."
                    );
                    messageList.add(errorMessage);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                    setInputEnabled(true);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                Log.d(TAG, "=== CHATBOT API RESPONSE ===");
                Log.d(TAG, "Status Code: " + response.code());
                Log.d(TAG, "Response Body: " + responseBody);
                Log.d(TAG, "==========================");

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response);
                    runOnUiThread(() -> {
                        removeTypingIndicator(typingPosition);
                        ChatMessage errorMessage = new ChatMessage(
                                "system",
                                "Lỗi: Máy chủ trả về mã " + response.code()
                        );
                        messageList.add(errorMessage);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                        setInputEnabled(true);
                    });
                    return;
                }

                try {
                    JSONObject responseObject = new JSONObject(responseBody);

                    // Ưu tiên field "reply" từ API
                    String replyText;
                    if (responseObject.has("reply")) {
                        replyText = responseObject.getString("reply");
                        Log.d(TAG, "✅ Found 'reply' field");
                    } else if (responseObject.has("response")) {
                        replyText = responseObject.getString("response");
                        Log.d(TAG, "✅ Found 'response' field");
                    } else if (responseObject.has("message")) {
                        replyText = responseObject.getString("message");
                        Log.d(TAG, "✅ Found 'message' field");
                    } else {
                        StringBuilder keys = new StringBuilder();
                        JSONArray keysArray = responseObject.names();
                        if (keysArray != null) {
                            for (int i = 0; i < keysArray.length(); i++) {
                                keys.append(keysArray.getString(i)).append(", ");
                            }
                        }
                        Log.w(TAG, "❌ No valid response field. Available keys: " + keys.toString());
                        replyText = "Xin lỗi, tôi không hiểu định dạng phản hồi từ máy chủ.";
                    }

                    Log.d(TAG, "📤 Extracted reply (" + replyText.length() + " chars): " +
                            (replyText.length() > 100 ? replyText.substring(0, 100) + "..." : replyText));

                    final String finalReply = replyText;
                    runOnUiThread(() -> {
                        // Xóa tin nhắn "đang trả lời..."
                        removeTypingIndicator(typingPosition);

                        // Thêm phản hồi thực
                        ChatMessage botMessage = new ChatMessage("assistant", finalReply);
                        messageList.add(botMessage);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);

                        // Kích hoạt lại input
                        setInputEnabled(true);
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse JSON response: " + e.getMessage());
                    Log.e(TAG, "Raw response was: " + responseBody);
                    runOnUiThread(() -> {
                        removeTypingIndicator(typingPosition);
                        ChatMessage errorMessage = new ChatMessage(
                                "system",
                                "Lỗi: Không thể phân tích phản hồi từ máy chủ."
                        );
                        messageList.add(errorMessage);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                        setInputEnabled(true);
                    });
                }
            }
        });
    }

    private void removeTypingIndicator(int position) {
        if (position < messageList.size() && messageList.get(position).isTyping()) {
            messageList.remove(position);
            adapter.notifyItemRemoved(position);
        }
    }

    private void setInputEnabled(boolean enabled) {
        isWaitingForResponse = !enabled;
        buttonSend.setEnabled(enabled);
        editTextMessage.setEnabled(enabled);
        buttonSend.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void fetchMessagesFromApi() {
        Request request = new Request.Builder()
                .url(API_GET_HISTORY_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response);
                    return;
                }

                String body = response.body() != null ? response.body().string() : null;
                if (body == null) {
                    Log.e(TAG, "Empty response body");
                    return;
                }

                try {
                    JSONArray arr = new JSONArray(body);
                    final List<ChatMessage> parsed = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String role = obj.optString("role", "unknown");
                        String content = obj.optString("content", "");
                        parsed.add(new ChatMessage(role, content));
                    }

                    runOnUiThread(() -> {
                        messageList.clear();
                        messageList.addAll(parsed);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                        if (!messageList.isEmpty()) {
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse JSON: " + e.getMessage());
                }
            }
        });
    }
}