package models;

import java.util.Date;

public class Review {
    private int id;
    private String carBrand;
    private String carModel;
    private String author;
    private String platform;
    private String content;
    private int rating;
    private Date reviewDate;
    private String sourceUrl;

    public Review() {}

    public Review(int id, String carBrand, String carModel, String author,
                  String platform, String content, int rating, Date reviewDate, String sourceUrl) {
        this.id = id; this.carBrand = carBrand; this.carModel = carModel;
        this.author = author; this.platform = platform; this.content = content;
        this.rating = rating; this.reviewDate = reviewDate; this.sourceUrl = sourceUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCarBrand() { return carBrand; }
    public void setCarBrand(String carBrand) { this.carBrand = carBrand; }
    public String getCarModel() { return carModel; }
    public void setCarModel(String carModel) { this.carModel = carModel; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public Date getReviewDate() { return reviewDate; }
    public void setReviewDate(Date reviewDate) { this.reviewDate = reviewDate; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getRatingStars() {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }

    @Override
    public String toString() { return author + " (" + platform + ") - " + getRatingStars(); }
}
