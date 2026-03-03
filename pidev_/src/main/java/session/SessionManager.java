package session;

public class SessionManager {

    private static SessionManager instance;

    private int    userId;
    private String name;
    private String email;
    private String role;
    private int    profileId;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(int userId, String name, String email, String role) {
        this.userId = userId;
        this.name   = name;
        this.email  = email;
        this.role   = role;
    }

    public void logout() {
        userId = 0; name = null; email = null; role = null; profileId = 0;
    }

    public boolean isLoggedIn()        { return userId != 0; }
    public int     getUserId()          { return userId; }
    public String  getName()            { return name; }
    public String  getEmail()           { return email; }
    public String  getRole()            { return role; }
    public int     getProfileId()       { return profileId; }
    public void    setProfileId(int id) { this.profileId = id; }
}