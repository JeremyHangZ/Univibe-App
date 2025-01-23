package com.example.comp90018_a2.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.comp90018_a2.R;
import com.example.comp90018_a2.basic.FriendRequest;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class SocialFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private float currentAcceleration;  // 当前加速度
    private float lastAcceleration; // 上一次的加速度
    private float shake;    // 摇动幅度
    private long lastShakeTime = 0; // 记录上次摇动的时间

    private EditText addFriendEmailEditText;
    private Button addFriendButton;
    private Button viewFriendRequestsButton;
    private ScrollView friendRequestsContainer;
    private boolean isDialogShowing = false;

    private DatabaseReference usersRef, tagsRef, friendRequestsRef, friendsRef;

    private static final long SHAKE_THRESHOLD_TIME = 2000; // 2秒

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_social, container, false);

        // 初始化 Firebase 引用
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        friendRequestsRef = FirebaseDatabase.getInstance().getReference("FriendRequests");
        friendsRef = FirebaseDatabase.getInstance().getReference("Friends");
        tagsRef = FirebaseDatabase.getInstance().getReference("Tags");  // Tags 引用

        // 初始化视图组件
        Spinner tagSpinner = view.findViewById(R.id.tag_spinner);
        TextView tagUserCountTextView = view.findViewById(R.id.tag_user_count);
        ImageView shakeImage = view.findViewById(R.id.shake_image);

        addFriendEmailEditText = view.findViewById(R.id.add_friend_email);
        addFriendButton = view.findViewById(R.id.button_add_friend);
        viewFriendRequestsButton = view.findViewById(R.id.button_view_friend_requests);
        friendRequestsContainer = view.findViewById(R.id.friend_requests_container);

        // 处理添加好友按钮点击
        addFriendButton.setOnClickListener(v -> addFriendByEmail());

        // 处理查看好友请求按钮点击
        viewFriendRequestsButton.setOnClickListener(v -> loadFriendRequests());

        // 初始化传感器
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        // 初始化加速度值
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;
        shake = 0.00f;

        // 加载并显示所有标签
        loadTagsIntoSpinner(tagsRef, tagSpinner, tagUserCountTextView);

        // 当用户选择一个标签时，更新注册人数
        tagSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTag = tagSpinner.getSelectedItem().toString();
                updateTagUserCount(tagsRef, selectedTag, tagUserCountTextView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        return view;
    }

    // 禁止在弹窗显示时摇一摇
    public void setDialogShowing(boolean showing) {
        isDialogShowing = showing;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isDialogShowing) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        lastAcceleration = currentAcceleration;
        currentAcceleration = (float) Math.sqrt((double) (x * x + y * y + z * z));
        float delta = currentAcceleration - lastAcceleration;
        shake = shake * 0.9f + delta;  // 低通滤波器

        long currentTime = System.currentTimeMillis();

        // 如果检测到摇动动作，且距离上次摇动时间超过阈值
        if (shake > 8 && (currentTime - lastShakeTime > SHAKE_THRESHOLD_TIME)) {
            lastShakeTime = currentTime;  // 更新上次摇动时间
            findRandomUser();  // 调用匹配用户函数
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 不需要实现
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    // 加载所有标签到 Spinner
    private void loadTagsIntoSpinner(DatabaseReference tagsRef, Spinner tagSpinner, TextView tagUserCountTextView) {
        tagsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    return;
                }

                List<String> tags = new ArrayList<>();
                tags.add("null");

                for (DataSnapshot tagSnapshot : snapshot.getChildren()) {
                    tags.add(tagSnapshot.getKey());
                }

                // 将标签加载到 Spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, tags);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                tagSpinner.setAdapter(adapter);

                // 默认显示 "null"，人数为 0
                tagUserCountTextView.setText("number of head count: 0");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "无法加载标签", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTagUserCount(DatabaseReference tagsRef, String selectedTag, TextView tagUserCountTextView) {
        if (selectedTag.equals("null")) {
            // 如果选择的是 null，获取所有用户的数量
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long userCount = snapshot.getChildrenCount();
                    tagUserCountTextView.setText("Tag head count: " + userCount);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "无法获取用户数量", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // 获取该标签下注册的用户数量
            tagsRef.child(selectedTag).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long userCount = snapshot.getChildrenCount();
                    tagUserCountTextView.setText("Tag head count: " + userCount);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "无法获取注册人数", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    // 随机从数据库中匹配用户
    private void findRandomUser() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("Friends").child(currentUserId);

        // 获取 Spinner 选中的标签
        String selectedTag = ((Spinner) getView().findViewById(R.id.tag_spinner)).getSelectedItem().toString();

        // 获取当前用户的好友列表
        friendsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot friendsSnapshot = task.getResult();
                Set<String> friendIds = new HashSet<>();
                for (DataSnapshot snapshot : friendsSnapshot.getChildren()) {
                    friendIds.add(snapshot.getKey());
                }

                // 将当前用户也加入排除列表，避免随机到自己
                friendIds.add(currentUserId);

                // 根据选择的标签筛选用户
                if (selectedTag.equals("null")) {
                    // 不指定标签，推荐所有非好友用户
                    recommendUserWithoutTag(friendIds);
                } else {
                    // 按照标签筛选用户
                    recommendUserWithTag(friendIds, selectedTag);
                }
            } else {
                Toast.makeText(getContext(), "获取好友列表失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recommendUserWithoutTag(Set<String> friendIds) {
        usersRef.get().addOnCompleteListener(usersTask -> {
            if (usersTask.isSuccessful()) {
                DataSnapshot usersSnapshot = usersTask.getResult();
                List<DataSnapshot> candidates = new ArrayList<>();

                for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                    if (!friendIds.contains(userSnapshot.getKey())) {
                        candidates.add(userSnapshot);
                    }
                }

                recommendRandomUser(candidates);
            }
        });
    }

    private void recommendUserWithTag(Set<String> friendIds, String selectedTag) {
        DatabaseReference tagUsersRef = tagsRef.child(selectedTag); // 获取标签下的用户
        tagUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot tagUsersSnapshot) {
                List<String> tagUserIds = new ArrayList<>();

                // 获取该标签下所有用户ID，过滤掉好友
                for (DataSnapshot tagUserSnapshot : tagUsersSnapshot.getChildren()) {
                    String userId = tagUserSnapshot.getKey();
                    if (!friendIds.contains(userId)) {
                        tagUserIds.add(userId);  // 将不是好友的用户加入候选人列表
                    }
                }

                // 查询这些用户的详细信息
                if (!tagUserIds.isEmpty()) {
                    List<DataSnapshot> candidates = new ArrayList<>();
                    for (String userId : tagUserIds) {
                        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                candidates.add(userSnapshot);

                                // 确保在最后一个用户数据加载完毕后调用推荐函数
                                if (candidates.size() == tagUserIds.size()) {
                                    recommendRandomUser(candidates);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    // 没有符合条件的用户
                    Toast.makeText(getContext(), "No users available for this tag", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load tag users", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void recommendRandomUser(List<DataSnapshot> candidates) {
        if (!isAdded()) {
            return;
        }

        if (candidates.isEmpty()) {
            Toast.makeText(requireContext(), "No random friend available now", Toast.LENGTH_SHORT).show();
        } else {
            Random random = new Random();
            int randomIndex = random.nextInt(candidates.size());
            DataSnapshot randomUserSnapshot = candidates.get(randomIndex);

            String userName = randomUserSnapshot.child("name").getValue(String.class);
            Toast.makeText(requireContext(), "Matched a potential friend: " + userName, Toast.LENGTH_SHORT).show();

            // 显示用户资料的弹窗
            String randomUserId = randomUserSnapshot.getKey();
            ProfileDialogFragment profileDialog = ProfileDialogFragment.newInstance(randomUserId);

            // 设置弹窗显示标志为 true
            isDialogShowing = true;
            profileDialog.show(getChildFragmentManager(), "ProfileDialog");
        }
    }

    // 通过邮箱添加好友
    private void addFriendByEmail() {
        String email = addFriendEmailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(getContext(), "Please enter email :)", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("Friends").child(currentUserId);

        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String currentUserName = snapshot.child("name").getValue(String.class);
                    addFriend(currentUserId, currentUserName, email, friendsRef);
                }else{
                    Toast.makeText(getContext(), "无法获取用户名", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "无法获取用户名", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addFriend(String currentUserId, String currentUserName, String email, DatabaseReference friendsRef) {
        Log.d("SocialFragment", "查询邮箱：" + email);
        Log.d("SocialFragment", "当前用户名：" + currentUserName);
        // 先通过邮箱查询用户
        usersRef.orderByChild("email").equalTo(email).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    for (DataSnapshot dataSnapshot : task.getResult().getChildren()) {
                        String friendId = dataSnapshot.getKey();

                        // 检查该用户是否已经是好友
                        friendsRef.child(friendId).get().addOnCompleteListener(friendCheckTask -> {
                            if (friendCheckTask.isSuccessful()) {
                                if (friendCheckTask.getResult().exists()) {
                                    // 用户已经是好友
                                    Toast.makeText(getContext(), "The user is your already friend", Toast.LENGTH_SHORT).show();
                                } else {
                                    // 在 FriendRequests 表中添加请求
                                    String requestId = friendRequestsRef.child(friendId).push().getKey();
                                    friendRequestsRef.child(friendId).child(requestId).setValue(new FriendRequest(currentUserId, "pending", currentUserName));
                                    Toast.makeText(getContext(), "Friend request sent :)", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getContext(), "检查好友状态失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(getContext(), "Email not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "查询失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFriendRequests() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        LinearLayout friendRequestsLayout = getView().findViewById(R.id.friend_requests_layout);

        friendRequestsRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) {
                    return;
                }

                friendRequestsLayout.removeAllViews();

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String requestId = requestSnapshot.getKey();
                    FriendRequest request = requestSnapshot.getValue(FriendRequest.class);

                    TextView requestView = new TextView(getContext());
                    requestView.setText(String.format("Friend request from: %s", request.getUsername()));
                    friendRequestsLayout.addView(requestView);

                    // 接受按钮
                    Button acceptButton = new Button(getContext());
                    acceptButton.setText("Accept");
                    acceptButton.setOnClickListener(v -> handleFriendRequest(requestId, request.getFrom(), true));
                    friendRequestsLayout.addView(acceptButton);

                    // 拒绝按钮
                    Button declineButton = new Button(getContext());
                    declineButton.setText("Reject");
                    declineButton.setOnClickListener(v -> handleFriendRequest(requestId, request.getFrom(), false));
                    friendRequestsLayout.addView(declineButton);
                }

                friendRequestsContainer.setVisibility(View.VISIBLE);  // 显示 ScrollView
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "无法加载好友请求", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void handleFriendRequest(String requestId, String fromUserId, boolean accepted) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (accepted) {
            // 添加到好友表中
            friendsRef.child(currentUserId).child(fromUserId).setValue(true);
            friendsRef.child(fromUserId).child(currentUserId).setValue(true);
            Toast.makeText(getContext(), "Accept friend request", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Reject friend request", Toast.LENGTH_SHORT).show();
        }

        // 移除好友请求
        friendRequestsRef.child(currentUserId).child(requestId).removeValue();
    }
}
