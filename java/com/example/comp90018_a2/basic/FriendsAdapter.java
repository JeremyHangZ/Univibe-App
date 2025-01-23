package com.example.comp90018_a2.basic;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp90018_a2.ChatActivity;
import com.example.comp90018_a2.R;
import com.squareup.picasso.Picasso;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private List<User> friendList;
    private Context context;

    public FriendsAdapter(List<User> friendList) {
        this.friendList = friendList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = friendList.get(position);
        holder.nameTextView.setText(user.getName());

        holder.profileImageView.setImageResource(R.drawable.default_avatar);
        Picasso.get().cancelRequest(holder.profileImageView);

        // 使用 Picasso 加载头像
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            Picasso.get().cancelRequest(holder.profileImageView);

            Picasso.get()
                    .load(user.getAvatar())
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .fit()
                    .centerCrop()
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.default_avatar);
        }

        // 设置在线状态
        if (user.isOnline()) {
            holder.onlineStatus.setImageResource(R.drawable.ic_online_green_dot);
        } else {
            holder.onlineStatus.setImageResource(R.drawable.ic_offline_grey_dot);
        }

        // 设置在校状态
        if (user.isInCampus()) {
            holder.onCampusStatus.setImageResource(R.drawable.ic_in_school);
        } else {
            holder.onCampusStatus.setImageResource(R.drawable.ic_at_home);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("friendId", user.getUid());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    public void updateList(List<User> newList) {
        friendList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public ImageView profileImageView, onlineStatus, onCampusStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.friend_name);
            profileImageView = itemView.findViewById(R.id.friend_avatar);
            onlineStatus = itemView.findViewById(R.id.online_status);
            onCampusStatus = itemView.findViewById(R.id.onCampus_status);
        }
    }
}

