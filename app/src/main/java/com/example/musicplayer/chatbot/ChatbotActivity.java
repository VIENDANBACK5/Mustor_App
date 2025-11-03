package com.example.musicplayer.chatbot;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    // Sử dụng IP của máy tính trong mạng LAN
    private static final String BASE_URL = "http://192.168.30.28:5030";
    private static final String API_GET_HISTORY_URL = BASE_URL + "/api/chatbot/stream";
    private static final String API_POST_MESSAGE_URL = BASE_URL + "/api/chatbot/chat/public";

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient();
    private EditText editTextMessage;
    private Button buttonSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        recyclerView = findViewById(R.id.recyclerViewChat);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(adapter);

        buttonSend.setOnClickListener(v -> {
            String messageText = editTextMessage.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            }
        });

        // Lấy lịch sử chat khi mở màn hình
        // fetchMessagesFromApi(); // Tạm thời tắt để tập trung debug POST
    }

    private void sendMessage(String messageText) {
        // 1. Thêm tin nhắn của người dùng vào UI
        ChatMessage userMessage = new ChatMessage("user", messageText);
        runOnUiThread(() -> {
            messageList.add(userMessage);
            adapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
            editTextMessage.setText("");
        });

        // 2. Gửi tin nhắn đến API
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("message", messageText);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON body", e);
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_POST_MESSAGE_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed: " + e.getMessage());
                runOnUiThread(() -> {
                    ChatMessage errorMessage = new ChatMessage("system", "Error: Could not connect to the server.");
                    messageList.add(errorMessage);
                    adapter.notifyItemInserted(messageList.size() - 1);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                // DÒNG LOG QUAN TRỌNG ĐỂ DEBUG
                Log.d(TAG, "=== CHATBOT API RESPONSE ===");
                Log.d(TAG, "Status Code: " + response.code());
                Log.d(TAG, "Response Body: " + responseBody);
                Log.d(TAG, "==========================");

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response);
                    runOnUiThread(() -> {
                        ChatMessage errorMessage = new ChatMessage("system",
                                "Error: Server returned " + response.code());
                        messageList.add(errorMessage);
                        adapter.notifyItemInserted(messageList.size() - 1);
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
                        // Log tất cả keys để debug
                        StringBuilder keys = new StringBuilder();
                        JSONArray keysArray = responseObject.names();
                        if (keysArray != null) {
                            for (int i = 0; i < keysArray.length(); i++) {
                                keys.append(keysArray.getString(i)).append(", ");
                            }
                        }
                        Log.w(TAG, "❌ No valid response field. Available keys: " + keys.toString());
                        replyText = "Sorry, I don't understand. (API response format unexpected)";
                    }

                    Log.d(TAG, "📤 Extracted reply (" + replyText.length() + " chars): " +
                            (replyText.length() > 100 ? replyText.substring(0, 100) + "..." : replyText));

                    final String finalReply = replyText;
                    ChatMessage botMessage = new ChatMessage("assistant", finalReply);
                    runOnUiThread(() -> {
                        messageList.add(botMessage);
                        adapter.notifyItemInserted(messageList.size() - 1);
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    });
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse JSON response: " + e.getMessage());
                    Log.e(TAG, "Raw response was: " + responseBody);
                    runOnUiThread(() -> {
                        ChatMessage errorMessage = new ChatMessage("system", "Error parsing response");
                        messageList.add(errorMessage);
                        adapter.notifyItemInserted(messageList.size() - 1);
                    });
                }
            }
        });
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
