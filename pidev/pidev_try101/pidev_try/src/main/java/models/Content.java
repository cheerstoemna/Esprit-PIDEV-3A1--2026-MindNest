package models;

import java.sql.Timestamp;

public class Content {
    private int id;
    private String title;
    private String description;
    private String type;
    private String sourceUrl;
    private String imageUrl;
    private String category;
    private Timestamp createdAt;
    private int upvotes;
    private int downvotes;

    public Content() {}

    public Content(String title, String description, String type, String sourceUrl, String imageUrl, String category) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.sourceUrl = sourceUrl;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public Content(int id, String title, String description, String type, String sourceUrl,
                   String imageUrl, String category, Timestamp createdAt, int upvotes, int downvotes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.sourceUrl = sourceUrl;
        this.imageUrl = imageUrl;
        this.category = category;
        this.createdAt = createdAt;
        this.upvotes = upvotes;
        this.downvotes = downvotes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }

    public int getDownvotes() { return downvotes; }
    public void setDownvotes(int downvotes) { this.downvotes = downvotes; }

    public int getScore() { return upvotes - downvotes; }

    @Override
    public String toString() {
        return "Content{id=" + id + ", title='" + title + "', type='" + type + "'}";
    }
}
