package com.example.comp90018_a2.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.comp90018_a2.R;
import com.example.comp90018_a2.basic.FriendRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class ProfileDialogFragment extends DialogFragment {

    private String userId;
    private DatabaseReference userRef;
    private ImageView userAvatar;
    private TextView userNameTextView, userGenderTextView, userEmailTextView, userBirthdayTextView, userDegreeTextView;
    private TextView userDepartmentTextView, userYearTextView, userSignatureTextView;
    private LinearLayout lifePhotosContainer;
    private Button addFriendButton;

    public static ProfileDialogFragment newInstance(String userId) {
        ProfileDialogFragment fragment = new ProfileDialogFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_profile, container, false);

        // 初始化视图
        userAvatar = view.findViewById(R.id.user_avatar);
        userNameTextView = view.findViewById(R.id.user_name);
        userGenderTextView = view.findViewById(R.id.user_gender);
        userEmailTextView = view.findViewById(R.id.user_email);
        userBirthdayTextView = view.findViewById(R.id.user_birthday);
        userDegreeTextView = view.findViewById(R.id.user_degree);
        userDepartmentTextView = view.findViewById(R.id.user_department);
        userSignatureTextView = view.findViewById(R.id.user_signature);
        lifePhotosContainer = view.findViewById(R.id.life_photos_container);
        addFriendButton = view.findViewById(R.id.button_add_friend);

        userId = getArguments().getString("userId");

        // 从 Firebase 加载用户数据
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // 获取用户头像
                    String avatarUrl = snapshot.child("avatar").getValue(String.class);
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Picasso.get().load(avatarUrl).into(userAvatar);
                        userAvatar.setVisibility(View.VISIBLE);
                    }

                    // 获取并显示姓名
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null && !name.isEmpty()) {
                        userNameTextView.setText(name);
                        userNameTextView.setVisibility(View.VISIBLE);
                    }

                    // 获取并显示性别
                    String gender = snapshot.child("gender").getValue(String.class);
                    if (gender != null && !gender.isEmpty()) {
                        userGenderTextView.setText("Gender: " + gender);
                        userGenderTextView.setVisibility(View.VISIBLE);
                    }

                    // 获取并显示邮件
                    String email = snapshot.child("email").getValue(String.class);
                    if (email != null && !email.isEmpty()) {
                        userEmailTextView.setText("Email: " + email);
                        userEmailTextView.setVisibility(View.VISIBLE);
                    }

                    // 获取并显示生日 (检查是否为 Long 类型)
                    Object birthdayValue = snapshot.child("birthday").getValue();
                    if (birthdayValue != null) {
                        String birthday = birthdayValue instanceof Long ? String.valueOf(birthdayValue) : (String) birthdayValue;
                        userBirthdayTextView.setText("Birthday: " + birthday);
                        userBirthdayTextView.setVisibility(View.VISIBLE);
                    }

                    // 获取并显示学位
                    String degree = snapshot.child("degree").getValue(String.class);
                    if (degree != null && !degree.isEmpty()) {
                        userDegreeTextView.setText("Degree: " + degree);
                        userDegreeTextView.setVisibility(View.VISIBLE);
                    }

                    // 获取并显示院系
                    String department = snapshot.child("department").getValue(String.class);
                    if (department != null && !department.isEmpty()) {
                        userDepartmentTextView.setText("Department: " + department);
                        userDepartmentTextView.setVisibility(View.VISIBLE);
                    }

                    // 获取并显示个性签名
                    String signature = snapshot.child("signature").getValue(String.class);
                    if (signature != null && !signature.isEmpty()) {
                        userSignatureTextView.setText("个性签名: " + signature);
                        userSignatureTextView.setVisibility(View.VISIBLE);
                    }

                    // 获取并显示生活照片
                    for (DataSnapshot photoSnapshot : snapshot.child("photos").getChildren()) {
                        String photoUrl = photoSnapshot.getValue(String.class);
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            ImageView imageView = new ImageView(getContext());
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
                            params.setMargins(10, 0, 10, 0);
                            imageView.setLayoutParams(params);
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                            // 使用 Picasso 加载照片
                            Picasso.get().load(photoUrl).into(imageView);

                            // 将照片添加到容器中
                            lifePhotosContainer.addView(imageView);
                            lifePhotosContainer.setVisibility(View.VISIBLE); // 确保容器可见
                        }
                    }
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "加载用户数据失败", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置添加好友按钮的点击事件
        addFriendButton.setOnClickListener(v -> sendFriendRequest());

        return view;
    }

    private void sendFriendRequest() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
        DatabaseReference friendRequestsRef = FirebaseDatabase.getInstance().getReference("FriendRequests");

        // 获取当前用户的名字
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentUserName = snapshot.child("name").getValue(String.class);  // 获取用户名字

                    // 创建好友请求
                    String requestId = friendRequestsRef.child(userId).push().getKey();

                    Map<String, Object> friendRequest = new HashMap<>();
                    friendRequest.put("from", currentUserId);
                    friendRequest.put("username", currentUserName);
                    friendRequest.put("status", "pending");

                    // 将好友请求保存到数据库
                    friendRequestsRef.child(userId).child(requestId).setValue(friendRequest);

                    Toast.makeText(getContext(), "Friend Request Sent :)", Toast.LENGTH_SHORT).show();
                    dismiss();  // 关闭对话框
                } else {
                    Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to send friend request", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof SocialFragment) {
            ((SocialFragment) parentFragment).setDialogShowing(false);
        }
    }
}
