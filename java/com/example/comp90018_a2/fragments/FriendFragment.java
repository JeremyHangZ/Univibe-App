package com.example.comp90018_a2.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp90018_a2.R;
import com.example.comp90018_a2.basic.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class FriendFragment extends Fragment {

    // 定义 friendList 列表
    private List<User> friendList;
    private RecyclerView recyclerView;
    private FriendsAdapter friendsAdapter;
    private DatabaseReference databaseReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend, container, false);

        // 初始化 RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView_friends);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 初始化好友列表和适配器
        friendList = new ArrayList<>();
        friendsAdapter = new FriendsAdapter(friendList);
        recyclerView.setAdapter(friendsAdapter);

        // 获取 Firebase Database 中的好友节点
        databaseReference = FirebaseDatabase.getInstance().getReference("Friends");
        loadFriends();

        // 搜索好友功能
        EditText searchFriend = view.findViewById(R.id.search_friend);
        searchFriend.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchFriends(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadFriends() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendList.clear(); // 清空列表，避免重复加载
                List<String> friendIds = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    friendIds.add(dataSnapshot.getKey());
                }

                // 加载好友详情并监听状态变化
                loadAllFriendDetails(friendIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAllFriendDetails(List<String> friendIds) {
        friendList.clear();

        for (String friendId : friendIds) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(friendId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User friend = snapshot.getValue(User.class);
                    if (friend != null) {
                        friendList.add(friend);
                        listenToFriendStatus(friendId, friend);
                    }


                    if (friendList.size() == friendIds.size()) {
                        sortFriendList();
                        friendsAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void sortFriendList() {
        friendList.sort((user1, user2) -> {
            if (user1.isInCampus() && !user2.isInCampus()) {
                return -1;
            } else if (!user1.isInCampus() && user2.isInCampus()) {
                return 1;
            }

            if (user1.isOnline() && !user2.isOnline()) {
                return -1;
            } else if (!user1.isOnline() && user2.isOnline()) {
                return 1;
            }

            String name1 = user1.getName() == null ? "" : user1.getName();
            String name2 = user2.getName() == null ? "" : user2.getName();
            return name1.compareTo(name2);
        });
    }


    private void listenToFriendStatus(String friendId, User friend) {
        DatabaseReference onlineRef = FirebaseDatabase.getInstance().getReference("Users").child(friendId).child("isOnline");
        onlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isOnline = snapshot.getValue(Boolean.class);
                    friend.setOnline(isOnline);
                    updateFriendStatusInAdapter(friendId);  // 更新用户状态
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        DatabaseReference inCampusRef = FirebaseDatabase.getInstance().getReference("Users").child(friendId).child("isInCampus");
        inCampusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isInCampus = snapshot.getValue(Boolean.class);
                    friend.setInCampus(isInCampus);
                    updateFriendStatusInAdapter(friendId);  // 更新用户状态
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void updateFriendStatusInAdapter(String friendId) {
        for (int i = 0; i < friendList.size(); i++) {
            if (friendList.get(i).getUid().equals(friendId)) {
                // 找到目标用户，更新状态
                User updatedUser = friendList.get(i);

                // 更新状态逻辑，确保好友的在线和在校状态更新正确
                // 例如，可以重新设置在线或在校状态：
                updatedUser.setOnline(updatedUser.isOnline()); // 如果是在线状态
                updatedUser.setInCampus(updatedUser.isInCampus()); // 如果是校内状态

                // 替换更新后的用户信息
                friendList.set(i, updatedUser);

                // 进行排序并通知适配器更新
                sortFriendList();
                friendsAdapter.notifyDataSetChanged();
                break;
            }
        }
    }


    private void searchFriends(String query) {
        List<User> filteredList = new ArrayList<>();
        for (User user : friendList) {
            if (user.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(user);
            }
        }
        friendsAdapter.updateList(filteredList);
    }
}
