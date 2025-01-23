package com.example.comp90018_a2.basic;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.comp90018_a2.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatMessage> messageList;

    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 获取当前的聊天消息
        ChatMessage message = messageList.get(position);

        // 设置消息文本内容
        holder.messageTextView.setText(message.getMessage());

        // 判断消息的发送者，并根据发送者调整消息的样式
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (message.getSenderId().equals(currentUserId)) {
            // 当前用户是发送者，消息对齐到右侧
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageContainer.getLayoutParams();
            params.gravity = Gravity.END;
            holder.messageContainer.setLayoutParams(params);
        } else {
            // 当前用户是接收者，消息对齐到左侧
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageContainer.getLayoutParams();
            params.gravity = Gravity.START;
            holder.messageContainer.setLayoutParams(params);
        }
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView messageTextView;
        public LinearLayout messageContainer;

        public ViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.textViewMessage);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }
    }
}

