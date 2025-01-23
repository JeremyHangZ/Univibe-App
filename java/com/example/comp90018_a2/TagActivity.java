package com.example.comp90018_a2;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;  // 导入 ActionBar
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

public class TagActivity extends AppCompatActivity {
    private EditText editTextNewTag, editTextJoinTag;
    private Button buttonCreateTag, buttonJoinTag, buttonLeaveTag;
    private LinearLayout registeredTagsLayout, unregisteredTagsLayout;

    private DatabaseReference tagsRef, userRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_activity_tags);

        // 初始化视图
        editTextNewTag = findViewById(R.id.editTextNewTag);
        editTextJoinTag = findViewById(R.id.editTextJoinTag);
        buttonCreateTag = findViewById(R.id.buttonCreateTag);
        buttonJoinTag = findViewById(R.id.buttonJoinTag);
        buttonLeaveTag = findViewById(R.id.buttonLeaveTag);
        registeredTagsLayout = findViewById(R.id.linearLayoutRegisteredTags);
        unregisteredTagsLayout = findViewById(R.id.linearLayoutUnregisteredTags);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tagsRef = FirebaseDatabase.getInstance().getReference("Tags");
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);

        buttonCreateTag.setOnClickListener(v -> createNewTag());
        buttonJoinTag.setOnClickListener(v -> joinExistingTag());
        buttonLeaveTag.setOnClickListener(v -> leaveTag());

        loadUserTags();

        // 启用返回键
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // 显示返回键
            actionBar.setDisplayShowHomeEnabled(true); // 可选：显示图标
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed(); // 处理返回按键
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createNewTag() {
        String newTag = editTextNewTag.getText().toString().trim();
        if (!newTag.isEmpty()) {
            tagsRef.child(newTag).child(currentUserId).setValue(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // 在用户的 tags 中添加新创建的标签
                    userRef.child("tags").child(newTag).setValue(true);
                    Toast.makeText(TagActivity.this, "Successfully create tag", Toast.LENGTH_SHORT).show();
                    loadUserTags(); // 重新加载标签
                } else {
                    Toast.makeText(TagActivity.this, "Tag creation failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Please Enter Tag Name :)", Toast.LENGTH_SHORT).show();
        }
    }

    private void joinExistingTag() {
        String tagToJoin = editTextJoinTag.getText().toString().trim();

        if (!tagToJoin.isEmpty()) {
            tagsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean tagFound = false;
                    String matchedTagName = null;

                    for (DataSnapshot tagSnapshot : snapshot.getChildren()) {
                        String existingTagName = tagSnapshot.getKey();

                        if (existingTagName != null && existingTagName.equalsIgnoreCase(tagToJoin)) {
                            tagFound = true;
                            matchedTagName = existingTagName;  // 保留原始大小写的标签名
                            break;
                        }
                    }

                    if (tagFound) {
                        String finalMatchedTagName = matchedTagName;
                        tagsRef.child(matchedTagName).child(currentUserId).setValue(true)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        userRef.child("tags").child(finalMatchedTagName).setValue(true);
                                        Toast.makeText(TagActivity.this, "Success: Joined tag '" + finalMatchedTagName + "'", Toast.LENGTH_SHORT).show();
                                        loadUserTags();
                                    } else {
                                        Toast.makeText(TagActivity.this, "Failed: Tag registration", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(TagActivity.this, "Tag not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(TagActivity.this, "Error: Unable to check tags", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Please enter a tag name", Toast.LENGTH_SHORT).show();
        }
    }

    private void leaveTag() {
        String tagToLeave = editTextJoinTag.getText().toString().trim();

        if (!tagToLeave.isEmpty()) {
            // 获取所有标签，忽略大小写比较
            tagsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean tagFound = false;
                    String matchedTagName = null;

                    for (DataSnapshot tagSnapshot : snapshot.getChildren()) {
                        String existingTagName = tagSnapshot.getKey();

                        if (existingTagName != null && existingTagName.equalsIgnoreCase(tagToLeave)) {
                            tagFound = true;
                            matchedTagName = existingTagName;
                            break;
                        }
                    }

                    if (tagFound) {
                        // 检查用户是否已注册该标签
                        String finalMatchedTagName = matchedTagName;
                        tagsRef.child(matchedTagName).child(currentUserId).get().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult().exists()) {
                                // 用户已注册该标签，移除用户
                                tagsRef.child(finalMatchedTagName).child(currentUserId).removeValue();
                                userRef.child("tags").child(finalMatchedTagName).removeValue();
                                Toast.makeText(TagActivity.this, "Success: Left tag '" + finalMatchedTagName + "'", Toast.LENGTH_SHORT).show();
                                loadUserTags();
                            } else {
                                Toast.makeText(TagActivity.this, "You are not registered to this tag", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // 未找到匹配的标签，提醒用户
                        Toast.makeText(TagActivity.this, "Tag not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(TagActivity.this, "Error: Unable to check tags", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Please enter a tag name", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserTags() {
        userRef.child("tags").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 清空布局
                registeredTagsLayout.removeAllViews();
                unregisteredTagsLayout.removeAllViews();

                // 获取用户已注册的标签
                Set<String> registeredTags = new HashSet<>();
                for (DataSnapshot tagSnapshot : snapshot.getChildren()) {
                    registeredTags.add(tagSnapshot.getKey());
                }

                displayRegisteredTags(registeredTags);
                displayUnregisteredTags(registeredTags);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TagActivity.this, "加载标签失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 显示用户已注册的标签
    private void displayRegisteredTags(Set<String> registeredTags) {
        for (String tag : registeredTags) {
            TextView tagTextView = new TextView(this);
            tagTextView.setText(getString(R.string.tag_with_bullet, tag));
            tagTextView.setTextSize(15);
            tagTextView.setTextColor(getColor(R.color.light_black));
            registeredTagsLayout.addView(tagTextView);
        }
    }

    // 显示用户未注册的标签
    private void displayUnregisteredTags(Set<String> registeredTags) {
        tagsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot tagSnapshot : snapshot.getChildren()) {
                    String tagName = tagSnapshot.getKey();
                    if (!registeredTags.contains(tagName)) {
                        TextView tagTextView = new TextView(TagActivity.this);
                        tagTextView.setText(getString(R.string.tag_with_bullet, tagName));
                        tagTextView.setTextSize(15);
                        tagTextView.setTextColor(getColor(R.color.light_black));
                        unregisteredTagsLayout.addView(tagTextView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TagActivity.this, "Loading Unregistered Tags Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

