package utils;

import models.AppUser;

public class UserSession {
    private static UserSession instance;
    private AppUser user;

    private UserSession() {}

    public static UserSession get() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public void setUser(AppUser user) { this.user = user; }

    public AppUser getUser() { return user; }

    public int userId() { return (user == null) ? 0 : user.getId(); }   // ✅ add this

    public boolean isLoggedIn() { return user != null; }

    public void clear() { user = null; }
}
