package models;

public class AppUser {
    private final int id;
    private final String fullName;
    private final Role role;

    public AppUser(int id, String fullName, Role role) {
        this.id = id;
        this.fullName = fullName;
        this.role = role;
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
}
