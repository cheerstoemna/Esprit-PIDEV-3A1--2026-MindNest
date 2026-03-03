package models;

import java.sql.Timestamp;

public class Comment {

    private int id;
    private int contentId;

    private int userId;
    private String userName;

    private String text;
    private Timestamp createdAt;

    private int upvotes;
    private int downvotes;

    // For insert
    public Comment(int contentId, int userId, String text) {
        this.contentId = contentId;
        this.userId = userId;
        this.text = text;
    }

    // Backward-compatible constructor (if older code used it)
    public Comment(int contentId, String text) {
        this.contentId = contentId;
        this.text = text;
    }

    // For read from DB (with username)
    public Comment(int id, int contentId, int userId, String userName, String text,
                   Timestamp createdAt, int upvotes, int downvotes) {
        this.id = id;
        this.contentId = contentId;
        this.userId = userId;
        this.userName = userName;
        this.text = text;
        this.createdAt = createdAt;
        this.upvotes = upvotes;
        this.downvotes = downvotes;
    }

    // Getters
    public int getId() { return id; }
    public int getContentId() { return contentId; }

    public int getUserId() { return userId; }
    public String getUserName() { return userName; }

    public String getText() { return text; }
    public Timestamp getCreatedAt() { return createdAt; }

    public int getUpvotes() { return upvotes; }
    public int getDownvotes() { return downvotes; }

    // Setters
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }
    public void setDownvotes(int downvotes) { this.downvotes = downvotes; }

    public int getScore() { return upvotes - downvotes; }
}