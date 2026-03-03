package models;

import java.time.LocalDateTime;

public class Journal {
    private int id;
    private String title;
    private String content;
    private String mood;
    private String aiAnalysis;
    private LocalDateTime date;

    // No-arg constructor
    public Journal() {}

    // Full constructor
    public Journal(int id, String title, String content, String mood, String aiAnalysis, LocalDateTime date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.mood = mood;
        this.aiAnalysis = aiAnalysis;
        this.date = date;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
}