package com.example.movieguide;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@IgnoreExtraProperties
public class Stats implements Parcelable {
    private String id;
    private String contentId;
    private String contentTitle;
    private String date;
    private int rating1Count;
    private int rating2Count;
    private int rating3Count;
    private int rating4Count;
    private int rating5Count;

    public Stats() {
    }

    public Stats(String contentId, String contentTitle) {
        this.contentId = contentId;
        this.contentTitle = contentTitle;
        this.date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
        this.rating1Count = 0;
        this.rating2Count = 0;
        this.rating3Count = 0;
        this.rating4Count = 0;
        this.rating5Count = 0;
    }

    protected Stats(Parcel in) {
        id = in.readString();
        contentId = in.readString();
        contentTitle = in.readString();
        date = in.readString();
        rating1Count = in.readInt();
        rating2Count = in.readInt();
        rating3Count = in.readInt();
        rating4Count = in.readInt();
        rating5Count = in.readInt();
    }

    public static final Creator<Stats> CREATOR = new Creator<Stats>() {
        @Override
        public Stats createFromParcel(Parcel in) {
            return new Stats(in);
        }

        @Override
        public Stats[] newArray(int size) {
            return new Stats[size];
        }
    };

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentTitle() {
        return contentTitle;
    }

    public void setContentTitle(String contentTitle) {
        this.contentTitle = contentTitle;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getRating1Count() {
        return rating1Count;
    }

    public void setRating1Count(int rating1Count) {
        this.rating1Count = rating1Count;
    }

    public int getRating2Count() {
        return rating2Count;
    }

    public void setRating2Count(int rating2Count) {
        this.rating2Count = rating2Count;
    }

    public int getRating3Count() {
        return rating3Count;
    }

    public void setRating3Count(int rating3Count) {
        this.rating3Count = rating3Count;
    }

    public int getRating4Count() {
        return rating4Count;
    }

    public void setRating4Count(int rating4Count) {
        this.rating4Count = rating4Count;
    }

    public int getRating5Count() {
        return rating5Count;
    }

    public void setRating5Count(int rating5Count) {
        this.rating5Count = rating5Count;
    }

    public void addRating(int rating) {
        switch (rating) {
            case 1:
                rating1Count++;
                break;
            case 2:
                rating2Count++;
                break;
            case 3:
                rating3Count++;
                break;
            case 4:
                rating4Count++;
                break;
            case 5:
                rating5Count++;
                break;
        }
        this.date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
    }

    @Exclude
    public int getTotalRatings() {
        return rating1Count + rating2Count + rating3Count + rating4Count + rating5Count;
    }

    @Exclude
    public float getAverageRating() {
        int total = getTotalRatings();
        if (total == 0) return 0;
        return (rating1Count * 1f + rating2Count * 2f + rating3Count * 3f +
                rating4Count * 4f + rating5Count * 5f) / total;
    }

    @Exclude
    public List<Float> getRatingDistribution() {
        int total = getTotalRatings();
        if (total == 0) return Arrays.asList(0f, 0f, 0f, 0f, 0f);

        return Arrays.asList(
                rating1Count * 100f / total,
                rating2Count * 100f / total,
                rating3Count * 100f / total,
                rating4Count * 100f / total,
                rating5Count * 100f / total
        );
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(contentId);
        dest.writeString(contentTitle);
        dest.writeString(date);
        dest.writeInt(rating1Count);
        dest.writeInt(rating2Count);
        dest.writeInt(rating3Count);
        dest.writeInt(rating4Count);
        dest.writeInt(rating5Count);
    }
}