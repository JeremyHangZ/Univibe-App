package com.example.comp90018_a2.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.comp90018_a2.ChatActivity;
import com.example.comp90018_a2.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.squareup.picasso.Picasso;

public class ProximityAlertDialogFragment extends DialogFragment {

    private final String friendId;
    private final String friendName;
    private final String avatarUrl;
    private final double friendLatitude;
    private final double friendLongitude;
    private final double userLatitude;  // 用户的纬度
    private final double userLongitude;

    public ProximityAlertDialogFragment(String friendId, String friendName, String avatarUrl, double friendLatitude, double friendLongitude, double userLatitude, double userLongitude) {
        this.friendId = friendId;
        this.friendName = friendName;
        this.avatarUrl = avatarUrl;
        this.friendLatitude = friendLatitude;
        this.friendLongitude = friendLongitude;
        this.userLatitude = userLatitude;
        this.userLongitude = userLongitude;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_proximity_alert, container, false);

        TextView alertMessage = view.findViewById(R.id.friend_name);
        ImageView avatarImageView = view.findViewById(R.id.avatar_image_view);
        alertMessage.setText(friendName);

        Picasso.get()
                .load(avatarUrl)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(avatarImageView);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map_container, mapFragment).commit();
        }

        mapFragment.getMapAsync(googleMap -> {
            LatLng friendLocation = new LatLng(friendLatitude, friendLongitude);
            LatLng userLocation = new LatLng(userLatitude, userLongitude);

            googleMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location")).showInfoWindow();
            googleMap.addMarker(new MarkerOptions().position(friendLocation).title(friendName)).showInfoWindow();

            requireView().post(() -> {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(friendLocation);
                builder.include(userLocation);
                LatLngBounds bounds = builder.build();

                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            });
        });

        // 设置快捷操作按钮
        Button navigateButton = view.findViewById(R.id.navigate_button);
        navigateButton.setOnClickListener(v -> openGoogleMaps());

        Button messageButton = view.findViewById(R.id.message_button);
        messageButton.setOnClickListener(v -> sendMessageToFriend(friendId));

        ImageView closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void openGoogleMaps() {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + friendLatitude + "," + friendLongitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }

    private void sendMessageToFriend(String friendId) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("friendId", friendId);
        startActivity(intent);
    }
}

