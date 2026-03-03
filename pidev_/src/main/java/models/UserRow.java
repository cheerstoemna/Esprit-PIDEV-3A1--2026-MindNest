package models;

import javafx.beans.property.*;

/**
 * JavaFX-friendly model for the admin users table.
 * Each field is a Property so TableView can bind to it directly.
 */
public class UserRow {

    private final IntegerProperty    id;
    private final StringProperty     name;
    private final StringProperty     email;
    private final StringProperty     role;
    private final StringProperty     gender;
    private final IntegerProperty    age;
    private final StringProperty     status;

    public UserRow(int id, String name, String email,
                   String role, String gender, int age, int status) {
        this.id     = new SimpleIntegerProperty(id);
        this.name   = new SimpleStringProperty(name);
        this.email  = new SimpleStringProperty(email);
        this.role   = new SimpleStringProperty(role);
        this.gender = new SimpleStringProperty(gender);
        this.age    = new SimpleIntegerProperty(age);
        this.status = new SimpleStringProperty(status == 1 ? "Active" : "Inactive");
    }

    // ── Getters for CSV export ────────────────────────────────────────
    public int    getId()     { return id.get(); }
    public String getName()   { return name.get(); }
    public String getEmail()  { return email.get(); }
    public String getRole()   { return role.get(); }
    public String getGender() { return gender.get(); }
    public int    getAge()    { return age.get(); }
    public String getStatus() { return status.get(); }

    // ── Property getters for TableView binding ────────────────────────
    public IntegerProperty idProperty()     { return id; }
    public StringProperty  nameProperty()   { return name; }
    public StringProperty  emailProperty()  { return email; }
    public StringProperty  roleProperty()   { return role; }
    public StringProperty  genderProperty() { return gender; }
    public IntegerProperty ageProperty()    { return age; }
    public StringProperty  statusProperty() { return status; }
}
