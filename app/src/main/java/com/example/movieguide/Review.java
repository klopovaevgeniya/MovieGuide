package com.example.movieguide;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

public class Review {
    private String id;
    private String contentId;
    private String userName;
    private String userId;
    private String reviewText;
    private float rating;
    @ServerTimestamp
    private Timestamp timestamp;

    public Review() {
    }

    public Review(String id, String contentId, String userId, String userName, String reviewText, float rating) {
        this.id = id;
        this.contentId = contentId;
        this.userId = userId;
        this.userName = userName;
        this.reviewText = reviewText;
        this.rating = rating;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}