package com.example.movieguide;

public class History {
    private String userId;
    private String contentId;

    public History() {
    }

    public History(String userId, String contentId) {
        this.userId = userId;
        this.contentId = contentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }
}