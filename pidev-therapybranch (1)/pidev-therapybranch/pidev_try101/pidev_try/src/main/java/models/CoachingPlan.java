package models;

public class CoachingPlan {

    private int planId;
    private int userId;
    private String title;
    private String description;
    private String goals;
    private String imagePath;

    public CoachingPlan() {}

    public CoachingPlan(int userId, String title, String description, String goals, String imagePath) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.goals = goals;
        this.imagePath = imagePath;
    }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
