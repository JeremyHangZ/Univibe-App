package com.example.comp90018_a2.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.example.comp90018_a2.R;
import com.example.comp90018_a2.fragments.ProximityAlertDialogFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LocationService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseReference;
    private DatabaseReference friendsLocationRef;
    private DatabaseReference usersRef;
    private String userId;
    private Set<String> notifiedFriends = new HashSet<>(); // 存储已发送消息的好友，避免重复提示

    private static final double MELBOURNE_UNI_LAT_NORTH = -37.796372;
    private static final double MELBOURNE_UNI_LAT_SOUTH = -37.802900;
    private static final double MELBOURNE_UNI_LNG_EAST = 144.964346;
    private static final double MELBOURNE_UNI_LNG_WEST = 144.956665;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("LocationService", "Service created");

        // 初始化 Firebase 引用
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        friendsLocationRef = FirebaseDatabase.getInstance().getReference("Friends").child(userId);
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // 创建通知渠道
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d("LocationService", "No user logged in, stopping service");
            stopSelf();  // 停止服务
            return START_NOT_STICKY;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, null);
        }

        startForeground(1, createForegroundNotification());

        return START_STICKY;
    }

    private LocationRequest createLocationRequest() {
        return new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)  // 设置最小更新间隔
                .setMaxUpdateDelayMillis(10000)    // 最大更新延迟
                .build();
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult != null) {
                for (Location location : locationResult.getLocations()) {
                    updateLocationToFirebase(location);
                    checkNearbyFriends(location);

                    // 别删，有用
//                    if (isInMelbourneUniversity(location)) {
//                        updateLocationToFirebase(location);
//                        checkNearbyFriends(location);
//                    } else {
//                        Map<String, Object> locationData = new HashMap<>();
//                        locationData.put("latitude", null);
//                        locationData.put("longitude", null);
//                        databaseReference.updateChildren(locationData);
//                    }
                }
            }
        }
    };

    // 更新用户位置到 Firebase
    private void updateLocationToFirebase(Location location) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());

        // isInCampus?
        boolean isInCampus = isInMelbourneUniversity(location);
        locationData.put("isInCampus", isInCampus);

        databaseReference.updateChildren(locationData);
    }

    private boolean isInMelbourneUniversity(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        return latitude >= MELBOURNE_UNI_LAT_SOUTH && latitude <= MELBOURNE_UNI_LAT_NORTH
                && longitude >= MELBOURNE_UNI_LNG_WEST && longitude <= MELBOURNE_UNI_LNG_EAST;
    }

    // 检查好友是否在100米范围内
    private void checkNearbyFriends(Location userLocation) {
        // 获取用户的好友列表和好友位置
        friendsLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot friendSnapshot : dataSnapshot.getChildren()) {
                    String friendId = friendSnapshot.getKey();

                    // 获取好友位置
                    usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Double friendLatitude = dataSnapshot.child("latitude").getValue(Double.class);
                            Double friendLongitude = dataSnapshot.child("longitude").getValue(Double.class);

                            if (friendLatitude == null || friendLongitude == null) {
                                Log.e("LocationService", "好友 " + friendId + " 缺少位置信息，无法计算距离。");
                                return;  // 如果任何一个值为空，跳过这个好友
                            }

                            // 计算两点之间的距离
                            float[] results = new float[1];
                            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                                    friendLatitude, friendLongitude, results);
                            float distanceInMeters = results[0];

                            // 如果距离小于100米，并且还没有发送过提示
                            if (distanceInMeters < 800 && !notifiedFriends.contains(friendId)) {
                                Log.d("LocationService", "找到附件好友");
                                sendProximityAlert(friendId, distanceInMeters, userLocation);  // 发送靠近提示
                                notifiedFriends.add(friendId); // 记录已通知的好友
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e("LocationService", "Failed to retrieve friend's location: " + databaseError.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("LocationService", "Failed to retrieve friends list: " + databaseError.getMessage());
            }
        });
    }

    // 创建通知渠道
    private void createNotificationChannel() {
        CharSequence name = "Proximity Alert Channel";
        String description = "Channel for proximity alerts";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel("ProximityChannelID", name, importance);
        channel.setDescription(description);

        // 注册通知渠道
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void sendProximityAlert(String friendId, float distance, Location userLocation) {
        // 获取好友名字并发送通知
        usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String friendName = snapshot.child("name").getValue(String.class);
                Double friendLatitude = snapshot.child("latitude").getValue(Double.class);
                Double friendLongitude = snapshot.child("longitude").getValue(Double.class);
                String avatarUrl = snapshot.child("avatar").getValue(String.class);

                // 检查权限并发送通知
                checkAndSendNotification(friendName, distance);

                // 发送广播通知当前 Activity 弹窗
                Intent intent = new Intent("PROXIMITY_ALERT");
                intent.putExtra("friendId", friendId);
                intent.putExtra("friendName", friendName);
                intent.putExtra("distance", distance);
                intent.putExtra("avatarUrl", avatarUrl);
                intent.putExtra("friendLatitude", friendLatitude);
                intent.putExtra("friendLongitude", friendLongitude);

                intent.putExtra("userLatitude", userLocation.getLatitude());
                intent.putExtra("userLongitude", userLocation.getLongitude());

                sendBroadcast(intent);  // 向应用的所有部分广播该消息

                saveProximityAlert(friendId, friendName, avatarUrl, friendLatitude, friendLongitude, userLocation);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("LocationService", "Failed to retrieve friend's name", error.toException());
            }
        });
    }

    // 在发送通知之前检查权限
    private void checkAndSendNotification(String friendName, float distance) {
        // 只有在 Android 13 及更高版本中需要请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // 权限没有被授予，可以根据需求向用户请求权限或者显示一个提示
                Log.d("LocationService", "通知权限未授予，无法发送通知。");
                return;  // 不发送通知
            }
        }

        // 如果权限已被授予，调用 sendProximityNotification
        sendProximityNotification(friendName, distance);
    }

    // 发出系统通知
    private void sendProximityNotification(String friendName, float distance) {
        Log.d("LocationService", "Sending proximity notification for " + friendName);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "ProximityChannelID")
                .setSmallIcon(R.drawable.ic_notification_close)
                .setContentTitle("Your friend is Close to you!")
                .setContentText("You are " + (int) distance + " meters away from "+ friendName + "!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("LocationService", "No notification permission");
                return;
            }
        }

        int notificationId = (int) System.currentTimeMillis();  // 动态生成通知ID
        notificationManager.notify(notificationId, builder.build());

        Log.d("LocationService", "Notification sent");
    }

    private void saveProximityAlert(String friendId, String friendName, String avatarUrl, Double latitude, Double longitude, Location userLoc) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference alertRef = FirebaseDatabase.getInstance().getReference("ProximityAlerts").child(currentUserId).push();

        Map<String, Object> alertData = new HashMap<>();
        alertData.put("friendId", friendId);
        alertData.put("friendName", friendName);
        alertData.put("avatarUrl", avatarUrl);
        alertData.put("friendLatitude", latitude);
        alertData.put("friendLongitude", longitude);

        alertData.put("userLatitude", userLoc.getLatitude());
        alertData.put("userLongitude", userLoc.getLongitude());

        alertRef.setValue(alertData);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("LocationService", "Task removed (user swiped away the app)");
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        Log.d("LocationService", "Location updates stopped.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "LocationServiceChannel",
                    "Location Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "LocationServiceChannel")
                .setContentTitle("Location Service Running")
                .setContentText("updating location...")
                .setSmallIcon(R.drawable.ic_notification_location)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return notificationBuilder.build();
    }

}
