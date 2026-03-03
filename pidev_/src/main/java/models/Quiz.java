package models;

import java.util.ArrayList;
import java.util.List;

public class Quiz {
    private int id;
    private String title;
    private String description;
    private String category;
    private List<Question> questions = new ArrayList<>();
    private boolean pinned; // pinned field

    // Full constructor
    public Quiz(int id, String title, String description, String category, boolean pinned) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.pinned = pinned;
    }

    // Short constructor (optional)
    public Quiz(int id, String title) {
        this(id, title, "", "", false);
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
}