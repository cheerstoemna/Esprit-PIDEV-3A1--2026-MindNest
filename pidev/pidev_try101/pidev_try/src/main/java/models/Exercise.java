package models;

public class Exercise {
    private int exerciseId;
    private int planId;
    private String title;
    private String description;
    private int duration; // minutes
    private String difficultyLevel; // Easy/Medium/Hard

    // NEW: resource image file name stored in DB (e.g., "breathing.png")
    private String image;

    public Exercise() {}

    public int getExerciseId() { return exerciseId; }
    public void setExerciseId(int exerciseId) { this.exerciseId = exerciseId; }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
