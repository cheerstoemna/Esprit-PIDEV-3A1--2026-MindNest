package models;

public class AppUser {
    private int id;
    private String name;
    private Role role;

    public AppUser(int id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setRole(Role role) { this.role = role; }
}