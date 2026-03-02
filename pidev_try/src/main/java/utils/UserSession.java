package utils;

import models.AppUser;
import models.Role;

public final class UserSession {
    private static UserSession instance;
    private AppUser user;

    private UserSession() {}

    public static UserSession get() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public void setUser(AppUser user) { this.user = user; }
    public AppUser user() { return user; }

    public int userId() { return user == null ? 0 : user.getId(); }
    public Role role() { return user == null ? Role.CLIENT : user.getRole(); }
}
