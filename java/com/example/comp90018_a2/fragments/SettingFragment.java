package com.example.comp90018_a2.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.comp90018_a2.DetailedInfoActivity;
import com.example.comp90018_a2.LoginActivity;
import com.example.comp90018_a2.R;
import com.example.comp90018_a2.TagActivity;
import com.example.comp90018_a2.services.LocationService;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

public class SettingFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private Button updateTagButton, editInfoButton, deleteAccountButton, logoutButton;
    private android.widget.ImageView avator;
    private android.widget.TextView name;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        // 初始化 FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        updateTagButton = view.findViewById(R.id.button_set_tags);
        editInfoButton = view.findViewById(R.id.button_change_info);
        deleteAccountButton = view.findViewById(R.id.button_delete_account);
        logoutButton = view.findViewById(R.id.button_logout);

        avator = view.findViewById(R.id.imageViewAvatar);
        name = view.findViewById(R.id.friend_name);

        updateTagButton.setOnClickListener(v -> updateTag());
        editInfoButton.setOnClickListener(v -> editPersonalInfo());
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountConfirmationDialog());
        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());

        SwitchMaterial visibilitySwitch = view.findViewById(R.id.visibility_switch);
        SwitchMaterial shareLocationSwitch = view.findViewById(R.id.share_location);


        // 获取当前用户的信息
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            DatabaseReference userRef = databaseRef.child("Users").child(user.getUid());
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DataSnapshot snapshot = task.getResult();
                    String userName = snapshot.child("name").getValue(String.class);
                    String userAvatar = snapshot.child("avatar").getValue(String.class);
                    Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);
                    Boolean isSharingLocation = snapshot.child("isSharingLocation").getValue(Boolean.class);

                    // 设置用户头像和昵称
                    if (userName != null) {
                        name.setText(userName);
                    }

                    if (userAvatar != null && !userAvatar.isEmpty()) {
                        Picasso.get()
                                .load(userAvatar)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .fit()
                                .centerCrop()
                                .into(avator);
                    } else {
                        avator.setImageResource(R.drawable.default_avatar);
                    }

                    visibilitySwitch.setChecked(isOnline != null ? isOnline : false);
                    shareLocationSwitch.setChecked(isSharingLocation != null ? isSharingLocation : false);
                }
            });
        }

        //monitoring the switch status and update with the database
        visibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> setUserOnlineStatus(isChecked));
        shareLocationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> setShareLocationSwitch(isChecked));
        return view;
    }

    private void updateTag(){
        Intent intent = new Intent(getActivity(), TagActivity.class);
        startActivity(intent);
    }

    private void editPersonalInfo(){
        Intent intent = new Intent(getActivity(), DetailedInfoActivity.class);
        startActivity(intent);
    }

    // 显示确认删除账号的对话框
    private void showDeleteAccountConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmation of account deletion")
                .setMessage("Are you sure you want to delete this account? This operation is not recoverable.")
                .setPositiveButton("YES", (dialog, which) -> deleteAccountAndData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm to logging out?")
                .setPositiveButton("YES", (dialog, which) -> signOut())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // 删除账号和相关数据
    private void deleteAccountAndData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            Intent intent = new Intent(getActivity(), LocationService.class);
            requireActivity().stopService(intent);

            // 删除用户数据
            deleteUserData(userId);

            // 删除用户账号
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (getActivity() != null) {
                            getActivity().finishAffinity();  // 关闭应用
                        }
                    }, 3000);  // 延迟3秒
                } else {
                    Toast.makeText(getContext(), "Unable to delete account, please try again", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // 删除与该用户相关的所有数据
    private void deleteUserData(String userId) {
        // 删除 Users 表中的用户信息
        databaseRef.child("Users").child(userId).removeValue();

        // 删除 Friends 表中的用户好友关系
        databaseRef.child("Friends").child(userId).removeValue();

        // 删除该用户的好友请求（发送和接收）
        databaseRef.child("FriendRequests").child(userId).removeValue();

        deleteChatData(userId);

        // 遍历 Friends 表，删除该用户作为好友出现在其他用户的数据
        databaseRef.child("Friends").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    snapshot.child(userId).getRef().removeValue();
                }
            }
        });
    }

    // 删除 Chat 表中的相关聊天记录
    private void deleteChatData(String userId) {
        DatabaseReference chatRef = databaseRef.child("Chats");

        // 遍历整个 Chats 表，检查与该用户相关的聊天记录
        chatRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DataSnapshot chatSnapshot : task.getResult().getChildren()) {
                    String chatId = chatSnapshot.getKey();
                    if (chatId != null && chatId.contains(userId)) {
                        // 删除与用户相关的聊天记录
                        chatRef.child(chatId).removeValue();
                    }
                }
            }
        });
    }

    private void signOut() {
        FirebaseUser user = mAuth.getCurrentUser();
        // 设置用户下线
        if (user != null) {
            DatabaseReference userRef = databaseRef.child("Users").child(user.getUid());
            DatabaseReference onlineStatusRef = userRef.child("isOnline");

            onlineStatusRef.setValue(false);
        }

        // 停止位置服务，确保服务在注销后不再运行
        Intent stopServiceIntent = new Intent(requireActivity(), LocationService.class);
        requireActivity().stopService(stopServiceIntent);

        // Firebase 退出登录
        mAuth.signOut();
        Toast.makeText(getContext(), "Logout Successful", Toast.LENGTH_SHORT).show();

        // 跳转回登录界面，清除任务栈，避免回到之前的活动
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // 结束当前活动，避免用户返回已登录状态
        requireActivity().finish();
    }

    private void setUserOnlineStatus(boolean isOnline) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = databaseRef.child("Users").child(user.getUid());
            DatabaseReference onlineStatusRef = userRef.child("isOnline");

            userRef.child("latitude").setValue(null);
            userRef.child("longitude").setValue(null);

            onlineStatusRef.setValue(isOnline);
        }
    }

    private void setShareLocationSwitch(boolean isSharingLocation) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = databaseRef.child("Users").child(user.getUid());
            DatabaseReference shareLocationRef = userRef.child("isSharingLocation");

            shareLocationRef.setValue(isSharingLocation);
        }
    }

}


