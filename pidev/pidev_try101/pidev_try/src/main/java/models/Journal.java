package models;

import java.time.LocalDateTime;

public class Journal {
    private int id;
    private String title;
    private String content;
    private String mood;        // <-- make sure this exists
    private LocalDateTime date;

    // No-arg constructor
    public Journal() {}

    // Full constructor
    public Journal(int id, String title, String content, String mood, LocalDateTime date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.mood = mood;      // <-- assign properly
        this.date = date;
    }
    public Journal(int id, String title, String content) {
        this(id, title, content, "", LocalDateTime.now());
    }
    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMood() { return mood; }   // <-- must exist
    public void setMood(String mood) { this.mood = mood; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
}
