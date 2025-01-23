package com.example.comp90018_a2.basic;

public class ChatMessage {
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;

    public ChatMessage() {}

    public ChatMessage(String senderId, String receiverId, String message, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}
