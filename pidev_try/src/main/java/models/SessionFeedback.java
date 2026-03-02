package models;

import java.sql.Timestamp;

public class SessionFeedback {

    private int feedbackId;
    private int sessionId;
    private int userId;
    private int rating;
    private String comment;
    private Timestamp createdAt;

    public SessionFeedback() {}

    /*public SessionFeedback(int sessionId, int userId, int rating, String comment) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
    }*/
    public SessionFeedback(int sessionId, int rating, String comment) {
        this.sessionId = sessionId;
        this.rating = rating;
        this.comment = comment;
    }


    // Getters & Setters

    public int getFeedbackId() { return feedbackId; }
    public void setFeedbackId(int feedbackId) { this.feedbackId = feedbackId; }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

   /* public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    */
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}