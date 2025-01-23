package com.example.comp90018_a2.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.comp90018_a2.MainActivity;
import com.example.comp90018_a2.R;
import com.example.comp90018_a2.services.LocationService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";
    private GoogleMap mMap;
    private DatabaseReference usersLocationRef;
    private ValueEventListener valueEventListener;

    // 定义用于权限请求的 Launcher
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // 如果权限被授予，启用用户位置功能
                    enableUserLocation();
                } else {
                    // 否则，显示一条消息
                    Toast.makeText(getContext(), "位置权限被拒绝", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // 初始化地图
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // 设置墨尔本大学的地理坐标范围
        LatLngBounds melbourneUniBounds = new LatLngBounds(
                new LatLng(-37.802900, 144.956665), // 墨尔本大学的一个坐标
                new LatLng(-37.796372, 144.964346)  // 墨尔本大学的另一个坐标
        );

        // 将地图中心设为墨尔本大学，并设置缩放级别
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(melbourneUniBounds, 100));

        // 检查并请求位置权限
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        } else {
            // 请求位置权限
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // 从 Firebase 获取所有用户的位置
        usersLocationRef = FirebaseDatabase.getInstance().getReference("Users");
        valueEventListener = usersLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mMap.clear();  // 清除旧的标记
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // 获取latitude和longitude，并进行null检查
                    Double latitude = dataSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = dataSnapshot.child("longitude").getValue(Double.class);
                    String avatarUrl = dataSnapshot.child("avatar").getValue(String.class);

                    if (latitude != null && longitude != null) {
                        LatLng userLocation = new LatLng(latitude, longitude);
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            // 加载头像并设置Marker
                            loadAvatarAndSetMarker(avatarUrl, userLocation);
                        } else {
                            setDefaultMarker(userLocation);
                        }
                    } else {
                        Log.e(TAG, "Missing location data for user: " + dataSnapshot.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase database error: " + error.getMessage());
            }
        });
    }

    // 使用Picasso加载头像并设置Marker
    private void loadAvatarAndSetMarker(String avatarUrl, LatLng userLocation) {
        // 使用Picasso加载头像
        Picasso.get().load(avatarUrl).resize(100, 100).into(new com.squareup.picasso.Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                // 将头像Bitmap设置为Marker
                setMarkerWithAvatar(bitmap, userLocation);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Log.e(TAG, "Failed to load avatar: " + e.getMessage());
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                // 可以在这里显示占位图像（可选）
            }
        });
    }

    private void setDefaultMarker(LatLng userLocation) {
        if (!isAdded()) {
            Log.e(TAG, "Fragment not attached to context. Skipping marker update.");
            return;
        }

        // 尝试将默认头像Drawable转换为Bitmap
        Bitmap defaultAvatarBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.robot);

        if (defaultAvatarBitmap != null) {
            Bitmap resizedAvatar = Bitmap.createScaledBitmap(defaultAvatarBitmap, 100, 100, false);

            // 如果Bitmap加载成功，设置为Marker图标
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(userLocation)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizedAvatar));  // 设置默认头像为Marker图标

            // 在地图上添加Marker
            mMap.addMarker(markerOptions);
        } else {
            // 如果Bitmap为null，使用默认的Marker颜色
            Log.e(TAG, "Failed to load default avatar. Using default marker color.");
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(userLocation)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));  // 使用默认的Marker颜色

            // 在地图上添加Marker
            mMap.addMarker(markerOptions);
        }
    }


    private void setMarkerWithAvatar(Bitmap avatarBitmap, LatLng userLocation) {
        // 将头像的Bitmap缩小到合适的大小
        Bitmap resizedAvatar = Bitmap.createScaledBitmap(avatarBitmap, 100, 100, false);

        // 创建Marker并设置自定义头像图标
        MarkerOptions markerOptions = new MarkerOptions()
                .position(userLocation)
                .icon(BitmapDescriptorFactory.fromBitmap(resizedAvatar));  // 设置头像为Marker图标

        // 在地图上添加Marker
        mMap.addMarker(markerOptions);
    }

    // 启用用户位置追踪
    private void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "No user logged in, not updating map.");
            return;  // 如果没有用户登录，不加载地图数据
        }

        // 先检查 mMap 是否已经初始化
        if (mMap != null) {
            LatLngBounds melbourneUniBounds = new LatLngBounds(
                    new LatLng(-37.802900, 144.956665),
                    new LatLng(-37.796372, 144.964346)
            );
            // 设置地图摄像机
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(melbourneUniBounds, 100));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usersLocationRef != null) {
            usersLocationRef.removeEventListener(valueEventListener);
        }
    }

}