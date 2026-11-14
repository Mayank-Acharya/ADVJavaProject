package com.smartats.model;

import java.sql.Timestamp;

public class Notification {
    private int notifId;
    private int userId;
    private String title;
    private String message;
    private boolean isRead;
    private Timestamp createdAt;

    public Notification() {}

    public Notification(int userId, String title, String message) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.isRead = false;
    }

    public int getNotifId() {
        return notifId;
    }

    public void setNotifId(int notifId) {
        this.notifId = notifId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
