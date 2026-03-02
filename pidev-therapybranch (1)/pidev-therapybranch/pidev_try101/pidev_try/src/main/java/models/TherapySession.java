package models;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class TherapySession {

    private int sessionId;
    private int psychologistId;
    private int userId;
    private LocalDateTime sessionDate;
    private int durationMinutes;
    private String sessionStatus;
    private String sessionNotes;
    private Timestamp createdAt;
    private String feedbackComment;

    // ✅ Empty constructor (required)
    public TherapySession() {
    }

    // ✅ Full constructor (used when reading from DB)
    public TherapySession(int sessionId, int psychologistId, int userId,
                          LocalDateTime sessionDate, int durationMinutes,
                          String sessionStatus, String sessionNotes,
                          Timestamp createdAt) {
        this.sessionId = sessionId;
        this.psychologistId = psychologistId;
        this.userId = userId;
        this.sessionDate = sessionDate;
        this.durationMinutes = durationMinutes;
        this.sessionStatus = sessionStatus;
        this.sessionNotes = sessionNotes;
        this.createdAt = createdAt;
    }

    // ✅ Constructor for INSERT (ID auto-generated)
    public TherapySession(int psychologistId, int userId,
                          LocalDateTime sessionDate, int durationMinutes,
                          String sessionStatus, String sessionNotes) {
        this.psychologistId = psychologistId;
        this.userId = userId;
        this.sessionDate = sessionDate;
        this.durationMinutes = durationMinutes;
        this.sessionStatus = sessionStatus;
        this.sessionNotes = sessionNotes;
        this.feedbackComment = "";
    }

    @Override
    public String toString() {
        return "TherapySession{" +
                "sessionId=" + sessionId +
                ", psychologistId=" + psychologistId +
                ", userId=" + userId +
                ", sessionDate=" + sessionDate +
                ", durationMinutes=" + durationMinutes +
                ", sessionStatus='" + sessionStatus + '\'' +
                ", sessionNotes='" + sessionNotes + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    // ✅ Getters & Setters

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getPsychologistId() {
        return psychologistId;
    }

    public void setPsychologistId(int psychologistId) {
        this.psychologistId = psychologistId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDateTime getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDateTime sessionDate) {
        this.sessionDate = sessionDate;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getSessionNotes() {
        return sessionNotes;
    }

    public void setSessionNotes(String sessionNotes) {
        this.sessionNotes = sessionNotes;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getFeedbackComment() { return feedbackComment; }

    public void setFeedbackComment(String feedbackComment) { this.feedbackComment = feedbackComment; }

}