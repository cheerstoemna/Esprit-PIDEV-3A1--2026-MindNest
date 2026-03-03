package models;

public class Profile {
    private int    profileId;
    private int    userId;
    private String goals;
    private String interests;
    private String bio;

    public Profile() {}

    public Profile(int profileId, int userId, String goals,
                   String interests, String bio) {
        this.profileId = profileId;
        this.userId    = userId;
        this.goals     = goals;
        this.interests = interests;
        this.bio       = bio;
    }

    // Getters & setters
    public int    getProfileId()  { return profileId; }
    public void   setProfileId(int profileId) { this.profileId = profileId; }
    public int    getUserId()     { return userId; }
    public void   setUserId(int userId) { this.userId = userId; }
    public String getGoals()      { return goals; }
    public void   setGoals(String goals) { this.goals = goals; }
    public String getInterests()  { return interests; }
    public void   setInterests(String interests) { this.interests = interests; }
    public String getBio()        { return bio; }
    public void   setBio(String bio) { this.bio = bio; }
}