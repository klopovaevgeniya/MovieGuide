package com.example.movieguide;

import java.io.Serializable;

public class Content implements Serializable {
    private String id;
    private String title;
    private String description;
    private String genre;
    private String imageUrl;
    private String trailerUrl;
    private String contentType;
    private long sumRatings;
    private long totalRatings;

    public Content() {
    }

    public Content(String title, String description, String genre, String imageUrl,
                   String trailerUrl, String contentType, long sumRatings, long totalRatings) {
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.imageUrl = imageUrl;
        this.trailerUrl = trailerUrl;
        this.contentType = contentType;
        this.sumRatings = sumRatings;
        this.totalRatings = totalRatings;
    }

    // Геттеры и сеттеры
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSumRatings() {
        return sumRatings;
    }

    public void setSumRatings(long sumRatings) {
        this.sumRatings = sumRatings;
    }

    public long getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(long totalRatings) {
        this.totalRatings = totalRatings;
    }

    public float getRating() {
        if (totalRatings == 0) return 0;
        return (float) sumRatings / totalRatings;
    }
}