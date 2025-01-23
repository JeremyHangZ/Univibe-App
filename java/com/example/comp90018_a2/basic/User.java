package com.example.comp90018_a2.basic;

import com.google.firebase.database.PropertyName;

public class User {

    private String uid;
    private String name;
    private String email;
    private String avatar;

    @PropertyName("isOnline")
    private boolean isOnline;

    @PropertyName("isInCampus")
    private boolean isInCampus;

    @PropertyName("isSharingLocation")
    private boolean isSharingLocation;

    // Firebase需要一个空构造函数
    public User() {
    }

    public User(String uid, String name, String email, boolean isOnline, boolean isInCampus, boolean isSharingLocation) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.isOnline = isOnline;
        this.isInCampus = isInCampus;
        this.isSharingLocation = isSharingLocation;
    }

    // Getter 和 Setter 方法
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    // 使用 @PropertyName 注解来确保 Firebase 正确地处理这些布尔值字段
    @PropertyName("isOnline")
    public boolean isOnline(){
        return isOnline;
    }

    @PropertyName("isOnline")
    public void setOnline(boolean onlineStatus){
        this.isOnline = onlineStatus;
    }

    @PropertyName("isInCampus")
    public boolean isInCampus() {
        return isInCampus;
    }

    @PropertyName("isInCampus")
    public void setInCampus(boolean inCampus) {
        this.isInCampus = inCampus;
    }

    @PropertyName("isSharingLocation")
    public boolean isSharingLocation() {
        return isSharingLocation;
    }

    @PropertyName("isSharingLocation")
    public void setSharingLocation(boolean sharingLocation) {
        this.isSharingLocation = sharingLocation;
    }
}
