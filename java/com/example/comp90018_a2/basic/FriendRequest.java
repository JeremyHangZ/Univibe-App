package com.example.comp90018_a2.basic;

public class FriendRequest {
    private String from;   // 发起请求的用户ID
    private String status; // 请求状态 ("pending", "accepted", "rejected")
    private String username; // 发起请求的用户名

    public FriendRequest() {
    }

    public FriendRequest(String from, String status, String username) {
        this.from = from;
        this.status = status;
        this.username = username;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
