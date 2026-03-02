package models;

import java.sql.Timestamp;

public class Comment {
    private int id;
    private int contentId;
    private String text;
    private Timestamp createdAt;
    private int upvotes;
    private int downvotes;

    public Comment() {}

    public Comment(int contentId, String text) {
        this.contentId = contentId;
        this.text = text;
    }

    public Comment(int id, int contentId, String text, Timestamp createdAt, int upvotes, int downvotes) {
        this.id = id;
        this.contentId = contentId;
        this.text = text;
        this.createdAt = createdAt;
        this.upvotes = upvotes;
        this.downvotes = downvotes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getContentId() { return contentId; }
    public void setContentId(int contentId) { this.contentId = contentId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }

    public int getDownvotes() { return downvotes; }
    public void setDownvotes(int downvotes) { this.downvotes = downvotes; }

    public int getScore() { return upvotes - downvotes; }

    @Override
    public String toString() {
        return text;
    }
}
