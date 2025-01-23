package com.example.comp90018_a2;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.comp90018_a2.basic.ChatAdapter;
import com.example.comp90018_a2.basic.ChatMessage;
import com.example.comp90018_a2.basic.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private DatabaseReference chatReference;
    private String friendId;

    private ImageView avatarImageView;
    private TextView nameTextView;
    private Button deleteButton;

    // 定义messageList
    private List<ChatMessage> messageList;
    private ChatAdapter chatAdapter; // 适配器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        friendId = getIntent().getStringExtra("friendId");
        chatReference = FirebaseDatabase.getInstance().getReference("Chats");

        recyclerView = findViewById(R.id.recyclerView_chat);
        messageEditText = findViewById(R.id.editTextMessage);
        sendButton = findViewById(R.id.buttonSend);

        // 初始化messageList和chatAdapter
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        avatarImageView = findViewById(R.id.friend_avatar);
        nameTextView = findViewById(R.id.friend_name);
        deleteButton = findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> showDeleteAccountConfirmationDialog());

        //获取好友信息
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(friendId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    nameTextView.setText(user.getName());
                    if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                        Picasso.get().load(user.getAvatar()).placeholder(R.drawable.default_avatar).into(avatarImageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });



        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // 显示返回键
            actionBar.setDisplayShowHomeEnabled(true); // 可选：显示图标
        }

        // 监听Firebase中的聊天记录
        loadMessages();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed(); // 处理返回按键
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 加载聊天记录
    private void loadMessages() {
        String chatId = getChatId(FirebaseAuth.getInstance().getCurrentUser().getUid(), friendId);
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(chatId);

        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();  // 清空旧的消息列表
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatMessage message = dataSnapshot.getValue(ChatMessage.class); // 获取消息
                    messageList.add(message);  // 添加到消息列表
                }
                chatAdapter.notifyDataSetChanged();  // 通知适配器更新UI

                if (!messageList.isEmpty()) {
                    recyclerView.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String message = messageEditText.getText().toString().trim();
        if (!message.isEmpty()) {
            String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String receiverId = friendId;
            String chatId = getChatId(senderId, receiverId);

            DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(chatId);

            HashMap<String, Object> messageData = new HashMap<>();
            messageData.put("senderId", senderId);
            messageData.put("receiverId", receiverId);
            messageData.put("message", message);
            messageData.put("timestamp", System.currentTimeMillis());

            chatRef.push().setValue(messageData);
            messageEditText.setText("");
        }
    }

    // 生成聊天ID，确保对话唯一
    private String getChatId(String senderId, String receiverId) {
        return senderId.compareTo(receiverId) > 0 ? senderId + "_" + receiverId : receiverId + "_" + senderId;
    }

    private void showDeleteAccountConfirmationDialog() {
        new AlertDialog.Builder(ChatActivity.this)
                .setTitle("Confirmation of friend deletion")
                .setMessage("Are you sure you want to delete this friend? This operation is not recoverable.")
                .setPositiveButton("YES", (dialog, which) -> handleDeleteFriend())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleDeleteFriend() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null || friendId == null) {
            Toast.makeText(ChatActivity.this, "Invalid user IDs", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference currentUserFriendsRef = FirebaseDatabase.getInstance().getReference("Friends")
                .child(currentUserId).child(friendId);
        DatabaseReference friendUserFriendsRef = FirebaseDatabase.getInstance().getReference("Friends")
                .child(friendId).child(currentUserId);

        currentUserFriendsRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                friendUserFriendsRef.removeValue().addOnCompleteListener(friendTask -> {
                    if (friendTask.isSuccessful()) {
                        finish();  // Operation successful
                    } else {
                        Toast.makeText(ChatActivity.this, "Failed to delete friend from friend's list", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(ChatActivity.this, "Failed to delete friend from user's list", Toast.LENGTH_SHORT).show();
            }
        });
    }

}