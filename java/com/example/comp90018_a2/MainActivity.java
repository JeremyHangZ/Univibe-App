package com.example.comp90018_a2;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.comp90018_a2.services.LocationService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.comp90018_a2.fragments.*;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authListener;
    private static final int PERMISSION_REQUEST_CODE = 100;  // 请求码

    private final BroadcastReceiver proximityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("PROXIMITY_ALERT".equals(intent.getAction())) {
                String friendId = intent.getStringExtra("friendId");
                String friendName = intent.getStringExtra("friendName");
                double friendLatitude = intent.getDoubleExtra("friendLatitude", 0);
                double friendLongitude = intent.getDoubleExtra("friendLongitude", 0);
                String avatarUrl = intent.getStringExtra("avatarUrl");

                double userLatitude = intent.getDoubleExtra("userLatitude", 0);
                double userLongitude = intent.getDoubleExtra("userLongitude", 0);

                // 弹出提醒弹窗
                ProximityAlertDialogFragment dialog = new ProximityAlertDialogFragment(friendId, friendName, avatarUrl,friendLatitude,friendLongitude, userLatitude, userLongitude);
                dialog.show(getSupportFragmentManager(), "ProximityAlertDialogFragment");
            }
        }
    };

    // 设置用户在线状态
    private void setupUserOnlineStatus(FirebaseUser user){
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
        DatabaseReference onlineStatusRef = userRef.child("isOnline");

        onlineStatusRef.setValue(true);
        onlineStatusRef.onDisconnect().setValue(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // 创建 AuthStateListener 来监听登录状态变化
        authListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // 用户已登录
                Toast.makeText(MainActivity.this, "Welcome Back !" + user.getEmail(), Toast.LENGTH_SHORT).show();
                setupUserOnlineStatus(user);
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
                monitorShareLocation(userRef);

            } else {
                // 用户已登出，跳转到登录界面
                Toast.makeText(MainActivity.this, "Please login again :)", Toast.LENGTH_SHORT).show();
                finish();
            }
        };

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set default fragment
        loadFragment(new FriendFragment());



        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.nav_friends) {
                    selectedFragment = new FriendFragment();
                } else if (item.getItemId() == R.id.nav_map) {
                    selectedFragment = new MapFragment();
                } else if (item.getItemId() == R.id.nav_social) {
                    selectedFragment = new SocialFragment();
                } else if (item.getItemId() == R.id.nav_setting) {
                    selectedFragment = new SettingFragment();
                }

                return loadFragment(selectedFragment);
            }
        });

        // 检查并请求通知权限
        checkNotificationPermission();
    }

    // 检查通知权限
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // Android 13 及以上版本
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // 请求通知权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    //监控用户是否share location
    private void monitorShareLocation(DatabaseReference userRef){

        DatabaseReference shareLocationRef = userRef.child("isSharingLocation");

        shareLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isSharingLocation = snapshot.getValue(Boolean.class);
                if (isSharingLocation){
                    //start location service
                    Intent intent = new Intent(MainActivity.this, LocationService.class);
                    startService(intent);
                }else{
                    //stop location service
                    Intent stopServiceIntent = new Intent(MainActivity.this, LocationService.class);
                    stopService(stopServiceIntent);
                    //change user location to null
                    userRef.child("latitude").setValue(null);
                    userRef.child("longitude").setValue(null);
                    userRef.child("isInCampus").setValue(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了通知权限
                Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                // 用户拒绝了通知权限
                Toast.makeText(this, "通知权限被拒绝，无法发送通知", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authListener);  // 开始监听用户登录状态
    }

    @Override
    public void onStop() {
        super.onStop();

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);

        userRef.child("latitude").setValue(null);
        userRef.child("longitude").setValue(null);

        if (authListener != null) {
            mAuth.removeAuthStateListener(authListener);  // 停止监听用户登录状态
        }
        unregisterReceiver(proximityReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 注册广播接收器
        IntentFilter filter = new IntentFilter("PROXIMITY_ALERT");
        registerReceiver(proximityReceiver, filter);

        if (mAuth.getCurrentUser() != null){
            setupUserOnlineStatus(mAuth.getCurrentUser());
        }
    }
}
